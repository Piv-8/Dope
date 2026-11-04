package it.unipd.dei.webapp.dope.resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.servlet.ServletInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents a container layer resource used within the web application.
 * This class encapsulates information such as the layer ID, size,
 * and image owner.
 *
 * It supports JSON serialization and deserialization for use in
 * RESTful web services.
 */
public class Layer extends AbstractResource {


    /** The unique identifier for the layer. */
    private String ID_Layer;

    /** The size of the layer in bytes. */
    private int size;

    /** The identifier of the image owner. */
    private int image_owner;


    /** Default constructor. */
    public Layer() {}

    /**
     * Creates a new Layer with the specified parameters.
     *
     * @param ID_Layer    the unique identifier of the layer
     * @param size        the size of the layer
     * @param image_owner the identifier of the image owner
     */
    public Layer(String ID_Layer, int size, int image_owner) {
        this.ID_Layer = ID_Layer;
        this.size = size;
        this.image_owner = image_owner;
    }


    /**
     * Returns the ID of the layer.
     *
     * @return the layer ID
     */
    public String getID_Layer() {
        return ID_Layer;
    }

    /**
     * Sets the ID of the layer.
     *
     * @param ID_Layer the layer ID to set
     */
    public void setID_Layer(String ID_Layer) {
        this.ID_Layer = ID_Layer;
    }

    /**
     * Returns the size of the layer.
     *
     * @return the layer size
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size of the layer.
     *
     * @param size the layer size to set
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Returns the image owner associated with the layer.
     *
     * @return the image owner ID
     */
    public int getImage_owner() {
        return image_owner;
    }

    /**
     * Sets the image owner associated with the layer.
     *
     * @param image_owner the image owner ID to set
     */
    public void setImage_owner(int image_owner) {
        this.image_owner = image_owner;
    }


    /**
     * Serializes the layer object to JSON and writes it to the provided output stream.
     *
     * @param out the output stream to write the JSON to
     * @throws Exception if an error occurs during JSON generation
     */
    @Override
    protected final void writeJSON(final OutputStream out) throws Exception {
        final JsonGenerator jg = JSON_FACTORY.createGenerator(out);

        jg.writeStartObject();
        jg.writeFieldName("layer");
        jg.writeStartObject();

        jg.writeStringField("id_layer", ID_Layer);
        jg.writeNumberField("size", size);
        jg.writeNumberField("image_owner", image_owner);

        jg.writeEndObject();
        jg.writeEndObject();

        jg.flush();
    }

    /**
     * Deserializes a {@code Layer} object from a JSON input stream.
     *
     * @param inputStream the input stream containing the JSON
     * @return the parsed Layer object, or null if the JSON is invalid
     * @throws IOException if an error occurs during parsing
     */
    public static Layer fromJSON(ServletInputStream inputStream) throws IOException {
        String jID_Layer = null;
        int jsize = -1;
        int jimage_owner = -1;

        Layer l = null;

        try {
            final JsonParser jp = JSON_FACTORY.createParser(inputStream);

            // Search for the start of the "layer" object
            while (jp.getCurrentToken() != JsonToken.FIELD_NAME || !"layer".equals(jp.getCurrentName())) {
                if (jp.nextToken() == null) {
                    LOGGER.error("No Layer object found");
                    throw new EOFException("Unable to parse JSON: no Layer object found");
                }
            }

            // Read and extract each field
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                    switch (jp.getCurrentName()) {
                        case "id_layer" -> {
                            jp.nextToken();
                            jID_Layer = jp.getText();
                        }
                        case "size" -> {
                            jp.nextToken();
                            jsize = jp.getIntValue();
                        }
                        case "image_owner" -> {
                            jp.nextToken();
                            jimage_owner = jp.getIntValue();
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to parse Layer from JSON", e);
            throw e;
        }

        if (jID_Layer != null && jsize > 0 && jimage_owner > 0) {
            l = new Layer(jID_Layer, jsize, jimage_owner);
        }

        return l;
    }
}

