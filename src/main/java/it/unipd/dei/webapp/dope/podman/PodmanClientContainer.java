package it.unipd.dei.webapp.dope.podman;

import it.unipd.dei.webapp.dope.resource.Container;
import jakarta.json.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * PodmanClientContainer provides methods to manage Podman containers via the Podman REST API.
 * <p>
 * This class allows for creating, starting, stopping, executing commands, inspecting, and retrieving logs from containers.
 * </p>
 */
public class PodmanClientContainer{

    /** Logger instance for this client. */
    protected static final Logger LOGGER = LogManager.getLogger(PodmanClientContainer.class,
            StringFormatterMessageFactory.INSTANCE);

    /** The container resource associated with this Podman client instance. */
    private final Container container;

    /**
     * Constructs a new PodmanClientContainer for the specified container.
     *
     * @param container the {@link Container} resource to manage
     * @throws Exception if an error occurs during initialization
     */
    public PodmanClientContainer(Container container) throws Exception {
        super();
        this.container = container;
    }

    /**
     * Creates a container using the Podman API.
     * <p>
     * If a command is specified, the container will be created with the provided command;
     * otherwise, a standard container is created using the image associated with the Container resource.
     * </p>
     *
     * @param command a list of command-line arguments to run in the container, or null/empty for default
     * @return the container ID if creation is successful, or null otherwise
     */
    public String createContainer(List<String> command) {
        String url = "http://podman:1234/v4.0.0/libpod/containers/create";
        String jsonPayload = null;

        // Build JSON payload according to whether a command was specified.
        if (command == null || command.isEmpty()) {
            jsonPayload = Json.createObjectBuilder()
                    .add("name", String.valueOf(container.getInstanceId()))
                    .add("image", "localhost/" + container.getImageRun())
                    .build()
                    .toString();
        } else {
            JsonArrayBuilder commandArrayBuilder = Json.createArrayBuilder();
            for (String cmdPart : command) {
                commandArrayBuilder.add(cmdPart);
            }
            // Including a command in the JSON payload for the container creation.
            jsonPayload = Json.createObjectBuilder()
                    .add("name", String.valueOf(container.getInstanceId()))
                    .add("image", "localhost/" + container.getImageRun())
                    .add("command", commandArrayBuilder)
                    .build()
                    .toString();
        }

        // Build the HTTP client with a connection timeout.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Build the HTTP POST request with the JSON payload.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            // Send the request and receive the response synchronously.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String containerId = null;
            // If the container was created successfully, the API returns HTTP 201 Created.
            if (response.statusCode() == 201) {
                containerId = response.body();
                LOGGER.info("Container %s created", container.getInstanceId());
            } else {
                LOGGER.error("Couldn't create container: %s\n Error code: %s, Error message: %s", container.getInstanceId(), response.statusCode(), response.body());
            }
            return containerId;
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error during container creation POST: %s", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Commits the current container to a new image using the Podman API.
     * <p>
     * The image is created under the provided image ID.
     * </p>
     *
     * @param image_id The ID to assign to the newly created image.
     * @return The image ID if the commit was successful, null otherwise.
     */
    public String commitImage(int image_id) {
        // Build the base URL for commit.
        String baseUrl = "http://podman:1234/v4.0.0/libpod/commit";

        // Append query parameters properly encoded.
        String repoParam = "repo=" + image_id;
        String containerParam = "container=" + container.getInstanceId();
        String url = String.format("%s?%s&&%s", baseUrl, repoParam, containerParam);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Since no body is required, use noBody().
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LOGGER.info("Container %s committed into image %s", container.getInstanceId(), image_id);
            } else {
                LOGGER.error("Couldn't commit container: %s\n Status: %s", container.getInstanceId(), response.statusCode());
                return null;
            }
            return String.valueOf(image_id);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error during commit image POST: %s", e.getMessage());
            return null;
        }  catch (Exception e) {
            LOGGER.error("Error while interacting with podman client: %s", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts the container using the Podman API.
     *
     * @return HTTP status code of the start request (204 if started, 304 if already started, error otherwise)
     */
    public int startContainer() {
        // Build the URI for starting the container.
        String url = "http://podman:1234/v4.0.0/libpod/containers/" + container.getInstanceId() + "/start";

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("Start container request sent for container %d", container.getInstanceId());
            int respStatus = response.statusCode();
            if (respStatus == 204) {
                LOGGER.info("Container %s started", container.getInstanceId());
            } else if (respStatus == 304) {
                LOGGER.error("Couldn't start container %d, container already started", container.getInstanceId());
            } else {
                LOGGER.error("Couldn't start container %d, error: %d", container.getInstanceId(), respStatus);
            }
            return respStatus;
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error during start container POST: %s", e.getMessage());
            Thread.currentThread().interrupt();
            return 500;
        }
    }

    /**
     * Stops (kills) the container using the Podman API.
     *
     * @return HTTP status code of the kill request (204 if killed, 409 if already stopped, error otherwise)
     */
    public int killContainer() {
        if(container == null) return 500;
        // Build the URL for killing the container.
        String url = "http://podman:1234/v4.0.0/libpod/containers/" + container.getInstanceId() + "/kill";

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int respStatus = response.statusCode();

            if (respStatus == 204) {
                LOGGER.info("Container %s killed", container.getInstanceId());
            } else if (respStatus == 409) {
                LOGGER.error("Couldn't kill container %s: ", response.body());
            } else {
                LOGGER.error("Couldn't kill container %d, error: %d", container.getInstanceId(), respStatus);
            }
            return respStatus;
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error killing container %s: %s", container.getInstanceId(), e.getMessage());
            return -1;
        }
    }

