package it.unipd.dei.webapp.dope.resource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.servlet.ServletInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;

/**
 * The {@code User} class represents a user entity used within the web application.
 * It has personal details, including an ID, username, password,name, surname, and birthdate.
 * <p>
 * It supports JSON serialization and deserialization for use in RESTful web service
 */
public class User extends AbstractResource {

    /** The unique ID of the user. */
    private final int ID_User;

    /** The username associated with the user. */
    private final String username;

    /** The password of the user. */
    private final String password;

    /** The first name of the user. */
    private final String name;

    /** The last name (surname) of the user. */
    private final String surname;

    /** The birthdate of the user. */
    private final String birthDate;

    /**
     * Constructor to create a {@code User} with specific values for its fields.
     *
     * @param ID_User    the unique ID of the user.
     * @param username   the username of the user.
     * @param password   the password of the user.
     * @param name       the first name of the user.
     * @param surname    the surname of the user.
     * @param birthDate  the birthdate of the user.
     */
    public User(int ID_User, String username, String password, String name, String surname, String birthDate) {
        this.ID_User = ID_User;
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
    }

    /**
     * Returns the unique ID of the user.
     *
     * @return the unique ID of the user.
     */
    public int getID_User() {
        return ID_User;
    }

    /**
     * Returns the username of the user.
     *
     * @return the username of the user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password of the user.
     *
     * @return the password of the user.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the first name of the user.
     *
     * @return the first name of the user.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the surname (last name) of the user.
     *
     * @return the surname (last name) of the user.
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Returns the birthdate of the user.
     *
     * @return the birthdate of the user.
     */
    public String getBirthDate() {
        return birthDate;
    }

    /**
     * Serializes the {@code User} object to JSON and writes it to the provided output stream.
     *
     * @param out the output stream to write the JSON representation of the user.
     * @throws Exception if an error occurs while writing the JSON data.
     */
    @Override
    protected final void writeJSON(final OutputStream out) throws Exception {
        final JsonGenerator jg = JSON_FACTORY.createGenerator(out);

        jg.writeStartObject();

        jg.writeFieldName("user");

        jg.writeStartObject();

        jg.writeNumberField("id_user", ID_User);

        jg.writeStringField("username", username);

        jg.writeStringField("password", password);

        jg.writeStringField("name", name);

        jg.writeStringField("surname", surname);

        jg.writeStringField("birth_date", birthDate);

        jg.writeEndObject();

        jg.writeEndObject();

        jg.flush();
    }

    /**
     * Deserializes a {@code User} object from a JSON input stream.
     *
     * @param inputStream the input stream containing the JSON data to deserialize.
     * @return the {@code User} object deserialized from the JSON input.
     * @throws IOException if an error occurs while reading the JSON data.
     */
    public static User fromJSON(ServletInputStream inputStream) throws IOException {
        int jID_User = -1;
        String jusername = null;
        String jpassword = null;
        String jname = null;
        String jsurname = null;
        String jbirthDate = null;

        User u = null;

        try {
            final JsonParser jp = JSON_FACTORY.createParser(inputStream);
            // Get head element or see if we're at the end
            while (jp.getCurrentToken() != JsonToken.FIELD_NAME || !"user".equals(jp.getCurrentName())) {
                if (jp.nextToken() == null) {
                    LOGGER.error("No User object found");
                    throw new EOFException("Unable to parse JSON: no User object found");
                }
            }

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                    if ("id_user".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jID_User = jp.getIntValue();
                    } else if ("username".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jusername = jp.getText();
                    } else if ("password".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jpassword = jp.getText();
                    } else if ("name".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jname = jp.getText();
                    } else if ("surname".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jsurname = jp.getText();
                    } else if ("birth_date".equals(jp.getCurrentName())) {
                        jp.nextToken();
                        jbirthDate = jp.getText();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to parse User from JSON", e);
            throw e;
        }
        if (jID_User > 0 && jusername != null && jpassword != null && jname != null && jsurname != null && jbirthDate != null) {
            u = new User(jID_User, jusername, jpassword, jname, jsurname, jbirthDate);
        }
        return u;
    }
}
