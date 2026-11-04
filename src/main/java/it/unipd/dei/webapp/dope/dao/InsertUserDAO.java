package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import it.unipd.dei.webapp.dope.resource.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static it.unipd.dei.webapp.dope.resource.Error.CONFLICT;

/**
 * Inserts a new {@link User} into the database.
 *
 * <p>This DAO performs an insert operation on the {@code user} table.
 * It adds a new user with the provided username, password (hashed), name, surname, and birth date.
 * Before inserting, it checks if the user already exists in the database. If the user already exists, an error message is returned.
 * The inserted user's details are returned after the insertion.</p>
 */
public class InsertUserDAO extends AbstractDAO<User> {

    /** The SQL query used to insert a new user into the database. */
    private static final String STATEMENT = """
        INSERT INTO dope."user"("username", "password", "name", "surname", "birth_date")
        VALUES (?, ?, ?, ?, ?)
        RETURNING *""";

    /** The {@link User} object to be inserted into the database. */
    private final User user;

    private ResultSet rs = null;
    private User u = null;

    /**
     * Creates a new {@code InsertUserDAO} object.
     *
     * @param con  the connection to be used for accessing the database.
     * @param user the user to insert into the database.
     * @throws NullPointerException if the user is {@code null}.
     */
    public InsertUserDAO(final Connection con, final User user) {
        super(con);
        if (user == null) {
            LOGGER.error("The user cannot be null.");
            throw new NullPointerException("The user cannot be null.");
        }
        this.user = user;
    }

    /**
     * Executes the insert query to add the new user to the database.
     *
     * <p>If the user is already registered, an error message is returned with a {@code CONFLICT} status code.
     * If the insertion is successful, the newly created user object is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.
     * The details of the newly created user are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.INSERT_USER);
        LogContext.setResource("User USERNAME:" + user.getUsername());

        PreparedStatement stmnt = null;
        Message m = null;

        try {
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            stmnt = con.prepareStatement(STATEMENT);
            stmnt.setString(1, user.getUsername());
            stmnt.setString(2, hashedPassword);
            stmnt.setString(3, user.getName());
            stmnt.setString(4, user.getSurname());
            stmnt.setDate(5, java.sql.Date.valueOf(user.getBirthDate()));
            LOGGER.debug("Inserting user: username={}, name={}, surname={}, birth_date={}", user.getUsername(), user.getName(), user.getSurname(), user.getBirthDate());

            // check if the user is already registered
            List<User> usersList = new SearchUserDAO(con, -1, user.getUsername(), null, null, null, null).access().getOutputParam();
            if (usersList.size() != 0) {
                // the user is already registered
                m = new Message("This user already exists " + usersList.get(0).toString(), CONFLICT, "User already existing");
                LOGGER.error("problems with fields:  {}.", m.getMessage());
                u = new User(-1, null, null, null, null, null);
            } else {
                rs = stmnt.executeQuery();
                if (rs.next()) {
                    u = new User(
                            rs.getInt("id_user"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("birth_date")
                    );
                    LOGGER.info("User ID {} successfully inserted.", u.getID_User());
                }
                else {
                    LOGGER.error("Failed to insert User. No result returned.");
                    throw new SQLException("Failed to insert the User.");
                }
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

        outputParam = u;
    }
}

