package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Containerfile;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Inserts a new {@link Containerfile} into the database.
 *
 * <p>This DAO performs an insert operation on the {@code containerfile} table.
 * It adds a new container file with the provided name, base image, command, JSON object for Optional command, and id of the image built.
 * The inserted container file's details are returned after the insertion.</p>
 */
public class InsertContainerfileDAO extends AbstractDAO<Containerfile> {

    /** The SQL query to insert a new container file into the database. */
    private static final String STATEMENT = """
        INSERT INTO dope."containerfile"("name", "from", "cmd", "json_obj", "image_built") 
        VALUES (?, ?, ?, ?::json, ?) 
        RETURNING *;
        """;

    /** The {@link Containerfile} object to be inserted into the database. */
    final Containerfile cf;

    /**
     * Creates a new {@code InsertContainerfileDAO} object.
     *
     * @param con the connection to be used for accessing the database.
     * @param cf  the container file to be inserted into the database.
     */
    public InsertContainerfileDAO(Connection con, final Containerfile cf) {
        super(con);
        this.cf = cf;
    }

    /**
     * Executes the SQL query to insert a new container file into the database.
     *
     * <p>If the insertion is successful, the newly created Containerfile object stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.
     * The details of the newly created container are logged.</p>
     *
     * @throws SQLException if a database access error occurs or the insertion fails.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.INSERT_CONTAINERFILE);
        LogContext.setResource("Containerfile NAME:" + cf.getName());

        PreparedStatement pstmt;
        ResultSet rs;

        Containerfile containerfile = null;
        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setString(1, cf.getName());
            pstmt.setString(2, cf.getFrom());
            pstmt.setString(3, cf.getCmd());
            pstmt.setString(4, cf.jsonObjToString());
            pstmt.setInt(5, cf.getImage_Built());

            rs = pstmt.executeQuery();

            if (rs.next()) {
                containerfile = new Containerfile(
                        rs.getString("name"),
                        rs.getString("from"),
                        rs.getString("cmd"),
                        rs.getString("json_obj"),
                        rs.getInt("image_built")
                );
                LOGGER.info("Containerfile {} successfully stored in the database", containerfile.getName());
            } else {
                LOGGER.error("Failed to insert Containerfile. No result returned.");
                throw new SQLException("Failed to insert the Containerfile.");
            }

        } catch (SQLException ex) {
            LOGGER.error("Error while inserting the Containerfile", ex);
            throw ex;
        }

        LogContext.removeAction();
        LogContext.removeResource();

        outputParam = cf;
    }
}

