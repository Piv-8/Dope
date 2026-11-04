package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Deletes a specific {@link Image} from the database, identified by its image ID.
 *
 * <p>This DAO removes an {@link Image} from the Image table. The image is identified by its primary key,
 * {@code id_image}. If no image with the provided ID exists, no rows will be deleted and an informational
 * log message will be written.</p>
 *
 * <p>The operation always returns {@code true} as output, regardless of whether an image was actually deleted,
 * indicating the execution of the deletion attempt.</p>
 */
public class DeleteImageDAO extends AbstractDAO<Boolean> {

    /** The SQL statement to delete the image with the specified ID. */
    private static final String STATEMENT_IMAGE = """
                                   DELETE FROM dope."image" WHERE "id_image" = ?""";

    private static final String STATEMENT_CONTAINER = """
                                   DELETE FROM dope."container" WHERE "image_run" = ?""";

    private static final String STATEMENT_CONTAINERFILE = """
                                   DELETE FROM dope."containerfile" WHERE "image_built" = ?""";

    private static final String STATEMENT_LAYER = """
                                   DELETE FROM dope."layer" WHERE "image_owner" = ?""";

    /* TODO: Prendi tutti i container, containerfile e layer con foreign key questa immagine, e cancellali
                         Quindi, 3 delete con foreign key = parametro, prendi come esempio DeleteRepositoryDAO
                         esempio DELETE FROM dope.container WHERE "image_run" = ?
                 */

    /** The ID of the image to be deleted. */
    private final int id_image;

    /**
     * Creates a new {@code DeleteImageDAO} object to delete a specific image.
     *
     * @param con      the connection to be used for accessing the database.
     * @param id_image the ID of the image to be deleted.
     */
    public DeleteImageDAO(final Connection con, final int id_image){
        super(con);
        this.id_image = id_image;
    }

    /**
     * Executes the SQL DELETE statement to remove the image from the database.
     *
     * <p>Logs whether the image was found and successfully deleted or not.</p>
     *
     * @throws Exception if a database error occurs during deletion.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.DELETE_IMAGE);
        LogContext.setResource("Image ID:" + id_image);

        PreparedStatement pstmt = null;

        try {
            // TODO: Prima cancellare tutto il resto (nuovi preparedStatement, settare l'int dell'id_image e poi executeUpdate)

            pstmt = con.prepareStatement(STATEMENT_LAYER);
            pstmt.setInt(1, id_image);
            int rows_layer = pstmt.executeUpdate();

            pstmt = con.prepareStatement(STATEMENT_CONTAINER);
            pstmt.setInt(1, id_image);
            int rows_container = pstmt.executeUpdate();

            pstmt = con.prepareStatement(STATEMENT_CONTAINERFILE);
            pstmt.setInt(1, id_image);
            int rows_containerfile = pstmt.executeUpdate();

            pstmt = con.prepareStatement(STATEMENT_IMAGE);
            pstmt.setInt(1, id_image);
            int rows_image = pstmt.executeUpdate();

            if (rows_image == 0 && rows_container == 0 && rows_containerfile == 0 && rows_layer==0) {
                LOGGER.info("Image ID {} not found. No image deleted.",id_image);
            } else {
                LOGGER.info("Image ID {} successfully deleted.",id_image);
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

