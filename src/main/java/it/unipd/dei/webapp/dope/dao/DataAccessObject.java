package it.unipd.dei.webapp.dope.dao;

import java.sql.SQLException;

/**
 * Represents a generic Data Access Object (DAO).
 *
 * <p>The {@link #access()} method performs the actual interaction with the database,
 * while the {@link #getOutputParam()} method allows retrieval of any output parameters resulting from the operation,
 * if present.</p>
 *
 * @param <T> the type of the output parameter returned by the DAO.
 */
public interface DataAccessObject<T> {

    /**
     * Executes the database access logic.
     *
     * @return a reference to this {@code DataAccessObject} instance.
     *
     * @throws SQLException if an error occurs while accessing the database.
     */
    DataAccessObject<T> access() throws SQLException;

    /**
     * Retrieves the output parameter after the database access has been performed.
     *
     * @return the output parameter, or {@code null} if no output is produced.
     */
    T getOutputParam();
}
