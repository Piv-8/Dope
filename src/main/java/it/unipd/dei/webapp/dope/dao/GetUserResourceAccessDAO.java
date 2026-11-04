package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object (DAO) for determining a user's access level to a resource
 * (repository or image) in the DOPE application.
 * <p>
 * This class provides two constructors to support access checks for both repositories and images.
 * The {@link #doAccess()} method returns an integer indicating the user's access level:
 * <ul>
 *   <li><b>0</b>: No access (the repository is private and the user is not the owner)</li>
 *   <li><b>1</b>: Public access (the repository is public, regardless of ownership)</li>
 *   <li><b>2</b>: Owner access (the user is the owner of the repository)</li>
 * </ul>
 */
public class GetUserResourceAccessDAO extends AbstractDAO<Integer> {

    private static final String STATEMENT_REPOSITORY = """
            SELECT "owner_id", "visible"
            FROM dope.repository 
            WHERE id_repo = ?;""";

    private static final String STATEMENT_IMAGE = """
            SELECT owner_id, "visible"
            FROM dope.repository 
            INNER JOIN dope.image ON repo = id_repo
            WHERE id_image = ?""";

    private final int userId;
    private final int resourceId;
    private final boolean isImage;

    /**
     * Creates a DAO to check access to a repository resource.
     *
     * @param con    the database connection
     * @param resourceID the resource to check
     * @param userId the user ID whose access is being checked
     * @param isImage if the resource we're accessing is an image
     */
    public GetUserResourceAccessDAO(final Connection con, int resourceID, final int userId, boolean isImage) {
        super(con);
        this.userId = userId;
        this.isImage = isImage;
        this.resourceId = resourceID;
    }


    /**
     * Executes the access check and sets {@code outputParam} to the user's access level.
     * <p>
     * The result is interpreted as follows:
     * <ul>
     *   <li><b>0</b>: The resource is private and the user is not the owner (no access)</li>
     *   <li><b>1</b>: The resource's repository is public (user has public access)</li>
     *   <li><b>2</b>: The user is the owner of the repository (owner access)</li>
     * </ul>
     *
     * @throws SQLException if a database error occurs during the access check
     */
    @Override
    public void doAccess() throws SQLException {
        LogContext.setAction(Actions.CHECK_USER);
        PreparedStatement pstmt = null;

        try {
            if (isImage) {
                pstmt = con.prepareStatement(STATEMENT_IMAGE);
                LogContext.setResource("Image ID:" + resourceId);
            } else {
                pstmt = con.prepareStatement(STATEMENT_REPOSITORY);
                LogContext.setResource("Repo ID:" + resourceId);
            }


            pstmt.setInt(1, resourceId);

            try (final ResultSet rs = pstmt.executeQuery()) {
                // Default to no access
                this.outputParam = 0;

                if (rs.next()) {
                    int ownerId = rs.getInt("owner_id");
                    String visibility = rs.getString("visible");

                    // Check access level
                    if (userId == ownerId) {
                        // User is owner
                        this.outputParam = 2;
                        LOGGER.info("User {} is owner of resource {}", userId, resourceId);
                    } else if (visibility.equals("public")) {
                        // Repository is public
                        this.outputParam = 1;
                        LOGGER.info("User {} has public access to resource {}", userId, resourceId);
                    } else {
                        // Repository is private and user is not owner
                        this.outputParam = 0;
                        LOGGER.info("User {} has no access to private resource {}", userId, resourceId);
                    }
                }
            }
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
        }

        LogContext.removeResource();
        LogContext.removeAction();
    }
}
