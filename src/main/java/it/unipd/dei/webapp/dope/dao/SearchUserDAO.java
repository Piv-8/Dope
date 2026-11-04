package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * Searches for {@link User} objects in the database based on the provided search criteria.
 *
 * <p>This DAO class allows searching for users in the {@code user} table by various parameters. The search can be filtered by
 * user ID, username, password, name, surname, or birthdate. The resulting list of users is returned.</p>
 */
public final class SearchUserDAO extends AbstractDAO<List<User>> {

    /** The SQL query to be executed. */
    private static final String STATEMENT = "SELECT * FROM dope.\"user\" ";

    /** The search parameters. */
    private final int ID_User;
    private final String username;
    private final String password;
    private final String name;
    private final String surname;
    private final String birth_date;

    /**
     * Creates a new {@code SearchUserDAO} object for searching users in the database.
     *
     * @param con        the connection to the database.
     * @param ID_User    the ID of the user to search for.
     * @param username   the username of the user to search for.
     * @param password   the password of the user to search for.
     * @param name       the name of the user to search for.
     * @param surname    the surname of the user to search for.
     * @param birth_date the birthdate of the user to search for.
     */
    public SearchUserDAO(final Connection con, final int ID_User, final String username, final String password, final String name, final String surname, final String birth_date) {
        super(con);
        this.ID_User = ID_User;
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.birth_date = birth_date;
    }

    /**
     * Executes the query to search for users in the database based on the specified search criteria.
     *
     * <p>If successful, a list of {@link User} objects is populated and stored in the {@code outputParam} field.
     * Otherwise, {@code outputParam} remains {@code null}. The details of the retrieved users are logged.</p>
     *
     * @throws SQLException if a database access error occurs.
     */
    @Override
    public final void doAccess() throws SQLException {
        LogContext.setAction(Actions.SEARCH_USER);
        if(ID_User == -1)
            LogContext.setResource("User USERNAME: " + username);
        else
            LogContext.setResource("User ID:" + ID_User);

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // List to hold the search results
        final List<User> users = new ArrayList<>();
        final List<String> clauses = new ArrayList<>();

        // Add conditions to the query based on non-null search parameters
        if (ID_User > 0) {
            clauses.add(" id_user = ? ");
        }
        if (username != null) {
            clauses.add(" username = ? ");
        }
        if (name != null) {
            clauses.add(" name = ? ");
        }
        if (surname != null) {
            clauses.add(" surname = ? ");
        }
        if (birth_date != null) {
            clauses.add(" birth_date = ? ");
        }

        // Construct the final SQL query
        StringBuilder sb = new StringBuilder();
        sb.append(STATEMENT);
        sb.append(" ");
        if (!clauses.isEmpty()) {
            sb.append(" WHERE ").append(String.join(" AND ", clauses));
        }
        sb.append(";");

        try {
            pstmt = con.prepareStatement(sb.toString());

            // Set the values for each parameter in the prepared statement
            int clauseCount = 1;

            if (ID_User > 0) {
                pstmt.setInt(clauseCount, ID_User);
                clauseCount++;
            }
            if (username != null) {
                pstmt.setString(clauseCount, username);
                clauseCount++;
            }
            if (password != null) {
                pstmt.setString(clauseCount, password);
                clauseCount++;
            }
            if (name != null) {
                pstmt.setString(clauseCount, name);
                clauseCount++;
            }
            if (surname != null) {
                pstmt.setString(clauseCount, surname);
                clauseCount++;
            }
            if (birth_date != null) {
                pstmt.setString(clauseCount, birth_date);
                clauseCount++;
            }

            rs = pstmt.executeQuery();

            // Collect the results
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id_user"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getDate("birth_date").toString()));
            }

            if(!users.isEmpty())
                LOGGER.info("User(s) successfully listed.");
            else
                LOGGER.info("User(s) not found.");
            }finally {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            }

        LogContext.removeAction();
        LogContext.removeResource();

        this.outputParam = users;
    }
}




