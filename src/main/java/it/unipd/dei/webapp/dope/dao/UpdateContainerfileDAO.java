package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Containerfile;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Updates a {@link Containerfile} in the database.
 *
 * <p>This DAO class performs an update operation on an existing {@link Containerfile} by setting the built image's version number
 * based on the provided {@link Containerfile} object. It updates the "image_built" field for the specified container file's name.
 * The updated {@link Containerfile} object is returned as the result.</p>
 */
public class UpdateContainerfileDAO extends AbstractDAO<Containerfile> {

    /** The SQL statement to update the "image_built" field of a container file. */
    private static final String STATEMENT = """
                                            UPDATE dope."containerfile" SET "image_built" = ? WHERE "name" = ? RETURNING *
                                            """;
    /** The {@link Containerfile} object that contains the data to be updated in the database. */
    private final Containerfile cf;

    /**
     * Creates a new {@code UpdateContainerfileDAO} object for updating a containerfile in the database.
     *
     * @param con the connection to be used for accessing the database.
     * @param cf the {@link Containerfile} object containing the updated data.
     */
    public UpdateContainerfileDAO(final Connection con, final Containerfile cf){
        super(con);
        this.cf = cf;
    }

    /**
     * Executes the update operation on the {@link Containerfile}.
     *
     * <p>This method updates the {@link Containerfile} in the database by modifying the "image_built" field for the container file
     * with the specified name. If the update is successful, the updated container file is returned in the {@code outputParam} field.
     * Otherwise, an appropriate message is logged.</p>
     *
     * @throws Exception if a database access error occurs during the update.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.UPDATE_CONTAINERFILE);
        LogContext.setResource("Containerfile NAME:" + cf.getName());

        PreparedStatement pstmnt = null;
        ResultSet rs = null;

        Containerfile cf_ret = null;

        try {
            // Prepare and execute the update query
            pstmnt = con.prepareStatement(STATEMENT);
            pstmnt.setInt(1, cf.getImage_Built());
            pstmnt.setString(2, cf.getName());

            rs = pstmnt.executeQuery();

            if (rs.next()) {
                cf_ret = new Containerfile(
                        rs.getString("name"),
                        rs.getString("from"),
                        rs.getString("cmd"),
                        rs.getString("json_obj"),
                        rs.getInt("image_built")
                );
                LOGGER.info("Containerfile {} successfully stored in the database", cf.getName());
            }
            else
            {
                LOGGER.error("Failed to Update Containerfile {}. No Containerfile found.", cf.getName());
                throw new SQLException("Failed to Update the Containerfile.");
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

        outputParam = cf_ret;
    }
}
