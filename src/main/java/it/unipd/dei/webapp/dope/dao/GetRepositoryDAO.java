package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Retrieves a {@link Repository} from the database by its unique identifier.
 *
 * <p>This DAO performs a query on the {@code repository} table using the provided repository ID.
 * If a repository is found, it returns the corresponding {@link Repository} object;
 * otherwise, it returns {@code null}.</p>
 */
public class GetRepositoryDAO extends AbstractDAO<Repository> {

    /**
     * The SQL statement to retrieve a repository by its ID.
     */
    private static final String STATEMENT = """
        SELECT *
        FROM dope.repository
        WHERE "id_repo" = ?;""";

    /**
     * The ID of the repository to be retrieved.
     */
    private final Integer id_repo;

    /**
     * Creates a new {@code GetRepositoryDAO} instance.
     *
     * @param con      the connection to the database.
     * @param id_repo  the ID of the repository to retrieve.
     */
    public GetRepositoryDAO(final Connection con, final Integer id_repo) {
        super(con);
        this.id_repo = id_repo;
    }

    /**
     * Executes the query to retrieve the repository with the specified ID.
     *
     * <p>If a repository with the given ID is found, it is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.</p>
     *
     * @throws Exception if an error occurs while accessing the database.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.GET_REPOSITORY);
        LogContext.setResource("Repository ID: " + id_repo);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Repository repo = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, id_repo);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                repo = new Repository(
                        rs.getInt("id_repo"),
                        rs.getString("name"),
                        rs.getString("visible"),
                        rs.getInt("owner_id")
                );

                LOGGER.info("Repository ID {} successfully retrieved.", id_repo);
            } else {
                LOGGER.info("Repository ID {} not found.", id_repo);
            }
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }

        LogContext.removeAction();
        LogContext.removeResource();

        outputParam = repo;
    }
}
