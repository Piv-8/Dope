package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Updates an {@link Image} in the database.
 *
 * <p>This DAO class performs an update operation on an existing {@link Image} by modifying its attributes such as the status, tag, repository,
 * and description based on the provided {@link Image} object. The updated {@link Image} object is returned as the result.</p>
 */
public class UpdateImageDAO extends AbstractDAO<Image> {

    /** The SQL query to update an image in the database. */
    private static final String STATEMENT = "UPDATE dope.\"image\" SET \"status\" = ?::dope.status_image, \"tag\" = ?, \"repo\" = ?, \"description\" = ? WHERE \"id_image\" = ? RETURNING *";

    /** The {@link Image} object that contains the data to be updated in the database */
    private final Image image;

    /**
     * Creates a new {@code UpdateImageDAO} object for updating an image in the database.
     *
     * @param con the connection to be used for accessing the database.
     * @param image the {@link Image} object containing the updated data.
     */
    public UpdateImageDAO(final Connection con, final Image image){
        super(con);
        this.image = image;
    }

    /**
     * Executes the update operation on the {@link Image}.
     *
     * <p>This method updates the attributes of the specified {@link Image} in the database, including the status, tag, repository, and description.
     * If the update is successful, the updated image is returned in the {@code outputParam} field. Otherwise, an appropriate message is logged.</p>
     *
     * @throws Exception if a database access error occurs during the update.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.UPDATE_IMAGE);
        LogContext.setResource("Image ID:" + image.getID_image());

        PreparedStatement pstmnt = null;
        ResultSet rs = null;

        Image i = null;

        try {
            // Prepare and execute the update query
            pstmnt = con.prepareStatement(STATEMENT);
            pstmnt.setString(1, image.getStatus());
            pstmnt.setString(2, image.getTag());
            pstmnt.setInt(3, image.getRepo());
            pstmnt.setString(4, image.getDescription());
            pstmnt.setInt(5, image.getID_image());

            rs = pstmnt.executeQuery();

            if (rs.next()) {
                i = new Image(
                        rs.getInt("id_image"),
                        rs.getString("tag"),
                        rs.getInt("repo"),
                        rs.getString("description"),
                        rs.getString("status")
                );
                LOGGER.info("Image {} successfully updated in the database.", image.getID_image());
            }
            else
            {
                LOGGER.error("Failed to Update Image {}. No Image found.", image.getID_image());
                throw new SQLException("Failed to Update the Image.");
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

        outputParam = i;
    }
}
