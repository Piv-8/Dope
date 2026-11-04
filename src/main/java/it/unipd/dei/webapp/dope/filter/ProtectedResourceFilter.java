package it.unipd.dei.webapp.dope.filter;

import it.unipd.dei.webapp.dope.dao.AuthenticateUserDAO;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Base64;

/**
 * A filter that protects resources by ensuring the user is authenticated before allowing access.
 *
 * <p>The filter checks if the user is authenticated by looking for an active session and user details within it. If no session
 * is found, or if the session does not contain valid user data, the filter attempts to authenticate the user using Basic
 * Authentication. If the user is successfully authenticated, a session is created and the request proceeds. Otherwise,
 * an authentication challenge is sent to the client.</p>
 *
 * <p>The filter interacts with a database to verify user credentials and uses Basic Authentication for user authentication.</p>
 */
public class ProtectedResourceFilter implements Filter {

    /** A LOGGER available for all the subclasses. */
    protected static final Logger LOGGER = LogManager.getLogger(ProtectedResourceFilter.class);

    /** The Base64 Decoder. */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    /** The name of the user attribute in the session. */
    public static final String USER_ATTRIBUTE = "user";

    /** The configuration for the filter. */
    private FilterConfig config = null;

    /** The connection pool to the database. */
    private DataSource ds;

    /**
     * Initializes the filter, including database connection pool setup.
     *
     * @param config the filter configuration.
     * @throws ServletException if an error occurs during initialization.
     */
    @Override
    public void init(final FilterConfig config) throws ServletException {
        if (config == null) {
            LOGGER.error("Filter configuration cannot be null.");
            throw new ServletException("Filter configuration cannot be null.");
        }
        this.config = config;

        // the JNDI lookup context
        InitialContext cxt;

        try {
            cxt = new InitialContext();
            ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/dope");
        } catch (NamingException e) {
            ds = null;
            LOGGER.error("Unable to acquire the connection pool to the database.", e);
            throw new ServletException("Unable to acquire the connection pool to the database", e);
        }
    }

    /**
     * Filters the HTTP request and response to ensure the user is authenticated before accessing the protected resource.
     *
     * @param servletRequest the HTTP request.
     * @param servletResponse the HTTP response.
     * @param chain the filter chain to continue the processing.
     * @throws IOException if an I/O error occurs during filtering.
     * @throws ServletException if any error occurs during filtering.
     */
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws
            IOException, ServletException {

        LogContext.setIPAddress(servletRequest.getRemoteAddr());
        LogContext.setAction(Actions.FILTER);

        try {
            if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
                LOGGER.error("Only HTTP requests/responses are allowed.");
                throw new ServletException("Only HTTP requests/responses are allowed.");
            }

            final HttpServletRequest req = (HttpServletRequest) servletRequest;
            final HttpServletResponse res = (HttpServletResponse) servletResponse;

            LogContext.setResource(req.getRequestURI());
            LOGGER.info("request URL = {}", req.getRequestURL());

            final HttpSession session = req.getSession(false);

            // if we do not have a session, try to authenticate the user
            if (session == null) {

                LOGGER.warn("Authentication required to access resource {} with method {}.", req.getRequestURI(),
                        req.getMethod());

                if (!authenticateUser(req, res)) {
                    return;
                }
            } else {

                final String user = (String) session.getAttribute(USER_ATTRIBUTE);

                // there might exist a session but without any user in it
                if (user == null || user.isBlank()) {

                    // invalidate the session
                    session.invalidate();

                    LOGGER.warn(
                            "Authentication required to access resource {} with method {}. Session {} exists but no user found in session. Session invalidated.",
                            req.getRequestURI(), req.getMethod(), session.getId());

                    // try to authenticate the user
                    if (!authenticateUser(req, res)) {
                        return;
                    }

                }
            }

            // the user is properly authenticated and in session, continue the processing
            chain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            LOGGER.error("Unable to perform the protected resource filtering.", e);
            throw e;
        } finally {
            LogContext.removeUser();
            LogContext.removeIPAddress();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }

    /**
     * Destroys the filter, releasing any resources held.
     */
    @Override
    public void destroy() {
        config = null;
        ds = null;
    }

    /**
     * Authenticates the user using Basic Authentication.
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @return {@code true} if the user has been successfully authenticated; {@code false} otherwise.
     */
    private boolean authenticateUser(HttpServletRequest req, HttpServletResponse res) {

        LogContext.setAction(Actions.AUTHENTICATE_USER);
        LOGGER.info("Trying to authenticate the user ");

        try {
            final String auth = req.getHeader("Authorization");

            if (auth == null || auth.isBlank()) {
                LOGGER.info("No authorization header sent by the client.");
                res.sendRedirect(req.getContextPath() + "/login");
                return false;
            }

            if (!auth.toUpperCase().startsWith("BASIC ")) {
                LOGGER.warn("Basic authentication is expected. Clients sent instead: {}", auth);
                res.sendRedirect(req.getContextPath() + "/login");
                return false;
            }

            final String pair = new String(DECODER.decode(auth.substring(6)));
            final String[] userDetails = pair.split(":", 2);

            LogContext.setUser(userDetails[0]);
            LOGGER.info("User: {}, Password: {}", userDetails[0], userDetails[1]);

            if (new AuthenticateUserDAO(ds.getConnection(), userDetails[0], userDetails[1]).access().getOutputParam()) {
                HttpSession session = req.getSession(true);
                session.setAttribute(USER_ATTRIBUTE, userDetails[0]);
                return true;
            }

            res.sendRedirect(req.getContextPath() + "/login");
        } catch (Exception e) {
            LOGGER.error("Unable to authenticate the user.", e);
        } finally {
            LogContext.removeAction();
        }

        return false;
    }

    /**
     * Sends the authentication challenge to the client.
     *
     * @param res the HTTP response.
     * @throws IOException if an error occurs while sending the authentication challenge.
     */
    private void sendAuthenticationChallenge(HttpServletResponse res) throws IOException {
        try {
            res.setHeader("WWW-Authenticate", "Basic realm=dope");
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            LOGGER.info("Basic Authentication Challenge sent.");
        } catch (Exception e) {
            LOGGER.error("Unable to send authentication challenge.", e);
            throw e;
        }
    }

}

