package it.unipd.dei.webapp.dope.resource;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The {@code Message} class is used for representing a message, which can either be a generic message or an error message.
 * <p>
 * It contains details such as the message content, an optional error code, and additional error details, if applicable.
 * This class is useful for returning messages in API responses, allowing clients to distinguish between regular messages
 * and error messages.
 */
public class Message extends AbstractResource {

    /**
     * The message content.
     */
    private final String message;

    /**
     * The error code, if any.
     */
    private final String errorCode;

    /**
     * Additional details about the error, if any.
     */
    private final String errorDetails;

    /**
     * Indicates whether the message is about an error or not.
     */
    private final boolean isError;

    /**
     * Creates an error message with a specific error code and additional details.
     *
     * @param message       the message content.
     * @param errorCode    the error code associated with the error.
     * @param errorDetails additional details about the error.
     */
    public Message(final String message, final String errorCode, final String errorDetails) {
        this.message = message;
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
        this.isError = true;
    }

    /**
     * Creates a generic message (non-error message).
     *
     * @param message the message content.
     */
    public Message(final String message) {
        this.message = message;
        this.errorCode = null;
        this.errorDetails = null;
        this.isError = false;
    }

    /**
     * Returns the message content.
     *
     * @return the message content.
     */
    public final String getMessage() {
        return message;
    }

    /**
     * Returns the error code associated with the message, if it is an error message.
     *
     * @return the error code, or {@code null} if not an error message.
     */
    public final String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns additional details about the error, if applicable.
     *
     * @return the error details, or {@code null} if not an error message.
     */
    public final String getErrorDetails() {
        return errorDetails;
    }

    /**
     * Indicates whether the message is an error message or not.
     *
     * @return {@code true} if the message is an error, {@code false} otherwise.
     */
    public final boolean isError() {
        return isError;
    }

    /**
     * Writes the message in JSON format to the specified output stream.
     * <p>
     * If the message is an error, it includes the error code and error details in the output.
     *
     * @param out the output stream to write the JSON representation of the message to.
     * @throws IOException if an I/O error occurs while writing the JSON.
     */
    @Override
    protected void writeJSON(final OutputStream out) throws IOException {
        final JsonGenerator jg = JSON_FACTORY.createGenerator(out);

        jg.writeStartObject();

        jg.writeFieldName("message");

        jg.writeStartObject();

        jg.writeStringField("message", message);

        if (errorCode != null) {
            jg.writeStringField("error-code", errorCode);
        }

        if (errorDetails != null) {
            jg.writeStringField("error-details", errorDetails);
        }

        jg.writeEndObject();

        jg.writeEndObject();

        jg.flush();
    }
}
