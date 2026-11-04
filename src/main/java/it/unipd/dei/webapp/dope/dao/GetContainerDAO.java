package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Retrieves the most recently created {@link Container} associated with a specific {@link User} from the database.
 *
 * <p>This DAO performs a query to fetch the latest {@link Container} (based on descending {@code instance_id})
 * linked to a user identified by their username. The result is encapsulated in a {@link Container} object.
 * If no container is found, the output is {@code null}.</p>
 */
public class GetContainerDAO extends AbstractDAO<Container> {

    /** The SQL query used to retrieve the latest container associated with a given user. */
    private static final String STATEMENT = """ 
        SELECT "instance_id", "status", "image_run", "user_instance", dope."user".id_user
        FROM dope."container" 
        INNER JOIN dope."user" ON dope."user".username = ?
        WHERE "user_instance" = dope."user".id_user
        ORDER BY "instance_id" DESC LIMIT 1
        """;

    /** The username of the user whose latest container will be retrieved. */
    private final String user;

    /**
     * Creates a new {@code GetContainerDAO} object.
     *
     * @param con  the connection to be used for accessing the database.
     * @param user the username of the user whose latest container will be retrieved.
     */
    public GetContainerDAO(final Connection con, final String user) {
        super(con);
        this.user = user;
    }

    /**
     * Executes the database query to retrieve the latest container associated with the user.
     *
     * <p>The result is stored in the {@code outputParam} field as a {@link Container} object.
     * If no container is found, {@code outputParam} will be {@code null}.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.GET_CONTAINER);
        LogContext.setResource("User USERNAME:" + user);

        PreparedStatement pstmnt = null;
        ResultSet rs = null;
        Container c = null;

        try {
            pstmnt = con.prepareStatement(STATEMENT);
            pstmnt.setString(1, user);
            rs = pstmnt.executeQuery();

            if (rs.next()) {
                c = new Container(
                        rs.getInt("instance_id"),
                        rs.getString("status"),
                        rs.getInt("image_run"),
                        rs.getInt("user_instance")
                );
                LOGGER.info("Container {} successfully read from DB.", c.getInstanceId());
            }
            else
                LOGGER.info("There are no containers for the user {}.", user);
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

