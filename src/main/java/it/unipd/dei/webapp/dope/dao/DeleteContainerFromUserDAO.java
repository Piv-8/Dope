package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.User;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes all containers associated with a specific user from the database.
 *
 * <p>This DAO removes every container entry that belongs to a given user,
 * identified by their {@code user_instance} field in the {@code container} table.</p>
 *
 * <p>If one or more rows are deleted, the operation is considered successful.
 * A corresponding log message will indicate whether containers were found and deleted or not.</p>
 */
public class DeleteContainerFromUserDAO extends AbstractDAO<List<Container>> {

    /** The SQL statement to delete all containers associated with a specific user. */
    private static final String STATEMENT = """
                                            DELETE FROM dope."container" WHERE "user_instance" = ? RETURNING *
                                            """;
    /** The ID of the user whose containers should be deleted. */
    private final Integer userId;

    /**
     * Creates a new {@code DeleteContainerFromUserDAO} object for deleting containers of a specific user.
     *
     * @param con    the connection to be used for accessing the database.
     * @param userId the ID of the user whose containers will be deleted.
     */
    public DeleteContainerFromUserDAO(final Connection con, Integer userId) {
        super(con);
        this.userId = userId;
    }

    /**
     * Executes the SQL DELETE statement to remove containers from the database.
     *
     * <p>Logs whether any containers were actually deleted.
     * Sets the output parameter to a list of {@code Container} objects, regardless of the number of deleted rows.</p>
     *
     * @throws Exception if a database error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.DELETE_CONTAINERS_FROM_USER);
        LogContext.setResource("User ID:" + userId);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        final List<Container> containers = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, userId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                containers.add(new Container(rs.getInt("instance_id"), rs.getString("status"), rs.getInt("image_run"), rs.getInt("user_instance")));
                LOGGER.info("Container {} deleted.", rs.getInt("instance_id"));
            }
            if (containers.isEmpty()) {
                LOGGER.info("No containers for user {}.", userId);
            }

        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }

        LogContext.removeAction();
        LogContext.removeResource();

        this.outputParam = containers;
    }
}








