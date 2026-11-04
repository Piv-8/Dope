package it.unipd.dei.webapp.dope.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Containerfile resource used in the web application.
 * <p>
 * This class contains metadata and configuration instructions for building container images.
 * This class provides support for JSON serialization and deserialization for use in a RESTful web services.
 * </p>
 *
 * <p>Fields include:</p>
 * <ul>
 *   <li>{@code name}: the name of the containerfile</li>
 *   <li>{@code from}: the base image for the container</li>
 *   <li>{@code cmd}: the default command to execute</li>
 *   <li>{@code json_obj}: a map of additional optional commands defined in a JSON configuration</li>
 *   <li>{@code image_built}: the ID of the associated built image</li>
 * </ul>
 */
public class Containerfile extends AbstractResource {

    /** The name of the containerfile. */
    @JsonProperty("name")
    private String name;

    /** The base image specified in the containerfile. */
    @JsonProperty("from")
    private String from;

    /** The CMD instruction to run in the container. */
    @JsonProperty("cmd")
    private String cmd;

    /** A map representing additional optional commands defined in a JSON configuration */
    @JsonProperty("json_obj")
    private Map<String, List<String>> json_Obj;

    /** The ID of the image built from this containerfile. */
    @JsonProperty("image_built")
    private int image_Built;

    /** Default constructor. */
    public Containerfile() {}

    /**
     * Constructs a new Containerfile with the given parameters.
     *
     * @param name        the name of the containerfile
     * @param from        the base image
     * @param cmd         the CMD instruction
     * @param json_ObjStr a JSON string representing additional optional commands
     * @param image_Built the ID of the built image
     */
    public Containerfile(String name, String from, String cmd, String json_ObjStr, int image_Built) {
        this.name = name;
        this.from = from;
        this.cmd = cmd;
        this.json_Obj = parseJsonObj(json_ObjStr);
        this.image_Built = image_Built;
    }

    /**
     * Parses a JSON string into a {@code Map<String, List<String>>}.
     *
     * @param json_ObjStr the JSON string
     * @return the parsed map
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private Map<String, List<String>> parseJsonObj(String json_ObjStr) {
        try {
            if (json_ObjStr == null || json_ObjStr.isBlank()) {
                return new HashMap<>();
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json_ObjStr, new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<String>>>() {});
        } catch (IOException e) {
            LOGGER.error("Error parsing json_obj", e);
            throw new IllegalArgumentException("Invalid JSON format for json_obj", e);
        }
    }

    /**
     * Returns the name of the containerfile.
     *
     * @return the containerfile name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the containerfile.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the base image specified in the containerfile.
     *
     * @return the base image
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the base image for the containerfile.
     *
     * @param from the base image to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the CMD instruction to run in the container.
     *
     * @return the CMD instruction
     */
    public String getCmd() {
        return cmd;
    }

    /**
     * Sets the CMD instruction to run in the container.
     *
     * @param cmd the CMD instruction to set
     */
    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    /**
     * Returns the optional commands specified in the JSON configuration.
     * This map represents additional instructions such as RUN, ENV, EXPOSE, etc.,
     * that are optionally included in the containerfile.
     *
     * @return a map containing optional commands
     */
    public Map<String, List<String>> getJson_Obj() {
        return json_Obj;
    }

    /**
     * Sets the optional commands in the JSON configuration.
     * These are additional Dockerfile-like instructions (e.g., RUN, ENV, etc.)
     * represented as a map, where each key is a command and the value is a list of arguments.
     *
     * @param json_Obj the map of optional commands to set
     */
    public void setJson_Obj(Map<String, List<String>> json_Obj) {
        this.json_Obj = json_Obj;
    }


    /**
     * Returns the ID of the image built from this containerfile.
     *
     * @return the built image ID
     */
    public int getImage_Built() {
        return image_Built;
    }

    /**
     * Sets the ID of the image built from this containerfile.
     *
     * @param image_Built the image ID to set
     */
    public void setImage_Built(int image_Built) {
        this.image_Built = image_Built;
    }


    /**
     * Converts the {@code json_Obj} map into a JSON string.
     *
     * @return a JSON string or an error message if conversion fails
     */
    public String jsonObjToString() {
        if (json_Obj == null || json_Obj.isEmpty()) {
            return "{}";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(json_Obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return "{\"error\": \"Invalid JSON\"}";
        }
    }

    /**
     * Deserializes a {@code Containerfile} object from a JSON input stream.
     *
     * @param json the servlet input stream
     * @return the {@code Containerfile} object
     * @throws IllegalArgumentException if required fields are missing or the format is invalid
     */
    public static Containerfile fromJSON(ServletInputStream json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode containerNode = root.get("containerfile");

            if (containerNode == null || containerNode.isNull()) {
                throw new IllegalArgumentException("No Containerfile object found");
            }

            Containerfile cf = mapper.treeToValue(containerNode, Containerfile.class);
            if (cf.getName() == null || cf.getCmd() == null || cf.getFrom() == null || cf.getImage_Built() == -1)
                throw new IllegalArgumentException("Invalid JSON format");
            else {
                // Convert the "from" field to lowercase
                if (cf.getFrom() != null) {
                    cf.setFrom(cf.getFrom().toLowerCase());
                }
                return cf;
            }
        } catch (IOException e) {
            LOGGER.error("Unable to parse ContainerFile from JSON", e);
            return null;
        }
    }

    /**
     * Serializes this {@code Containerfile} instance to JSON and writes it to the output stream.
     *
     * @param out the output stream to write to
     * @throws Exception if an error occurs during serialization
     */
    @Override
    protected final void writeJSON(final OutputStream out) throws Exception {
        final JsonGenerator jg = JSON_FACTORY.createGenerator(out);
        jg.writeStartObject();
        jg.writeFieldName("containerfile");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(jg, this);
        jg.writeEndObject();
        jg.flush();
    }
}
