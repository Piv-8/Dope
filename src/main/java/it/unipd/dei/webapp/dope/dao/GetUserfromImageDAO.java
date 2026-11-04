package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.User;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Retrieves a {@link User} associated with an {@link Image} from the database.
 *
 * <p>This DAO retrieves the user who owns the repository related to a specific image.
 * It joins the {@code image}, {@code repository}, and {@code user} tables to fetch the user details.</p>
 */
public class GetUserfromImageDAO extends AbstractDAO<User> {

    /** The SQL query to retrieve the user associated with the given image ID. */
    private static final String STATEMENT = """
        SELECT u.* 
        FROM dope."image" i 
        JOIN dope."repository" r ON i.repo = r.id_repo 
        JOIN dope."user" u ON r.owner_id = u.id_user 
        WHERE i.id_image = ?;
        """;

    /** The ID of the image used to retrieve the associated user. */
    private final Integer id_image;

    /**
     * Creates a new {@code GetUserfromImageDAO} object.
     *
     * @param con      the connection to be used for accessing the database.
     * @param id_image the ID of the image whose associated user is to be retrieved.
     */
    public GetUserfromImageDAO(final Connection con, final Integer id_image) {
        super(con);
        this.id_image = id_image;
    }

    /**
     * Executes the query to retrieve the user associated with the given image ID.
     *
     * <p>If a user is found, their details are returned and logged. If no user is found, the result will be {@code null}.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        LogContext.setAction(Actions.GET_USER_FROM_IMAGE);
        LogContext.setResource("User of Image: " + id_image);

        User usr = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, id_image);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                usr = new User(
                        rs.getInt("id_user"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("birth_date")
                );
                LOGGER.info("User ID {} successfully retrieved.", usr.getID_User());
            }
            else
                LOGGER.info("No User found for the Image {}.", id_image);
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

        outputParam = usr;
    }
}
