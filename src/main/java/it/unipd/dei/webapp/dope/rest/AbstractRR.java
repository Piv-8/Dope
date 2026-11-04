package it.unipd.dei.webapp.dope.rest;


import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Message;
import it.unipd.dei.webapp.dope.resource.LogContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * Abstract class that provides common functionality for REST resources.
 *
 * <p>This class is designed to be extended by other classes that handle specific REST operations. It includes common
 * functionality for handling HTTP requests, responses, logging, and verifying method and MIME type compatibility.</p>
 */
public abstract class AbstractRR implements RestResource {


    /**
     * A {@link Logger} available for all the subclasses of this class.
     * <p>This logger is used to log messages related to the operation of the REST resource.</p>
     */
    protected static final Logger LOGGER = LogManager.getLogger(AbstractRR.class);

    /**
     * The JSON MIME media type.
     * <p>Represents the MIME type for JSON content.</p>
     */
    protected static final String JSON_MEDIA_TYPE = "application/json";

    /**
     * The JSON UTF-8 MIME media type.
     * <p>Represents the MIME type for JSON content with UTF-8 encoding.</p>
     */
    protected static final String JSON_UTF_8_MEDIA_TYPE = "application/json; charset=utf-8";

    /**
     * The any MIME media type.
     * <p>Represents a wildcard MIME type that accepts any media type.</p>
     */
    protected static final String ALL_MEDIA_TYPE = "*/*";

    /**
     * The HTTP request associated with the REST resource.
     * <p>This field holds the HTTP request that is being processed for the current REST operation.</p>
     */
    protected final HttpServletRequest req;

    /**
     * The HTTP response associated with the REST resource.
     * <p>This field holds the HTTP response that will be sent after processing the request.</p>
     */
    protected final HttpServletResponse res;

    /**
     * The connection pool to the database.
     * <p>This resource is used to create new connections to with the database during the execution of the REST resource.</p>
     */
    private DataSource ds;

    /**
     * The action performed by this REST resource.
     * <p>This field holds the action that describes the operation being performed by the REST resource.</p>
     */
    private final String action;

    /**
     * Creates a new instance of the REST resource.
     *
     * <p>This constructor initializes the necessary fields including the action, HTTP request, HTTP response, and
     * database connection.</p>
     *
     * @param action the action performed by this REST resource.
     * @param req    the HTTP request.
     * @param res    the HTTP response.
     * @throws NullPointerException if any of the arguments are {@code null}.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    protected AbstractRR(final String action, final HttpServletRequest req, final HttpServletResponse res) throws ServletException {

        if (action == null || action.isEmpty()) {
            LOGGER.warn("Action is null or empty.");
        }
        this.action = action;
        LogContext.setAction(action);

        if (req == null) {
            LOGGER.error("The HTTP request cannot be null.");
            throw new NullPointerException("The HTTP request cannot be null.");
        }
        this.req = req;

        if (res == null) {
            LOGGER.error("The HTTP response cannot be null.");
            throw new NullPointerException("The HTTP response cannot be null.");
        }
        this.res = res;

        // the JNDI lookup context
        InitialContext cxt;

        try {
            cxt = new InitialContext();
            ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/dope");

            LOGGER.info("Connection pool to the database pool successfully acquired.");
        } catch (NamingException e) {
            ds = null;

            LOGGER.error("Unable to acquire the connection pool to the database.", e);

            throw new ServletException("Unable to acquire the connection pool to the database", e);
        }
    }


    /**
     * Serves the REST request.
     *
     * <p>This method handles the process of checking the HTTP request method and MIME type, and then invokes the actual
     * logic to serve the request. If any errors occur, an appropriate message is logged, and an error response is sent.</p>
     *
     * @throws IOException if an error occurs in the client/server communication.
     */

    @Override
    public void serve() throws IOException {

        try {
            // if the request method and/or the MIME media type are not allowed, return.
            // Appropriate error message sent by {@code checkMethodMediaType}
            if (!checkMethodMediaType(req, res)) {
                return;
            }

            doServe();
        } catch (Throwable t) {
            LOGGER.error("Unable to serve the REST request.", t);

            final Message m = new Message(String.format("Unable to serve the REST request: %s.", action), "E5A1",
                    t.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } finally {
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }

    /**
     * Performs the actual logic needed for serving the REST request.
     *
     * <p>Subclasses must implement this method to define the specific logic for handling the REST request.</p>
     *
     * @throws IOException if any error occurs in the client/server communication.
     */
    protected abstract void doServe() throws IOException;

    /**
     * Checks that the HTTP request method and MIME media type are compatible with this REST resource.
     *
     * <p>This method verifies if the request method (e.g., GET, POST) and the MIME media type (e.g., JSON) are supported
     * by the REST resource. It may be overridden by subclasses to support different content types or methods.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @return {@code true} if the request method and MIME type are allowed; {@code false} otherwise.
     * @throws IOException if any error occurs in the client/server communication.
     */
    protected boolean checkMethodMediaType(final HttpServletRequest req, final HttpServletResponse res) throws
            IOException {

        final String method = req.getMethod();
        final String contentType = req.getHeader("Content-Type");
        final String accept = req.getHeader("Accept");
        final OutputStream out = res.getOutputStream();

        Message m = null;

        if (accept == null) {
            LOGGER.error("Output media type not specified. Accept request header missing.");
            m = new Message("Output media type not specified.", "E4A1", "Accept request header missing.");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(out);
            return false;
        }

        if (!accept.contains(JSON_MEDIA_TYPE) && !accept.equals(ALL_MEDIA_TYPE)) {
            LOGGER.error(
                    "Unsupported output media type. Resources are represented only in application/json. Requested representation is {}.",
                    accept);
            m = new Message("Unsupported output media type. Resources are represented only in application/json.",
                    "E4A2", String.format("Requested representation is %s.", accept));
            res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            m.toJSON(out);
            return false;
        }

        // if the method is supposed to send a body, check its MIME media type
        switch (method) {
            case "GET":
            case "DELETE":
                // nothing to do
                break;

            case "POST":
            case "PUT":
                if (contentType == null) {
                    LOGGER.error("Input media type not specified. Content-Type request header missing.");
                    m = new Message("Input media type not specified.", "E4A3", "Content-Type request header missing.");
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    m.toJSON(out);
                    return false;
                }

                if (!contentType.contains(JSON_MEDIA_TYPE)) {
                    LOGGER.error(
                            "Unsupported input media type. Resources are represented only in application/json. Submitted representation is {}.",
                            contentType);
                    m = new Message("Unsupported input media type. Resources are represented only in application/json.",
                            "E4A4", String.format("Submitted representation is %s.", contentType));
                    res.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    m.toJSON(out);
                    return false;
                }

                break;
            default:
                LOGGER.error("Unsupported operation. Requested operation {}.", method);
                m = new Message("Unsupported operation.", "E4A5", String.format("Requested operation %s.", method));
                res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                m.toJSON(out);
                return false;
        }

        return true;
    }

    /**
     * Returns a {@link Connection} for accessing the database.
     * <p>This method provides a {@link Connection} object from the connection pool for database interactions.
     * If any error occurs while acquiring the connection, a {@link SQLException} is thrown.</p>
     *
     * @return a {@link Connection} for accessing the database.
     *
     * @throws SQLException if anything goes wrong in obtaining the connection.
     */
    protected final Connection getConnection() throws SQLException {
        try {
            return ds.getConnection();
        } catch (final SQLException e) {
            LOGGER.error("Unable to acquire the connection from the pool.", e);
            throw e;
        }
    }

}
