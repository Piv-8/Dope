package it.unipd.dei.webapp.dope.servlet;

import static it.unipd.dei.webapp.dope.resource.Error.*;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.rest.InsertUserRR;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.resource.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Servlet for handling user login and registration.
 *
 * <p>This servlet processes HTTP requests for login, user validation,
 * and new user registration. If the user is not authenticated, a login
 * form is presented. Upon successful login, a user session is created.
 * If the user does not exist, an error response is returned.
 * The servlet also handles user registration, providing a form to submit
 * necessary user details for account creation.</p>
 */
public class LoginServlet extends AbstractDatabaseServlet {

    /**
     * Handles GET requests to display the login and registration forms.
     *
     * <p>If the user is not authenticated, the login form is displayed. </p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException if an error occurs while processing the request.
     * @throws IOException if an error occurs while writing the response.
     */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String loginError = (String) req.getSession().getAttribute("loginError");
        if(loginError == null){
            req.getSession().removeAttribute("loginError");
        }
        req.setAttribute("loginError", loginError);
        LOGGER.info("loginError:" + loginError);
        // Forward to the JSP page
        req.getRequestDispatcher("/jsp/login.jsp").forward(req, res);
    }

    /**
     * Handles POST requests for user login and registration.
     *
     * <p>If the action is "login", the servlet validates the user's credentials.
     * If the action is "register", the servlet processes the registration
     * form to create a new user account.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException if an error occurs while processing the request.
     * @throws IOException if an error occurs while writing the response.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String action = req.getParameter("action");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String name = req.getParameter("name");
        String surname = req.getParameter("surname");
        String birthDate = req.getParameter("birthDate");

        //Set logs
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setAction(Actions.LOGIN);
        LogContext.setResource(req.getRequestURI());

        Message m = null;

        try (Connection con = getConnection()) { // Recupera la connessione dal DataSource
            if ("login".equalsIgnoreCase(action)) {
                try {
                    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                        m = new Message("Invalid username or password", WRONG_RESOURCE_PROVIDED, "Username and password cannot be empty.");
                    } else {
                        List<User> users = new SearchUserDAO(con, -1, username, null, null, null, null).access().getOutputParam();

                        if (users.isEmpty()) {
                            m = new Message("User not found.", AUTHENTICATION_ERROR, "No user found with the provided username.");
                        } else {
                            User user = users.get(0); // Prendiamo il primo risultato (username univoco)
                            if (BCrypt.checkpw(password, user.getPassword())) {
                                m = new Message("Login successful.");
                                LogContext.setUser(username);
                                HttpSession session = req.getSession();
                                session.setAttribute(ProtectedResourceFilter.USER_ATTRIBUTE, username);
                            } else {
                                m = new Message("Invalid password.", AUTHENTICATION_ERROR, "The password entered is incorrect.");
                            }
                        }
                    }
                } catch (SQLException ex) {
                    m = new Message("SQL error while searching user.", INTERNAL_SERVER_ERROR_SQL, ex.getMessage());
                } catch (Exception ex) {
                    m = new Message("Unexpected error occurred.", INTERNAL_SERVER_ERROR_TC, ex.getMessage());
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Exception: {}", e.getMessage());
            m = new Message("Database Error", INTERNAL_SERVER_ERROR_SQL, e.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch (Exception e) {
            LOGGER.error("Internal Server Error: {}", e.getMessage());
            m = new Message("Internal Server Error", INTERNAL_SERVER_ERROR_TC, e.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } finally{
            if(m != null && m.isError()){
                req.removeAttribute("username");
                req.removeAttribute("password");
                req.removeAttribute("action");
                req.getSession().setAttribute("loginError", "Login failed: username or password not correct");
                res.sendRedirect(req.getContextPath() + "/login");
            }else{
                res.sendRedirect(req.getContextPath() + "/");
            }
                LogContext.removeIPAddress();
                LogContext.removeUser();
                LogContext.removeAction();
                LogContext.removeResource();
        }
    }
}

