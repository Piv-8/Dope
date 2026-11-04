package it.unipd.dei.webapp.dope.podman;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import it.unipd.dei.webapp.dope.resource.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PodmanClientImage provides methods to interact with Podman images via the Podman REST API.
 * <p>
 * This class allows for retrieving image layers, converting size strings to bytes, and building images from Containerfile objects.
 * </p>
 */
public class PodmanClientImage{

    /** Logger instance for this client. */
    protected static final Logger LOGGER = LogManager.getLogger(PodmanClientImage.class,
            StringFormatterMessageFactory.INSTANCE);

    /**
     * The JSON factory to be used for creating JSON parsers and generators.
     */
    protected static final JsonFactory JSON_FACTORY;

    static {
        // Set up the JSON factory.
        JSON_FACTORY = new JsonFactory();
        JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        JSON_FACTORY.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        LOGGER.debug("JSON factory successfully setup.");
    }

    /** The image resource associated with this client. */
    private Image image;

    /**
     * Constructs a new PodmanClientImage for the specified image.
     *
     * @param image the {@link Image} resource to manage
     * @throws Exception if an error occurs during initialization
     */
    public PodmanClientImage(Image image) throws Exception {
        super();
        this.image = image;
    }

    /**
     * Converts a size string with units (e.g., "122.9MB", "1.024kB", "512B") to an integer value in bytes.
     *
     * @param sizeStr The size string to convert.
     * @return The size in bytes.
     */
    private static int convertSizeToBytes(String sizeStr) {
        int bytes = 0;
        try {
            if (sizeStr.endsWith("MB")) {
                double value = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 2));
                bytes = (int) Math.round(value * 1024 * 1024);
            } else if (sizeStr.endsWith("kB")) {
                double value = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 2));
                bytes = (int) Math.round(value * 1024);
            } else if (sizeStr.endsWith("B")) {
                double value = Double.parseDouble(sizeStr.substring(0, sizeStr.length() - 1));
                bytes = (int) Math.round(value);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    /**
     * Retrieves the layers of an image from the Podman API.
     * <p>
     * This method sends an HTTP GET request to the appropriate endpoint, parses the JSON response to extract the "Tree" field, and then processes that text to construct a list of layers.
     * </p>
     *
     * @return A list of Layer objects derived from the image tree.
     * @throws RuntimeException if the HTTP response status is not as expected or if parsing fails.
     */
    public List<Layer> getLayers() {
        String url = "http://podman:1234/v4.0.0/libpod/images/" + image.getID_image() + "/tree";

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(300))
                .build();

        // Build the HTTP GET request.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            // Send the request and get the response as a string.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (response.statusCode() == 404) {
                    throw new RuntimeException("Can't get layers from image " + image.getID_image() + " image not found");
                } else {
                    throw new RuntimeException("Can't get layers from image " + image.getID_image());
                }
            }
            LOGGER.info("Image %s, layers retrieved, response: %s", image.getID_image(), response.body());

            String jsonResponse = response.body();
            String tree = null;

            // Parse the JSON response to extract the "Tree" field.
            try (JsonParser parser = JSON_FACTORY.createParser(jsonResponse)) {
                while (parser.nextToken() != null) {
                    String fieldName = parser.getCurrentName();
                    if ("Tree".equals(fieldName)) {
                        parser.nextToken(); // Move to the value token.
                        tree = parser.getText();
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error parsing JSON response: " + e.getMessage(), e);
            }

            if (tree == null) {
                throw new NullPointerException("No tree found in JSON response.");
            }

            // Process the tree string to extract layer details.
            String[] lines = tree.split("\n");
            int layersIndex = -1;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().equals("Image Layers")) {
                    layersIndex = i;
                    break;
                }
            }
            if (layersIndex == -1) {
                return new ArrayList<>();
            }

            List<Layer> layers = new ArrayList<>();


            //First find the primary tag
            String primaryTag = "";
            for (String rawLine : lines) {
                String trimmed = rawLine.trim();
                if (trimmed.startsWith("Tags:")) {
                    int openBracket = trimmed.indexOf('[');
                    int closeBracket = trimmed.lastIndexOf(']');
                    if (openBracket >= 0 && closeBracket > openBracket) {
                        String insideBrackets = trimmed.substring(openBracket + 1, closeBracket).trim();
                        // Split on whitespace, just like Docker does when listing multiple tags
                        String[] allTags = insideBrackets.split("\\s+");
                        if (allTags.length > 0) {
                            primaryTag = allTags[0];
                        }
                    }
                    break;
                }
            }


            //Then like before parse every top layer buf if there are multiple top layers for the same
            //tag, only use the primary tag from this image
            for (int i = layersIndex + 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.startsWith("├──") || line.startsWith("└──")) {
                    String cleanLine = line.substring(3).trim();

                    // Same regex to grab ID, size, and everything inside the brackets:
                    Pattern pattern = Pattern.compile(
                            "ID:\\s*(\\S+)\\s+Size:\\s*(\\S+)\\s+Top Layer of:\\s*\\[(.+)\\]"
                    );
                    Matcher matcher = pattern.matcher(cleanLine);
                    if (! matcher.find()) {
                        continue;
                    }

                    //String idLayer   = matcher.group(1); // We don't need this
                    String sizeStr   = matcher.group(2);
                    String rawTags   = matcher.group(3);

                    // Filter multiple candidates down to a single chosenTag
                    String chosenTag = rawTags;
                    String[] candidates = rawTags.split("\\s+");
                    if (candidates.length > 1) {
                        boolean foundPrimary = false;
                        for (String cand : candidates) {
                            if (cand.equals(primaryTag)) {
                                chosenTag = cand;
                                foundPrimary = true;
                                break;
                            }
                        }
                        if (! foundPrimary) {
                            // Fallback: just take the first candidate
                            chosenTag = candidates[0];
                        }
                    }

                    Layer layer = new Layer(
                            chosenTag,
                            convertSizeToBytes(sizeStr),
                            image.getID_image()
                    );
                    layers.add(layer);
                }
            }
            return layers;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during HTTP GET for layers: " + e.getMessage(), e);
        }
    }

    /**
     * Builds an image from a given Containerfile object using the Podman API.
     * <p>
     * First, the method generates a Dockerfile from the provided Containerfile and writes it to a temporary folder.
     * Then, it sends an HTTP POST request to trigger the build, passing the necessary query parameters.
     * </p>
     *
     * @param cf The Containerfile object containing the build instructions.
     * @return The image tag if the build was successful, null otherwise.
     */
    public String buildImage(Containerfile cf) {
        // Generate the Dockerfile at the designated path.
        generateDockerfile(cf);

        // Construct the URL with the appropriate query parameters.
        String baseUrl = "http://podman:1234/v4.0.0/libpod/build";
        String tParam = "t=" + image.getID_image();
        String dockerfileParam = "dockerfile=" + URLEncoder.encode("../../../../tmp/containerfiles/" + cf.getName(), StandardCharsets.UTF_8);
        String url = String.format("%s?%s&%s", baseUrl, tParam, dockerfileParam);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(300))
                .build();

        // Send a POST request with an empty body.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String imageTag = null;
            if (response.statusCode() == 200 && !response.body().contains("errorDetail")) {
                imageTag = image.getTag();
                LOGGER.info("Image built: %s", response.body());
                LOGGER.info("Image successfully built with TAG: %s", imageTag);
            } else {
                LOGGER.error("Couldn't create image: %s\n Status: %s\n ResponseBody: %s", image.getTag(), response.statusCode(), response.body());
            }
            return imageTag;
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error during build image POST: %s", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("Error while building the image through podman client: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
    * Exports the image associated with this PodmanClientImage instance.
    * <p>
    * This method sends an HTTP GET request to the Podman API to retrieve the image data.
    * If the image is successfully retrieved, the method returns the image data as a string.
    * If the image is not found or an error occurs during the request, it logs the error and returns null.
    * </p>
    *
    * @return The image data as a string if the export is successful; null if the image is not found or an error occurs.
    */
    public byte[] exportImage() {
        if (image == null) return null;
        // Build the URL for killing the container.
        String url = "http://podman:1234/v4.0.0/libpod/images/" + image.getID_image() + "/get";

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(100))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int respStatus = response.statusCode();

            if (respStatus == 200) {
                LOGGER.info("Image %s, retrieved for export", image.getID_image());
            } else if (respStatus == 404) {
                LOGGER.error("Image %s not found", image.getID_image());
            } else {
                LOGGER.error("Couldn't export image %s, internal server error podman", image.getID_image());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error retrieving image %s: %s", image.getID_image(), e.getMessage());
            return null;
        }
    }


        /**
         * Generates a Dockerfile based on the properties of the given Containerfile object.
         * <p>
         * The generated file includes the FROM directive, any additional instructions provided in the JSON object,
         * an optional CMD directive, and is stored at {/tmp/containerfiles/}
         * </p>
         *
         * @param containerfile The Containerfile object containing build instructions.
         * @throws IllegalArgumentException if the containerfile or its critical properties are null.
         */
    public static void generateDockerfile(Containerfile containerfile) {
        if (containerfile == null) {
            throw new IllegalArgumentException("The Containerfile object and its required properties cannot be null.");
        }

        StringBuilder dockerfileContent = new StringBuilder();
        dockerfileContent.append("FROM ").append(containerfile.getFrom()).append("\n\n");

        // Process any additional instructions from the JSON object.
        if (containerfile.getJson_Obj() != null && !containerfile.getJson_Obj().isEmpty()) {
            for (Map.Entry<String, List<String>> entry : containerfile.getJson_Obj().entrySet()) {
                String instruction = entry.getKey().toUpperCase();
                List<String> commands = entry.getValue();
                String joinedCommands = String.join(" && ", commands);
                dockerfileContent.append(instruction)
                        .append(" ")
                        .append(joinedCommands)
                        .append("\n");
            }
            dockerfileContent.append("\n");
        }

        // Append the CMD directive if specified.
        if (containerfile.getCmd() != null && !containerfile.getCmd().isEmpty()) {
            dockerfileContent.append("CMD ").append(containerfile.getCmd()).append("\n\n");
        }

        // Define the output path.
        Path outputPath = Paths.get("/tmp/containerfiles", containerfile.getName());

        try {
            // Ensure that the directory exists.
            Files.createDirectories(outputPath.getParent());
            // Write the Dockerfile using UTF-8 encoding.
            Files.write(outputPath, dockerfileContent.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Containerfile successfully created at: " + outputPath.toString());
        } catch (IOException e) {
            System.err.println("Error writing the Containerfile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}