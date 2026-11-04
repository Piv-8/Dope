package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Retrieves an {@link Image} from the database by its unique identifier.
 *
 * <p>This DAO performs a query on the {@code image} table using the provided image ID.
 * If an image is found, it returns the corresponding {@link Image} object; otherwise, it returns {@code null}.</p>
 */
public class GetImageDAO extends AbstractDAO<Image> {

    /** The SQL query used to retrieve an image by its ID. */
    private static final String STATEMENT = """
        SELECT "id_image", "tag", "repo", "description", "status"
        FROM dope."image"
        WHERE "id_image" = ?""";

    /** The ID of the image to be retrieved. */
    private final Integer id_image;

    /**
     * Creates a new {@code GetImageDAO} object.
     *
     * @param con       the connection to be used for accessing the database.
     * @param id_image  the ID of the image to retrieve.
     */
    public GetImageDAO(final Connection con, final Integer id_image) {
        super(con);
        this.id_image = id_image;
    }

    /**
     * Executes the query to retrieve the image with the specified ID.
     *
     * <p>If an image with the given ID is found, it is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.GET_IMAGE);
        LogContext.setResource("Image " + id_image);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Image im = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, id_image);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                im = new Image(
                        rs.getInt("id_image"),
                        rs.getString("tag"),
                        rs.getInt("repo"),
                        rs.getString("description"),
                        rs.getString("status")
                );

                LOGGER.info("Image ID {} successfully retrieved.", id_image);
            }
            else
                LOGGER.info("Image ID {} not found.", id_image);
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

        outputParam = im;
    }
}
