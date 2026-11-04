package it.unipd.dei.webapp.dope.resource;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The {@code AbstractResource} class serves as a base class for any resource
 * that can be serialized into JSON format. It provides common functionality
 * for serializing objects to JSON and error handling. Subclasses are expected
 * to implement the {@link #writeJSON(OutputStream)} method to define their
 * specific JSON serialization logic.
 */
public abstract class AbstractResource implements Resource {


    /** The logger used for logging messages related to resource serialization. */
    protected static final Logger LOGGER = LogManager.getLogger(AbstractResource.class);

    /** The JSON factory to be used for creating JSON parsers and generators. */
    protected static final JsonFactory JSON_FACTORY;

    static {
        // set up the JSON factory
        JSON_FACTORY = new JsonFactory();
        JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        JSON_FACTORY.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

        LOGGER.debug("JSON factory successfully setup.");
    }

    /**
     * Converts the current resource into JSON and writes it to the provided output stream.
     * Delegates the actual writing process to the {@link #writeJSON(OutputStream)} method.
     *
     * @param out the output stream where the JSON representation of the resource will be written.
     * @throws IOException if an I/O error occurs or the output stream is {@code null}.
     */
    @Override
    public void toJSON(final OutputStream out) throws IOException {

        if(out == null) {
            LOGGER.error("The output stream cannot be null.");
            throw new IOException("The output stream cannot be null.");
        }

        try {
            // delegates writing to subclsses
            writeJSON(out);
        } catch(Exception e) {
            LOGGER.error("Unable to serialize the resource to JSON.", e);
            throw new IOException("Unable to serialize the resource to JSON.", e);
        }

    }

    /**
     * Performs the actual writing of JSON.
     *
     * Subclasses have to implement this method to provide the actual logic needed for representing the {@code
     * Resource} to JSON.
     *
     * @param out the stream to which the JSON representation of the {@code Resource} has to be written.
     *
     * @throws Exception if something goes wrong during writing.
     */
    protected abstract void writeJSON(OutputStream out) throws Exception;
}