    /**
     * Executes a command inside the container using the Podman API.
     *
     * @param command The command to execute
     * @return The standard output produced by the command.
     */
    public String executeCommand(String command) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String createUrl = "http://podman:1234/v4.0.0/libpod/containers/" + container.getInstanceId() + "/exec";
        LOGGER.info("Executing command: sh -c %s", command.toString());
        List<String> cmd = List.of("sh", "-c", command);
        // Build the JSON payload for creating the exec instance.
        JsonArrayBuilder commandArrayBuilder = Json.createArrayBuilder();
        for (String cmdPart : cmd) {
            commandArrayBuilder.add(cmdPart);
        }
        JsonObject createPayload = Json.createObjectBuilder()
                .add("Cmd", commandArrayBuilder)
                .add("AttachStdout", true)
                .add("AttachStderr", true)
                .build();
        String createPayloadStr = createPayload.toString();

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(createUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createPayloadStr))
                .build();


        try {
            HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
            if (createResponse.statusCode() != 200 && createResponse.statusCode() != 201) {
                throw new RuntimeException("Failed to create exec instance, HTTP error code: " + createResponse.statusCode() + ", HTTP error response: %s" + createResponse.body());
            }

            // Parse the response JSON to extract the exec ID.
            String createResponseStr = createResponse.body();
            JsonReader jsonReader = Json.createReader(new StringReader(createResponseStr));
            JsonObject createResponseJson = jsonReader.readObject();
            jsonReader.close();
            String execId = createResponseJson.getString("Id");

            String startExecUrl = "http://podman:1234/v4.0.0/libpod/exec/" + execId + "/start";
            // Build the JSON payload for starting the exec instance.
            JsonObject startPayload = Json.createObjectBuilder()
                    .add("Detach", false)
                    .add("Tty", false)
                    .build();
            String startPayloadStr = startPayload.toString();

            HttpRequest startRequest = HttpRequest.newBuilder()
                .uri(URI.create(startExecUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(startPayloadStr))
                .build();

            HttpResponse<String> startResponse = httpClient.send(startRequest, HttpResponse.BodyHandlers.ofString());

            if (startResponse.statusCode() != 200) {
                throw new RuntimeException("Failed to start exec instance, HTTP error code: " + startResponse.statusCode());
            }

            // Return the standard output produced by the command.
            return startResponse.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to start exec instance: " + e.getMessage());
        }
    }


    /**
     * Retrieves the current status of the container using the Podman API.
     *
     * @return The status string of the container (e.g., "running", "exited").
     */
    public String getStatus() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String createUrl = "http://podman:1234/v4.0.0/libpod/containers/" + container.getInstanceId() + "/json";
        // Build the JSON payload for creating the exec instance.

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(createUrl))
                .header("Content-Type", "application/json")
                .GET()
                .build();


        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to inspect container: HTTP error code: " + response.statusCode() + ", HTTP error response: %s" + response.body());
            }

            // Parse the response JSON to extract the status
            String inspectString = response.body();
            JsonReader jsonReader = Json.createReader(new StringReader(inspectString));
            JsonObject responseJson = jsonReader.readObject();
            jsonReader.close();
            JsonObject jsonState = responseJson.getJsonObject("State");
            String status = jsonState.getString("Status");
            LOGGER.info("Inspecting through podman container: %s, has status: %s", container.getInstanceId(), status);
            container.setStatus(status);
            return status;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to inspect container: " + e.getMessage());
        }
    }



    /**
     * Retrieves the logs of the container using the Podman API.
     *
     * @return The logs from both stdout and stderr of the container.
     */
    public String getLogs() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Write the Query paramaters to get logs from both stderr and stdout.
        String url = "http://podman:1234/v4.0.0/libpod/containers/" + container.getInstanceId() + "/logs?stderr=true&stdout=true";
        JsonArrayBuilder commandArrayBuilder = Json.createArrayBuilder();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();


        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to get logs, HTTP error code: " + response.statusCode() + ", HTTP error response: %s" + response.body());
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to start exec instance: " + e.getMessage());
        }
    }
}