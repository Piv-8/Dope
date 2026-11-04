package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Inserts a new {@link Image} into the database.
 *
 * <p>This DAO performs an insert operation on the {@code image} table.
 * It adds a new image with the provided tag, repository ID, description, and status.
 * The inserted image's details are returned after the insertion.</p>
 */
public class InsertImageDAO extends AbstractDAO<Image> {

    /** The SQL query used to insert a new image into the database. */
    private static final String STATEMENT = """
        INSERT INTO dope."image"("tag", "repo", "description", "status")
        VALUES (?, ?, ?, ?::dope.status_image)
        RETURNING *""";

    /** The {@link Image} object to be inserted into the database. */
    private final Image image;

    /**
     * Creates a new {@code InsertImageDAO} object.
     *
     * @param con   the connection to be used for accessing the database.
     * @param image the image object to insert.
     */
    public InsertImageDAO(final Connection con, final Image image) {
        super(con);
        this.image = image;
    }

    /**
     * Executes the insert query to add the new image to the database.
     *
     * <p>If the insertion is successful, the newly created image object is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.
     * The details of the newly created image are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.INSERT_IMAGE);
        LogContext.setResource("Image ID:" + image.getID_image());

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        Image i = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setString(1, image.getTag());
            pstmt.setInt(2, image.getRepo());
            pstmt.setString(3, image.getDescription());
            pstmt.setString(4, image.getStatus());

            rs = pstmt.executeQuery(); // Performing the insert operation

            if (rs.next()) {
                i = new Image(
                        rs.getInt("id_image"),
                        rs.getString("tag"),
                        rs.getInt("repo"),
                        rs.getString("description"),
                        rs.getString("status")
                );
                LOGGER.info("Image ID {} successfully inserted.", i.getID_image());
            } else {
                LOGGER.error("Failed to insert Image. No result returned.");
                throw new SQLException("Failed to insert the Image.");
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
        }

        LogContext.removeAction();
        LogContext.removeResource();

        outputParam = i;
    }
}

