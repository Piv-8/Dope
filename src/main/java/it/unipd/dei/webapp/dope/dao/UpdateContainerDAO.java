package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Updates a {@link Container} in the database.
 *
 * <p>This DAO class performs an update operation on an existing {@link Container} based on the user's username and the specified container data.
 * It retrieves the container associated with the given user and updates the container's attributes if necessary. The updated {@link Container}
 * object is returned as the result.</p>
 */
public class UpdateContainerDAO extends AbstractDAO<Container> {

    /** The SQL query to retrieve the container for a specific user. */
    private static final String STATEMENT_SELECT = """ 
                                            SELECT "instance_id", "status", "image_run", "user_instance", "user".id_user
                                            FROM dope."container" 
                                            INNER JOIN dope."user" ON dope."user".username = ?
                                            WHERE "user_instance" = dope."user".id_user ORDER BY "instance_id" DESC LIMIT 1""";

    /** The {@link Container} object that contains the data to be updated in the database. */
    private final Container container;

    /** The username of the user associated with the container. */
    private final String username;


    /**
     * Creates a new {@code UpdateContainerDAO} object for searching users in the database.
     *
     * @param con the connection to be used for accessing the database.
     * @param username the username of the user associated with the container.
     * @param container the container object containing the updated data.
     */
    public UpdateContainerDAO(final Connection con, String username, final Container container){
        super(con);
        this.container = container;
        this.username = username;
    }

    /**
     * Executes the update operation on the {@link Container}.
     *
     * <p>This method retrieves the {@link Container} associated with the provided username, constructs the necessary SQL update statement, and
     * applies the changes to the {@link Container} in the database. If successful, the updated container is returned in the {@code outputParam} field.
     * Otherwise, an appropriate message is logged.</p>
     *
     * @throws SQLException if a database access error occurs or no container is found for the given user.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.UPDATE_CONTAINER);
        LogContext.setResource("Container ID:" + container.getInstanceId());

        PreparedStatement pstmnt = null;
        ResultSet rs = null;

        Container c = null;
        try {
            // Retrieve the user's container
            pstmnt = con.prepareStatement(STATEMENT_SELECT);
            pstmnt.setString(1, username);

            rs = pstmnt.executeQuery();

            if (rs.next()) {
                c = new Container(
                        rs.getInt("instance_id"),
                        rs.getString("status"),
                        rs.getInt("image_run"),
                        rs.getInt("user_instance")
                );
            } else {
                throw new SQLException("No container found for user " + username);
            }

            // Build the update query dynamically
            String update_statement = "UPDATE dope.\"container\"";
            List<String> clauses = new ArrayList<>();

            if (container.getInstanceId() > 0) { clauses.add(" \"instance_id\" = ?"); }
            if (container.getStatus() != null) { clauses.add(" \"status\" = ?::dope.status_container"); }
            if (container.getImageRun() > 0) { clauses.add(" \"image_run\" = ?"); }
            if (container.getUserInstance() > 0) { clauses.add(" \"user_instance\" = ?"); }

            StringBuilder sb = new StringBuilder();
            sb.append(update_statement);
            sb.append(" SET");
            for (int i = 0; i < clauses.size(); i++) {
                sb.append(clauses.get(i));
                if (i < clauses.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append(" WHERE \"instance_id\" = ? RETURNING *;");

            // Prepare and execute the update query
            pstmnt = con.prepareStatement(sb.toString());

            int clauseCount = 1;

            if (container.getInstanceId() > 0) {
                pstmnt.setInt(clauseCount, container.getInstanceId());
                clauseCount++;
            }
            if (container.getStatus() != null) {
                pstmnt.setString(clauseCount, container.getStatus());
                clauseCount++;
            }
            if (container.getImageRun() > 0) {
                pstmnt.setInt(clauseCount, container.getImageRun());
                clauseCount++;
            }
            if (container.getUserInstance() > 0) {
                pstmnt.setInt(clauseCount, container.getUserInstance());
                clauseCount++;
            }

            pstmnt.setInt(clauseCount, c.getInstanceId());

            rs = pstmnt.executeQuery();

            if (rs.next()) {
                c = new Container(
                        rs.getInt("instance_id"),
                        rs.getString("status"),
                        rs.getInt("image_run"),
                        rs.getInt("user_instance")
                );
                LOGGER.info("Container {} successfully updated in the database.",
                        c.getInstanceId());
            } else {
                LOGGER.info("No container updated for instance ID {}", container.getInstanceId());
            }

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pstmnt != null) {
                pstmnt.close();
            }
        }

        LogContext.removeAction();
        LogContext.removeResource();

        outputParam = c;
    }
}
