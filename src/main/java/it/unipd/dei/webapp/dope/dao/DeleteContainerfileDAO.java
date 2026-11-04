package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Containerfile;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Deletes a specific {@link Containerfile} from the database, identified by its name.
 *
 * <p>This DAO removes a {@link Containerfile} from the containerfile table. The containerfile is identified by its primary key,
 * {@code name}. If no containerfile with the provided name exists, no rows will be deleted and an informational
 * log message will be written.</p>
 *
 * <p>The operation always returns {@code true} as output, regardless of whether a containerfile was actually deleted,
 * indicating the execution of the deletion attempt.</p>
 */
public class DeleteContainerfileDAO extends AbstractDAO<Boolean> {

    /** The SQL statement to delete the containerfile with the specified name. */
    private static final String STATEMENT = "DELETE FROM dope.\"containerfile\" WHERE \"name\" = ?";

    /** The name of the containerfile to be deleted. */
    private final String name;

    /**
     * Creates a new {@code DeleteContainerfileDAO} object to delete a specific containerfile.
     *
     * @param con  the connection to be used for accessing the database.
     * @param name the name of the containerfile to be deleted.
     */
    public DeleteContainerfileDAO(final Connection con, final String name){
        super(con);
        this.name = name;
    }

    /**
     * Executes the SQL DELETE statement to remove the containerfile from the database.
     *
     * <p>Logs whether the containerfile was found and successfully deleted or not.</p>
     *
     * @throws Exception if a database error occurs during deletion.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.DELETE_CONTAINERFILE);
        LogContext.setResource("Containerfile NAME:" + name);

        PreparedStatement pstmt = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setString(1, name);
            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                LOGGER.info("Containerfile {} not found. No containerfile deleted.", name);
            } else {
                LOGGER.info("Containerfile {} successfully deleted.", name);
            }
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }

        LogContext.removeResource();
        LogContext.removeAction();

        this.outputParam = true;
    }
}
