package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Inserts a new {@link Container} into the database.
 *
 * <p>This DAO performs an insert operation on the {@code container} table.
 * It adds a new container with the provided status, image run ID, and user instance.
 * The inserted container's details are returned after the insertion.</p>
 */
public class InsertContainerDAO extends AbstractDAO<Container> {

    /** The SQL query used to insert a new container into the database. */
    private static final String STATEMENT = """
        INSERT INTO dope."container"("status", "image_run", "user_instance")
        VALUES (?::dope.status_container, ?, ?)
        RETURNING *""";

    /** The {@link Container} object to be inserted into the database. */
    private final Container container;

    /**
     * Creates a new {@code InsertContainerDAO} object.
     *
     * @param con       the connection to be used for accessing the database.
     * @param container the container object to insert.
     */
    public InsertContainerDAO(final Connection con, final Container container) {
        super(con);
        this.container = container;
    }

    /**
     * Executes the insert query to add the new container to the database.
     *
     * <p>If the insertion is successful, the newly created container object stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.
     * The details of the newly created container are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.INSERT_CONTAINER);
        LogContext.setResource("Container ID:" + container.getInstanceId());

        PreparedStatement pstmnt = null;
        ResultSet rs = null;

        Container c = null;

        try {
            pstmnt = con.prepareStatement(STATEMENT);
            pstmnt.setString(1, container.getStatus());
            pstmnt.setInt(2, container.getImageRun());
            pstmnt.setInt(3, container.getUserInstance());

            rs = pstmnt.executeQuery();

            if (rs.next()) {
                c = new Container(
                        rs.getInt("instance_id"),
                        rs.getString("status"),
                        rs.getInt("image_run"),
                        rs.getInt("user_instance")
                );
                LOGGER.info("Container {} successfully stored in the database.", c.getInstanceId());
            }
            else {
                LOGGER.error("Failed to insert Container. No result returned.");
                throw new SQLException("Failed to insert the Container.");
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

