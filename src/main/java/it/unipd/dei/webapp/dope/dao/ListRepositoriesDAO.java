package it.unipd.dei.webapp.dope.dao;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Retrieves a list of {@link Repository} objects for a specific user from the database.
 *
 * <p>This DAO performs a query on the {@code repository} table to retrieve public repositories and
 * private repositories that belong to the specified user. The result is returned as a list of
 * {@link Repository} objects.</p>
 */
public class ListRepositoriesDAO extends AbstractDAO<List<Repository>> {

    /** The SQL query used to retrieve public repositories and private repositories of the specified user. */
    private static final String STATEMENT = """
                                            SELECT * FROM dope."repository" 
                                            WHERE "visible" = 'public' OR "owner_id" = ?
                                            ORDER BY "name" ASC, "id_repo" ASC
                                            """;

    /** The ID of the user whose repositories are to be retrieved. */
    private final Integer userId;

    /**
     * Creates a new {@code ListRepositoriesDAO} object.
     *
     * @param con     the connection to be used for accessing the database.
     * @param userId  the ID of the user whose repositories are to be retrieved.
     */
    public ListRepositoriesDAO(final Connection con, final Integer userId) {
        super(con);
        this.userId = userId;
    }

    /**
     * Executes the query to retrieve the list of public and private repositories associated with the given user.
     *
     * <p>If the query is successful, a list of {@link Repository} objects is populated and stored in the
     * {@code outputParam} field; otherwise, {@code outputParam} remains {@code null}. The details of the
     * retrieved repositories are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected final void doAccess() throws Exception {
        LogContext.setAction(Actions.LIST_REPOSITORIES);
        LogContext.setResource("Repository of User ID:" + userId);

        List<Repository> repositoryList = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, userId);  // Set user ID for filtering private repos

            // Execute the query and add repositories to the set (to remove duplicates)
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Repository repo = new Repository(
                        rs.getInt("id_repo"),
                        rs.getString("name"),
                        rs.getString("visible"),
                        rs.getInt("owner_id")
                );
                repositoryList.add(repo);
            }

            if(!repositoryList.isEmpty())
                LOGGER.info("Repositories (public and private for user {}) successfully retrieved.", userId);
            else
                LOGGER.info("Repositories (public and private for user {}) not found.", userId);
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

        this.outputParam = repositoryList;
    }
}
