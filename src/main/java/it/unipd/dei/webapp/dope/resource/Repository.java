package it.unipd.dei.webapp.dope.resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.servlet.ServletInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The {@code Repository} class represents a repository resource used within the web application,
 * used for managing resources like images or layers.
 * <p>
 * This class includes information such as the repository's ID, name, visibility, and owner ID.
 * <p>
 * It supports JSON serialization and deserialization for use in RESTful web service
 */
public class Repository extends AbstractResource {

    /** The ID of the repository. */
    private int ID_repo;

    /** The name of the repository. */
    private String name;

    /** The visibility status of the repository (public or private). */
    private String visibility;

    /** The ID of the repository owner. */
    private int owner_ID;

    /** Default constructor */
    public Repository() {}

    /**
     * Constructor to create a {@code Repository} with specific values for its fields.
     *
     * @param ID_repo   the ID of the repository.
     * @param name      the name of the repository.
     * @param visibility the visibility status of the repository.
     * @param owner_ID  the ID of the repository owner.
     */
    public Repository(int ID_repo, String name, String visibility, int owner_ID) {
        this.ID_repo = ID_repo;
        this.name = name;
        this.visibility = visibility;
        this.owner_ID = owner_ID;
    }

    /**
     * Returns the ID of the repository.
     *
     * @return the ID of the repository.
     */
    public int getID_repo() {
        return ID_repo;
    }

    /**
     * Sets the ID of the repository.
     *
     * @param ID_repo the ID of the repository.
     */
    public void setID_repo(int ID_repo) {
        this.ID_repo = ID_repo;
    }

    /**
     * Returns the name of the repository.
     *
     * @return the name of the repository.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the repository.
     *
     * @param name the name of the repository.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the visibility status of the repository.
     *
     * @return the visibility status of the repository.
     */
    public String getVisibility() {
        return visibility;
    }

    /**
     * Sets the visibility status of the repository.
     *
     * @param visibility the visibility status of the repository.
     */
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    /**
     * Returns the ID of the repository owner.
     *
     * @return the ID of the repository owner.
     */
    public int getOwner_ID() {
        return owner_ID;
    }

    /**
     * Sets the ID of the repository owner.
     *
     * @param owner_ID the ID of the repository owner.
     */
    public void setOwner_ID(int owner_ID) {
        this.owner_ID = owner_ID;
    }

    /**
     * Serializes the repository object to JSON and writes it to the provided output stream.
     *
     * @param out the output stream to write the JSON representation of the repository.
     * @throws Exception if an error occurs while writing the JSON data.
     */
    @Override
    protected final void writeJSON(final OutputStream out) throws Exception {
        final JsonGenerator jg = JSON_FACTORY.createGenerator(out);

        jg.writeStartObject();

        jg.writeFieldName("repository");

        jg.writeStartObject();

        jg.writeNumberField("id_repo", ID_repo);

        jg.writeStringField("name", name);

        jg.writeStringField("visible", visibility);

        jg.writeNumberField("owner_id", owner_ID);

        jg.writeEndObject();

        jg.writeEndObject();

        jg.flush();
    }

    /**
     * Deserializes a {@code Repository} object from a JSON input stream.
     *
     * @param inputStream the input stream containing the JSON data to deserialize.
     * @return the {@code Repository} object deserialized from the JSON input.
     * @throws IOException if an error occurs while reading the JSON data.
     */
    public static Repository fromJSON(ServletInputStream inputStream) throws IOException {
        int jID_repo = -1;
        String jname = null;
        String jvisibility = null;
        int jowner_ID = -1;

        Repository r = null;

        try {
            final JsonParser jp = JSON_FACTORY.createParser(inputStream);
            // Get head element or see if we're at the end
            while (jp.getCurrentToken() != JsonToken.FIELD_NAME || !"repository".equals(jp.getCurrentName())) {
                if (jp.nextToken() == null) {
                    LOGGER.error("No Repository object found");
                    throw new EOFException("Unable to parse JSON: no Repository object found");
                }
            }

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                    if ("id_repo".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jID_repo = jp.getIntValue();
                    } else if ("name".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jname = jp.getText();
                    } else if ("visible".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jvisibility = jp.getText();
                    } else if ("owner_id".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jowner_ID = jp.getIntValue();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to parse Repository from JSON", e);
            throw e;
        }
        if (jID_repo > 0 && jname != null && jvisibility != null && jowner_ID > 0) {
            r = new Repository(jID_repo, jname, jvisibility, jowner_ID);
        }
        return r;
    }
}
