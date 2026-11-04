package it.unipd.dei.webapp.dope.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides foundational support for DAO (Data Access Object) implementations.
 *
 * <p>This abstract class is designed with two main goals:</p>
 * <ol>
 *     <li>To ensure a consistent approach to database access and exception handling for all DAO classes.</li>
 *     <li>To promote independence from any specific DBMS, enabling upper application layers to change the underlying DBMS without code modifications.</li>
 * </ol>
 *
 * <p>To achieve the first goal, {@code AbstractDAO} provides a framework that handles:
 * the database connection, transaction commit or rollback, and error handling in a unified way.</p>
 *
 * <p>To achieve the second goal, {@code AbstractDAO} serves as the root of a hierarchy of helper classes
 * that isolate DBMS-dependent code as much as possible.</p>
 *
 * <p>Subclasses must implement the {@link #doAccess()} method to define the actual logic for accessing the database.
 * This method is invoked by {@link #access()}, which handles the connection lifecycle and exception management.</p>
 *
 * <p>If needed, subclasses may override {@link #access()} to customize how the connection or transaction is managed,
 * although the default implementation aims to cover most use cases efficiently.</p>
 *
 * @param <T> the type of the output parameter returned by the DAO.
 */
public abstract class AbstractDAO<T> implements DataAccessObject<T> {

    /** A logger available to all subclasses. */
    protected static final Logger LOGGER = LogManager.getLogger(AbstractDAO.class);

    /** The connection used to access the database. */
    protected final Connection con;

    /** The output parameter resulting from the database operation. */
    protected T outputParam = null;

    /** Indicates whether the database operation has already been performed. */
    private boolean accessed = false;

    /** A lock object used for synchronization. */
    private final Object lock = new Object();

    /**
     * Constructs a new DAO with the specified database connection.
     *
     * @param con the database connection to be used.
     * @throws NullPointerException if the connection is {@code null}.
     */
    protected AbstractDAO(final Connection con) {
        if (con == null) {
            LOGGER.error("The connection cannot be null.");
            throw new NullPointerException("The connection cannot be null.");
        }

        this.con = con;

        try {
            // Ensure that auto-commit is enabled
            con.setAutoCommit(true);
            LOGGER.debug("Auto-commit set to default value true.");
        } catch (final SQLException e) {
            LOGGER.warn("Unable to set connection auto-commit to true.", e);
        }
    }

    /**
     * Performs the database access logic by invoking {@link #doAccess()},
     * and manages the connection lifecycle including rollback on failure.
     *
     * <p>This method can only be called once per DAO instance. Subsequent calls will result in an exception.</p>
     *
     * @return the current DAO instance
     * @throws SQLException if a database error occurs during access or connection management
     */
    public DataAccessObject<T> access() throws SQLException {

        synchronized (lock) {
            try {
                if (accessed) {
                    LOGGER.error("Cannot use a DataAccessObject more than once.");
                    throw new SQLException("Cannot use a DataAccessObject more than once.");
                }
            } finally {
                accessed = true;
            }
        }

        try {
            doAccess();

            try {
                con.close();
                LOGGER.debug("Connection successfully closed.");
            } catch (final SQLException e) {
                LOGGER.error("Unable to close the connection to the database.", e);
                throw e;
            }
        } catch (final Throwable t) {
            LOGGER.error("Unable to perform the requested database access operation.", t);

            try {
                if (!con.getAutoCommit()) {
                    con.rollback();
                    LOGGER.info("Transaction successfully rolled back.");
                }
            } catch (final SQLException e) {
                LOGGER.error("Unable to roll back the transaction.", e);
            } finally {
                try {
                    con.close();
                    LOGGER.debug("Connection successfully closed.");
                } catch (final SQLException e) {
                    LOGGER.error("Unable to close the connection to the database.", e);
                }
            }

            if (t instanceof SQLException) {
                throw (SQLException) t;
            } else {
                throw new SQLException("Unable to perform the requested database access operation.", t);
            }
        }

        return this;
    }

    /**
     * Returns the output parameter resulting from the database access.
     *
     * @return the output parameter
     * @throws IllegalStateException if called before {@link #access()} has been executed
     */
    @Override
    public final T getOutputParam() {
        synchronized (lock) {
            if (!accessed) {
                LOGGER.error("Cannot retrieve the output parameter before accessing the database.");
                throw new IllegalStateException("Cannot retrieve the output parameter before accessing the database.");
            }
        }

        return outputParam;
    }

    /**
     * Performs the actual logic for accessing the database.
     * <p>
     * Subclasses must implement this method to define how to interact with the database.
     *
     * @throws Exception if any error occurs during database access
     */
    protected abstract void doAccess() throws Exception;
}

