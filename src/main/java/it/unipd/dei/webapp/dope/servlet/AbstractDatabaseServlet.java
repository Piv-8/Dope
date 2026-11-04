package it.unipd.dei.webapp.dope.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * An abstract base servlet class that manages the connection pool to a database.
 * <p>This class provides the necessary functionality to acquire and release a connection pool to the database. It also
 * provides a method for subclasses to obtain a database connection. It extends {@link HttpServlet} and is intended to be
 * used as a superclass for servlets that need to interact with a database.</p>
 *
 * <p>Subclasses can call {@link #getConnection()} to retrieve a {@link Connection} to interact with the database.</p>
 */
public abstract class AbstractDatabaseServlet extends HttpServlet {

    /**
     * A LOGGER available for all the subclasses.
     */
    protected static final Logger LOGGER = LogManager.getLogger(AbstractDatabaseServlet.class);

    /**
     * The connection pool to the database.
     */
    private DataSource ds;

    /**
     * Initializes the servlet and acquires the connection pool to the database from JNDI.
     * <p>This method is called during servlet initialization and attempts to acquire a connection pool from JNDI
     * using the name "java:/comp/env/jdbc/dope". If successful, it logs the acquisition. If the acquisition fails,
     * it throws a {@link ServletException}.</p>
     *
     * @param config a {@code ServletConfig} object containing the servlet's configuration and initialization parameters.
     *
     * @throws ServletException if an exception has occurred that interferes with the servlet's normal operation.
     */
    public void init(ServletConfig config) throws ServletException {

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
     * Releases the connection pool to the database.
     * <p>This method is called when the servlet is destroyed and ensures the connection pool is released.</p>
     */
    public void destroy() {
        ds = null;
        LOGGER.info("Connection pool to the database pool successfully released.");
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
