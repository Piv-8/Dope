package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Checks whether an {@code Image} with a specific tag and repository ID exists in the database.
 *
 * <p>This DAO performs a query on the {@code image} table using the provided tag and repository ID.
 * It returns {@code true} if no such image exists (i.e., it is safe to insert a new one),
 * or {@code false} if an image with the same tag and repository already exists.</p>
 */
public class CheckImageByTagAndRepoDAO extends AbstractDAO<Boolean> {

    /** The SQL query used to check for an existing image with the same tag and repository. */
    private static final String STATEMENT = """
        SELECT 1 FROM dope."image"
        WHERE "tag" = ? AND "repo" = ?
        LIMIT 1""";

    /** The tag of the image to check. */
    private final String tag;

    /** The repository ID of the image to check. */
    private final Integer repo;

    /**
     * Creates a new {@code CheckImageByTagAndRepoDAO} object.
     *
     * @param con  the connection to be used for accessing the database.
     * @param tag  the tag to filter images.
     * @param repo the repository ID to filter images.
     */
    public CheckImageByTagAndRepoDAO(final Connection con, final String tag, final Integer repo) {
        super(con);
        this.tag = tag;
        this.repo = repo;
    }

    /**
     * Executes the query to check if an image with the specified tag and repository already exists.
     *
     * <p>Sets {@code outputParam} to {@code false} if such image exists, or {@code true} if not.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.CHECK_IMAGE);
        LogContext.setResource("Images with TAG: " + tag + " in REPOSITORY: " + repo);

        try (PreparedStatement pstmt = con.prepareStatement(STATEMENT)) {
            pstmt.setString(1, tag);
            pstmt.setInt(2, repo);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    outputParam = true;
                    LOGGER.info("An image with TAG '{}' already exists in REPOSITORY {}.", tag, repo);
                } else {
                    outputParam = false;
                    LOGGER.info("No image with TAG '{}' found in REPOSITORY {}.", tag, repo);
                }
            }
        } finally {
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}

