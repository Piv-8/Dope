package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.*;
import static it.unipd.dei.webapp.dope.resource.Error.*;
import it.unipd.dei.webapp.dope.dao.InsertUserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * This class represents the REST resource responsible for handling requests to insert a new user into the system.
 * It extends {@link AbstractRR} and implements the logic to process and persist a new {@link User}
 * based on the data received in the HTTP request.
 *
 * <p>The request must contain a JSON representation of a {@link User} object, including the username, password,
 * and birthdate fields. These fields are validated using regular expressions before attempting to store the user.
 * Upon successful validation and insertion, the server responds with the created object and an HTTP 201 status code.
 * If any error occurs during processing, such as invalid input or database failure, an appropriate error message
 * and status code are returned.</p>
 */
public class InsertUserRR extends AbstractRR {

    // Regular expressions to validate the fields
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-]{3,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^(19\\d{2}|20\\d{2}|2100)-\\d{2}-\\d{2}$");
    private static final Pattern NAME_PATTERN = Pattern.compile(".*[A-Za-z].*");

    /**
     * Constructs a new {@code InsertUserRR} instance.
     *
     * @param req the {@link HttpServletRequest} object representing the client request.
     * @param res the {@link HttpServletResponse} object for sending the response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public InsertUserRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.INSERT_USER, req, res);
    }

    /**
     * Handles the HTTP request to insert a new {@link User} into the database.
     * This method performs the following steps:
     * <ul>
     *     <li>Logs contextual information such as IP address and user session.</li>
     *     <li>Parses the request body into a {@link User} object using its {@code fromJSON()} method.</li>
     *     <li>Validates the user fields: username, password, and birthdate using regular expressions.</li>
     *     <li>Persists the user using {@link InsertUserDAO} if validation succeeds.</li>
     *     <li>Sends back the created user and HTTP 201 status if successful, or a relevant error response otherwise.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Malformed or missing JSON input (400 Bad Request)</li>
     *     <li>Invalid input fields (custom error messages with 400-level status)</li>
     *     <li>Database errors or unexpected failures (500 Internal Server Error)</li>
     * </ul>
     *
     * @throws IOException if an I/O error occurs while reading the request or writing the response.
     */
    @Override
    public void doServe() throws IOException {

        LogContext.setIPAddress(req.getRemoteAddr());
        String us = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setUser(us);
        User u = null;
        Message m = null;

        try {
            // Parse the incoming JSON into a User object
            final User user = User.fromJSON(req.getInputStream());

            if (user != null) {
                String username = user.getUsername();
                String password = user.getPassword();
                String birth_date = user.getBirthDate();
                String name = user.getName();
                String surname = user.getSurname();

                LogContext.setAction(Actions.INSERT_USER);
                LogContext.setResource(String.valueOf(user.getID_User()));

                // Validate user fields
                if (username == null || username.isEmpty() || !USERNAME_PATTERN.matcher(username).matches()) {
                    m = new Message("Invalid username", WRONG_RESOURCE_PROVIDED, "Username must be at least 3 characters and contain only letters, numbers, '.', '_', or '-'");
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    m.toJSON(res.getOutputStream());
                } else if (password == null || password.isEmpty() || !PASSWORD_PATTERN.matcher(password).matches()) {
                    m = new Message("Invalid password", WRONG_RESOURCE_PROVIDED, "Password must be at least 8 characters long and contain at least one letter and one number.");
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    m.toJSON(res.getOutputStream());
                } else if (birth_date == null || birth_date.isEmpty() || !DATE_PATTERN.matcher(birth_date).matches()) {
                    m = new Message("Invalid birth date", WRONG_RESOURCE_PROVIDED, "Birth date must be in format YYYY-MM-DD and the year must be between 1900 and 2100");
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    m.toJSON(res.getOutputStream());
                } else if (name == null || name.isEmpty() || !NAME_PATTERN.matcher(name).matches()) {
                    m = new Message("Invalid name", WRONG_RESOURCE_PROVIDED, "Name must contain at least one letter");
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    m.toJSON(res.getOutputStream());
                } else if (surname == null || surname.isEmpty() || !NAME_PATTERN.matcher(surname).matches()) {
                    m = new Message("Invalid surname", WRONG_RESOURCE_PROVIDED, "Surname must contain at least one letter");
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    m.toJSON(res.getOutputStream());
                } else {
                    // Create and insert the user into the database
                    u = new InsertUserDAO(getConnection(), user).access().getOutputParam();
                    LogContext.setUser(username);

                    if (u != null && u.getID_User() != -1) {
                        LOGGER.info("User successfully created");
                        res.setStatus(HttpServletResponse.SC_CREATED);
                        u.toJSON(res.getOutputStream());
                    } else if (u.getID_User() == -1) {
                        LOGGER.error("Attempted to create an existing user");
                        res.setStatus(HttpServletResponse.SC_CONFLICT);
                        m = new Message("This user already exists", CONFLICT, "User already existing");
                        m.toJSON(res.getOutputStream());
                    } else {
                        LOGGER.error("Error while creating the user");
                        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        m = new Message("Error while creating the user", ERROR_PROCESSING_RESOURCE, "Error while creating the user");
                        m.toJSON(res.getOutputStream());
                    }
                }
            } else {
                throw new NullPointerException("Unable to parse JSON: no User object found");
            }
        } catch (EOFException ex) {
            LOGGER.warn("Cannot create the user: No valid JSON found in the request", ex);
            m = new Message("Cannot create the user: No valid JSON found in the request", WRONG_RESOURCE_PROVIDED, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            LOGGER.error("Can't create the User, unexpected SQL error", ex);
            m = new Message("Can't create the User, unexpected SQL error", INTERNAL_SERVER_ERROR_SQL, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.setContentType("application/json");
            res.setCharacterEncoding("utf-8");
            m.toJSON(res.getOutputStream());
        } finally {
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}

