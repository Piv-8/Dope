package it.unipd.dei.webapp.dope.resource;

import jakarta.servlet.http.HttpServletResponse;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;

import static it.unipd.dei.webapp.dope.resource.AbstractResource.LOGGER;

/**
 * Defines a collection of standardized error codes used in REST responses.
 * <p>
 * These constants represent specific error situations encountered during
 * HTTP request processing, such as missing media types, unsupported operations,
 * resource conflicts, or server-side issues.
 * <p>
 * The format of the error codes follows a naming convention (e.g., {@code E4A1}, {@code E5A3})
 * to help categorize and identify errors programmatically.
 * <p>
 * This class is not meant to be instantiated.
 */
public final class Error {

    /**
     * Private constructor to prevent instantiation.
     * Throws an {@link AssertionError} if called.
     */
    private Error() {
        throw new AssertionError(String.format("No instances of %s allowed.", Error.class.getName()));
    }

    /** Error code indicating that the URI is malformed or incorrect. */
    public static final String WRONG_URI = "E4A7";

    /** Error code indicating that the wrong resource was provided in the request body. */
    public static final String WRONG_RESOURCE_PROVIDED = "E4A8";

    /** Error code indicating that an authentication error occurred. */
    public static final String AUTHENTICATION_ERROR = "E4A9";

    /** Error code indicating a general failure in processing the resource. */
    public static final String ERROR_PROCESSING_RESOURCE = "E5A1";

    /** Error code indicating a conflict in resource creation or update. */
    public static final String CONFLICT = "E5A2";

    /** Error code indicating that the requested resource was not found. */
    public static final String RESOURCE_NOT_FOUND = "E5A3";

    /** Error code indicating that the user is not authorized to perform the requested operation. */
    public static final String NOT_AUTHORIZED = "E5A4";

    /**Error code indicating that the method used is not allowed for that URI */
    public static final String METHOD_NOT_ALLOWED = "E5A5";

    /** Error code indicating an internal server error caused by an SQL exception. */
    public static final String INTERNAL_SERVER_ERROR_SQL = "E500_SQL";

    /** Error code indicating an internal server error caused by an unexpected technical condition. */
    public static final String INTERNAL_SERVER_ERROR_TC = "E500_TC";


}

