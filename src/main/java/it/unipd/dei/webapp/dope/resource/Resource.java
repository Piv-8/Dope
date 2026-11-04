package it.unipd.dei.webapp.dope.resource;
import java.io.IOException;
import java.io.OutputStream;


/**
 * The {@code Resource} interface defines the structure for objects
 * that can be serialized into JSON format.
 * Implementing classes should provide their own implementation
 * of the {@code toJSON} method to convert their state into a JSON representation.
 */
public interface Resource {

    /**
     * Serializes the current resource into JSON format and writes it to the provided {@link OutputStream}.
     *
     * @param out the output stream where the JSON representation of the resource will be written.
     * @throws IOException if an I/O error occurs while writing to the output stream.
     */
    void toJSON(OutputStream out) throws IOException;

}