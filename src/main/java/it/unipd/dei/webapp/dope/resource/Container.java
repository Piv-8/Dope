package it.unipd.dei.webapp.dope.resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.servlet.ServletInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents a container resource used in the web application.
 * <p>
 * A container is characterized by a unique ID, a status string, an associated image to run,
 * and a user instance that owns or initiated the container.
 * <p>
 * This class provides support for JSON serialization and deserialization for use in a RESTful web services.
 */
public class Container extends AbstractResource {

    /** The unique identifier for the instance. */
    private int instanceId;

    /** The current status of the instance (created, stopped, running, paused, restarting, exited, dead) */
    private String status;

    /** The ID of the image that is being run by the instance. */
    private int imageRun;

    /** The user ID associated with the instance. */
    private int userInstance;


    /**
     * Creates a new {@code Container} object with the specified parameters.
     *
     * @param instanceId   the unique ID of the container instance.
     * @param status       the current status of the container.
     * @param imageRun     the ID of the image being run in the container.
     * @param userInstance the ID of the user instance associated with the container.
     */
    public Container(int instanceId, String status, int imageRun, int userInstance) {
        this.instanceId = instanceId;
        this.status = status;
        this.imageRun = imageRun;
        this.userInstance = userInstance;
    }

    /**
     * Returns the container instance ID.
     *
     * @return the instance ID.
     */
    public int getInstanceId() {
        return this.instanceId;
    }

    /**
     * Sets the container instance ID.
     *
     * @param newInstanceId the new instance ID.
     */
    public void setInstanceId(int newInstanceId) {
        this.instanceId = newInstanceId;
    }

    /**
     * Returns the status of the container.
     *
     * @return the status.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Sets the status of the container.
     *
     * @param newStatus the new status.
     */
    public void setStatus(String newStatus) {
        this.status = newStatus;
    }

    /**
     * Returns the ID of the image being run in the container.
     *
     * @return the image run ID.
     */
    public int getImageRun() {
        return imageRun;
    }

    /**
     * Sets the ID of the image to be run in the container.
     *
     * @param imageRun the image run ID.
     */
    public void setImageRun(int imageRun) {
        this.imageRun = imageRun;
    }

    /**
     * Returns the ID of the user instance associated with this container.
     *
     * @return the user instance ID.
     */
    public int getUserInstance() {
        return userInstance;
    }

    /**
     * Sets the ID of the user instance associated with this container.
     *
     * @param userInstance the new user instance ID.
     */
    public void setUserInstance(int userInstance) {
        this.userInstance = userInstance;
    }

    /**
     * Serializes this {@code Container} object to JSON format and writes it to the specified output stream.
     *
     * @param out the {@code OutputStream} to write the JSON data to.
     * @throws Exception if an error occurs during JSON serialization.
     */
    @Override
    protected final void writeJSON(final OutputStream out) throws Exception {
        final JsonGenerator jg = JSON_FACTORY.createGenerator(out);

        jg.writeStartObject();
        jg.writeFieldName("container");
        jg.writeStartObject();

        jg.writeNumberField("instance_id", instanceId);
        jg.writeStringField("status", status);
        jg.writeNumberField("image_run", imageRun);
        jg.writeNumberField("user_instance", userInstance);

        jg.writeEndObject();
        jg.writeEndObject();

        jg.flush();
    }

    /**
     * Deserializes a {@code Container} object from a JSON input stream.
     *
     * @param inputStream the {@code ServletInputStream} containing the JSON data.
     * @return the {@code Container} object parsed from JSON, or {@code null} if parsing fails or input is invalid.
     * @throws IOException if an I/O error occurs during parsing.
     */
    public static Container fromJSON(ServletInputStream inputStream) throws IOException {
        int jinstanceId = -1;
        String jstatus = null;
        int jimageRun = -1;
        int juserInstance = -1;

        Container c = null;

        try {
            final JsonParser jp = JSON_FACTORY.createParser(inputStream);
            // Move to the "container" field
            while (jp.getCurrentToken() != JsonToken.FIELD_NAME || !"container".equals(jp.getCurrentName())) {
                if (jp.nextToken() == null) {
                    LOGGER.error("No Container object found");
                    throw new EOFException("Unable to parse JSON: no Container object found");
                }
            }

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                    switch (jp.getCurrentName()) {
                        case "instance_id":
                            jp.nextToken();
                            jinstanceId = jp.getIntValue();
                            break;
                        case "status":
                            jp.nextToken();
                            jstatus = jp.getText();
                            break;
                        case "image_run":
                            jp.nextToken();
                            jimageRun = jp.getIntValue();
                            break;
                        case "user_instance":
                            jp.nextToken();
                            juserInstance = jp.getIntValue();
                            break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to parse Container from JSON", e);
            throw e;
        }

        if (jinstanceId > 0 && jstatus != null && jimageRun > 0 && juserInstance > 0) {
            c = new Container(jinstanceId, jstatus, jimageRun, juserInstance);
        }

        return c;
    }
}

