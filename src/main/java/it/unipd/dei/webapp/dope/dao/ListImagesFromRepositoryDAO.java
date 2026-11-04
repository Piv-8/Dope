package it.unipd.dei.webapp.dope.dao;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Repository;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves a list of {@link Image} objects associated with a specific {@link Repository} from the database.
 *
 * <p>This DAO performs a query on the {@code image} table to retrieve images associated with a given repository.
 * The images are identified by the repository's ID, and their details are returned as a list of {@link Image} objects.</p>
 */
public class ListImagesFromRepositoryDAO extends AbstractDAO<List<Image>> {

    /** The SQL query used to retrieve images associated with the given repository. */
    private static final String STATEMENT = "SELECT i.* FROM dope.\"image\" i JOIN dope.\"repository\" r ON i.\"repo\" = r.\"id_repo\" WHERE r.\"id_repo\" = ?";

    /** The ID of the repository whose images are to be retrieved. */
    private final int repository_id;

    /**
     * Creates a new {@code ListImagesFromRepositoryDAO} object.
     *
     * @param con           the connection to be used for accessing the database.
     * @param repository_id the ID of the repository whose images are to be retrieved.
     */
    public ListImagesFromRepositoryDAO(Connection con, final int repository_id) {
        super(con);
        this.repository_id = repository_id;
    }

    /**
     * Executes the query to retrieve the list of images associated with the given repository.
     *
     * <p>If the query is successful, a list of {@link Image} objects is populated and stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}. The details of the retrieved images are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.LIST_IMAGES_FROM_REPOSITORIES);
        LogContext.setResource("Images of Repository ID:" + repository_id);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Image img = null;
        List<Image> images = new ArrayList<>();

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, repository_id);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                img = new Image(
                        rs.getInt("id_image"),
                        rs.getString("tag"),
                        rs.getInt("repo"),
                        rs.getString("description"),
                        rs.getString("status")
                );
                images.add(img);
            }

            if(!images.isEmpty())
                LOGGER.info("List of Images successfully retrieved from the Repository. Retrieved {} Images", images.size());
            else
                LOGGER.info("There are no images for the repository {}.", repository_id);
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

        this.outputParam = images;
    }
}
