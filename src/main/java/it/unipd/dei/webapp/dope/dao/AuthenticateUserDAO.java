package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;


/**
 * Authenticates a {@link User} by verifying their username and password against stored credentials in the database.
 *
 * <p>This DAO executes a query to retrieve the hashed password for the specified username from the database,
 * and compares it to the provided plain-text password using BCrypt hashing.</p>
 *
 * <p>If the credentials match, the {@code outputParam} will be set to {@code true}; otherwise, it will be set to {@code false}.</p>
 */
public final class AuthenticateUserDAO extends AbstractDAO<Boolean> {

    /** SQL query to retrieve the hashed password of the user with the specified username. */
    private static final String STATEMENT = """
            SELECT password FROM dope."user" WHERE "username" = ?
            """;

    /** The username provided for authentication. */
    private final String username;

    /** The plain-text password provided for authentication. */
    private final String password;

    /**
     * Creates a new {@code AuthenticateUserDAO} object for authenticating a user.
     *
     * @param con      the database connection to be used.
     * @param username the username of the user attempting authentication.
     * @param password the plain-text password of the user attempting authentication.
     *
     * @throws NullPointerException if {@code username} or {@code password} is {@code null} or blank.
     */
    public AuthenticateUserDAO(final Connection con, final String username, final String password) {
        super(con);

        if (username == null || username.isBlank()) {
            LOGGER.error("The username cannot be null or empty.");
            throw new NullPointerException("The username cannot be null or empty.");
        }
        this.username = username;

        if (password == null || password.isBlank()) {
            LOGGER.error("The password cannot be null or empty.");
            throw new NullPointerException("The password cannot be null or empty.");
        }
        this.password = password;
    }

    /**
     * Executes the authentication query, compares the password using BCrypt, and sets the result in {@code outputParam}.
     *
     * @throws SQLException if a database access error occurs.
     */
    @Override
    protected final void doAccess() throws SQLException {
        LogContext.setAction(Actions.AUTHENTICATE_USER);
        LogContext.setResource("User USERNAME: " + username);

        try (PreparedStatement pstmt = con.prepareStatement(STATEMENT)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");

                    // Verify password using BCrypt
                    if (BCrypt.checkpw(password, storedHash)) {
                        outputParam = true;
                        LOGGER.info("User {} successfully authenticated.", username);
                    } else {
                        outputParam = false;
                        LOGGER.warn("Authentication failed for user {}: Incorrect password.", username);
                    }
                } else {
                    outputParam = false;
                    LOGGER.warn("Authentication failed for user {}: User not found.", username);
                }
            }
        }
    }
}





