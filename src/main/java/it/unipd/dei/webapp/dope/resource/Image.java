package it.unipd.dei.webapp.dope.resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.servlet.ServletInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents a container image resource used within the web application.
 * This class encapsulates information such as the image ID, tag,
 * associated repository, description, and status.
 *
 * It supports JSON serialization and deserialization for use in
 * RESTful web services.
 */
public class Image extends AbstractResource {

    /** The unique identifier for the image. */
    private int ID_image;

    /**
     *  The tag associated with the image.
     * Tags are used to differentiate different versions or variants of the same image.
     */
    private String tag;

    /** The repository ID where the image is stored. */
    private int repo;

    /** A brief description of the image. */
    private String description;

    /** The current status of the image(building, ready). */
    private String status;


    /** Default constructor. */
    public Image() {
    }

    /**
     * Creates a new Image with the specified parameters.
     *
     * @param ID_image    the unique identifier of the image
     * @param tag         the tag associated with the image
     * @param repo        the identifier of the repository the image belongs to
     * @param description a human-readable description of the image
     * @param status      the current status of the image (building, ready)
     */
    public Image(int ID_image, String tag, int repo, String description, String status) {
        this.ID_image = ID_image;
        this.tag = tag;
        this.repo = repo;
        this.description = description;
        this.status = status;
    }


    /**
     * Returns the ID of the image.
     *
     * @return the image ID
     */
    public int getID_image() {
        return ID_image;
    }

    /**
     * Sets the ID of the image.
     *
     * @param ID_image the image ID to set
     */
    public void setID_image(int ID_image) {
        this.ID_image = ID_image;
    }

    /**
     * Returns the tag of the image.
     *
     * @return the image tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the tag of the image.
     *
     * @param tag the image tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Returns the repository ID the image is associated with.
     *
     * @return the repository ID
     */
    public int getRepo() {
        return repo;
    }

    /**
     * Sets the repository ID the image is associated with.
     *
     * @param repo the repository ID to set
     */
    public void setRepo(Integer repo) {
        this.repo = repo;
    }

    /**
     * Returns the description of the image.
     *
     * @return the image description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the image.
     *
     * @param description the image description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the status of the image.
     *
     * @return the image status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of the image.
     *
     * @param status the image status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }


    /**
     * Serializes the image object to JSON and writes it to the provided output stream.
     *
     * @param out the output stream to write the JSON to
     * @throws Exception if an error occurs during JSON generation
     */
    @Override
    protected final void writeJSON(final OutputStream out) throws Exception {
        final JsonGenerator jg = JSON_FACTORY.createGenerator(out);

        jg.writeStartObject();
        jg.writeFieldName("image");
        jg.writeStartObject();

        jg.writeNumberField("id_image", ID_image);
        jg.writeStringField("tag", tag);
        jg.writeNumberField("repo", repo);
        jg.writeStringField("description", description);
        jg.writeStringField("status", status);

        jg.writeEndObject();
        jg.writeEndObject();

        jg.flush();
    }

    /**
     * Deserializes a {@code Image} object from a JSON input stream.
     *
     * @param inputStream the input stream containing the JSON
     * @return the parsed Image object, or null if the JSON is invalid
     * @throws IOException if an error occurs during parsing
     */
    public static Image fromJSON(ServletInputStream inputStream) throws IOException {
        int jID_image = -1;
        String jtag = null;
        int jrepo = -1;
        String jdescription = null;
        String jstatus = null;

        Image i = null;

        try {
            final JsonParser jp = JSON_FACTORY.createParser(inputStream);

            // Search for the start of the "image" object
            while (jp.getCurrentToken() != JsonToken.FIELD_NAME || !"image".equals(jp.getCurrentName())) {
                if (jp.nextToken() == null) {
                    LOGGER.error("No Image object found");
                    throw new EOFException("Unable to parse JSON: no Image object found");
                }
            }

            // Read and extract each field
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                    switch (jp.getCurrentName()) {
                        case "id_image" -> {
                            jp.nextToken();
                            jID_image = jp.getIntValue();
                        }
                        case "tag" -> {
                            jp.nextToken();
                            jtag = jp.getText();
                        }
                        case "repo" -> {
                            jp.nextToken();
                            jrepo = jp.getIntValue();
                        }
                        case "description" -> {
                            jp.nextToken();
                            jdescription = jp.getText();
                        }
                        case "status" -> {
                            jp.nextToken();
                            jstatus = jp.getText();
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to parse Image from JSON", e);
            throw e;
        }

        if (jID_image > 0 && jtag != null && jrepo > 0 && jdescription != null && jstatus != null) {
            i = new Image(jID_image, jtag, jrepo, jdescription, jstatus);
        }

        return i;
    }
}
