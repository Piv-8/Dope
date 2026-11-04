package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.*;
import it.unipd.dei.webapp.dope.resource.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Deletes a {@link Repository} and all associated {@link Image} from the database.
 *
 * This DAO performs a transactional deletion of:
 * <ul>
 *     <li>All {@link Image} associated with the repository (from the {@code image} table).</li>
 *     <li>All the {@link Layer} {@link Container} and {@link Containerfile} associated with the image (from the {@code layer}, {@code container} and {@code containerfile} tables).</li>
 *     <li>The {@link Repository} itself (from the {@code repository} table).</li>
 * </ul>
 *
 * If no repository is found with the specified ID, no rows will be deleted, and an informational message
 * will be logged. The operation is executed inside a transaction, which is committed only if both deletions
 * succeed. In case of failure, the transaction is rolled back.
 *
 * The result of the operation is always {@code true}, indicating the attempt was completed,
 * regardless of whether the repository existed or not.
 */
public class DeleteRepositoryDAO extends AbstractDAO<Boolean> {

    /** The SQL statement to delete images of a repository. */
    private static final String DELETE_IMAGES = "DELETE FROM dope.\"image\" WHERE \"repo\" = ?";

    /** The SQL statement to delete the repository itself. */
    private static final String DELETE_REPO = "DELETE FROM dope.\"repository\" WHERE \"id_repo\" = ?";

    /** The SQL statement to delete containers running off the image which got selected from the repo */
    private static final String DELETE_CONTAINERS = "DELETE FROM dope.container WHERE image_run IN (SELECT id_image FROM dope.image WHERE repo = ?)";

    /** The SQL statement to delete layers composing the image which got selected from the repo */
    private static final String DELETE_LAYERS = "DELETE FROM dope.layer WHERE image_owner IN (SELECT id_image FROM dope.image WHERE repo = ?)";

    /** The SQL statement to delete containerfiles that built an image selected from the repo */
    private static final String DELETE_CONTAINERFILES = "DELETE FROM dope.containerfile WHERE image_built IN (SELECT id_image FROM dope.image WHERE repo = ?)";

    private final Integer repoId;

    /**
     * Creates a new {@code DeleteRepositoryDAO} object.
     *
     * @param con     the connection to be used for accessing the database.
     * @param repoId  the ID of the repository to be deleted from the database.
     */
    public DeleteRepositoryDAO(final Connection con, final Integer repoId) {
        super(con);
        this.repoId = repoId;
    }

    /**
     * Executes the database deletion operation inside a transaction.
     *
     * <p>Deletes all images linked to the given repository and then deletes the repository itself.
     * Logs the result and ensures transactional consistency with commit/rollback.</p>
     *
     * @throws Exception if a database error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.DELETE_REPOSITORY);
        LogContext.setResource("Repository ID: " + repoId);

        PreparedStatement pstmtImages = null;
        PreparedStatement pstmtRepo = null;
        PreparedStatement pstmtContainers = null;
        PreparedStatement pstmtLayers = null;
        PreparedStatement pstmtContainerfiles = null;

        try {
            con.setAutoCommit(false);

            pstmtContainerfiles = con.prepareStatement(DELETE_CONTAINERFILES);
            pstmtContainerfiles.setInt(1, repoId);
            pstmtContainerfiles.executeUpdate();

            pstmtContainers = con.prepareStatement(DELETE_CONTAINERS);
            pstmtContainers.setInt(1, repoId);
            pstmtContainers.executeUpdate();

            pstmtLayers = con.prepareStatement(DELETE_LAYERS);
            pstmtLayers.setInt(1, repoId);
            pstmtLayers.executeUpdate();

            pstmtImages = con.prepareStatement(DELETE_IMAGES);
            pstmtImages.setInt(1, repoId);
            pstmtImages.executeUpdate();

            pstmtRepo = con.prepareStatement(DELETE_REPO);
            pstmtRepo.setInt(1, repoId);
            int affectedRows = pstmtRepo.executeUpdate();

            if (affectedRows == 0) {
                LOGGER.info("Repository {} not found. No deletion performed.", repoId);
            } else {
                LOGGER.info("Repository {} and associated images/containers successfully deleted.", repoId);
            }

            con.commit();
        } catch (SQLException e) {
            if (con != null) {
                con.rollback();
            }
            LOGGER.error("Failed to delete repository {}: {}", repoId, e.getMessage(), e);
            throw e;
        } finally {
            if (pstmtImages != null) pstmtImages.close();
            if (pstmtRepo != null) pstmtRepo.close();
            if (con != null) con.setAutoCommit(true);
        }

        LogContext.removeResource();
        LogContext.removeAction();

        this.outputParam = true;
    }
}
