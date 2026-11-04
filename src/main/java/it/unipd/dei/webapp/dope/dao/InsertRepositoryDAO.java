package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Inserts a new {@link Repository} into the database.
 *
 * <p>This DAO performs an insert operation on the {@code repository} table.
 * It adds a new repository with the provided name, visibility, and owner ID.
 * The inserted repository's details are returned after the insertion.</p>
 */
public class InsertRepositoryDAO extends AbstractDAO<Repository> {

    private static final String DUPLICATE_CHECK = """
            SELECT * FROM dope.repository
            WHERE owner_id = ?
            """;

    /** The SQL query used to insert a new repository into the database. */
    private static final String STATEMENT = """
        INSERT INTO dope."repository"("name", "visible", "owner_id")
        VALUES (?, ?::dope.visibility, ?)
        RETURNING *""";

    /** The {@link Repository} object to be inserted into the database. */
    private final Repository repository;

    private ResultSet rs = null;
    private Repository r = null;

    /**
     * Creates a new {@code InsertRepositoryDAO} object.
     *
     * @param con        the connection to be used for accessing the database.
     * @param repository the repository to insert.
     * @throws NullPointerException if the repository is {@code null}.
     */
    public InsertRepositoryDAO(final Connection con, final Repository repository) {
        super(con);
        if (repository == null) {
            LOGGER.error("The repository cannot be null.");
            throw new NullPointerException("The repository cannot be null.");
        }
        this.repository = repository;
    }

    /**
     * Executes the insert query to add the new repository to the database.
     *
     * <p>If the insertion is successful, the newly created repository object is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.
     * The details of the newly created repository are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     * @throws SQLException if there's a duplicate repository
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.INSERT_REPOSITORY);
        LogContext.setResource("Repository ID:" + repository.getID_repo());


        PreparedStatement stmnt = null;
        PreparedStatement dup_stmnt = null;

        try {
            dup_stmnt = con.prepareStatement(DUPLICATE_CHECK);
            dup_stmnt.setInt(1, repository.getOwner_ID());
            rs = dup_stmnt.executeQuery();
            while(rs.next()){
                if(rs.getString("name").equals(repository.getName())){
                    LOGGER.error("Error: duplicate repository with the same name and same user");
                    throw new SQLException("Duplicate repository name for the same owner", "42701");
                }
            }

            stmnt = con.prepareStatement(STATEMENT);
            stmnt.setString(1, repository.getName());
            stmnt.setString(2, repository.getVisibility());
            stmnt.setInt(3, repository.getOwner_ID());

            rs = stmnt.executeQuery(); // Performing the insert operation

            if (rs.next()) {
                r = new Repository(
                        rs.getInt("id_repo"),
                        rs.getString("name"),
                        rs.getString("visible"),
                        rs.getInt("owner_id")
                );
                LOGGER.info("Repository ID {} successfully inserted.", r.getID_repo());
            }
            else
            {
                LOGGER.error("Failed to insert Repository. No result returned.");
                throw new SQLException("Failed to insert the Repository.");
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmnt != null) {
                stmnt.close();
            }
        }

        LogContext.removeAction();
        LogContext.removeResource();

        outputParam = r;
    }
}


