package it.unipd.dei.webapp.dope.servlet;

import it.unipd.dei.webapp.dope.dao.DeleteContainerFromUserDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.Message;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Actions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;


/**
 * Servlet to log out the user and redirect it to the login/signup page. Furthermore, every container of that user is deleted.
 */
public class LogoutServlet extends AbstractDatabaseServlet{
    /**
     * Handles the HTTP GET request logging out the current user.
     * <p>
     * When a user logs out, its containers are deleted from the database with {@link DeleteContainerFromUserDAO} and using
     * the Podman API with {@link PodmanClientContainer#killContainer()}.
     * <p>
     * It redirects the user to the login/signup page.
     *
     * @param req the HTTP request from the client.
     * @param res the HTTP response from the server.
     * @throws IOException if any error occurs in the client/server communication.
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        LogContext.setIPAddress(req.getRemoteAddr());
        String user = null;
        Integer userId = null;
        Message m  = null;
        //logout invalidating the session
        try {
            HttpSession session = req.getSession(false);
            if(session != null){
                user = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
                if(user!=null){ //checks is the user is logged in
                    LogContext.setUser(user);
                    // searching for the id of the logged-in user
                    userId = new SearchUserDAO(getConnection(), -1, user, null, null, null, null).access().getOutputParam().get(0).getID_User();
                    // when a user logs out, its container are deleted
                    List<Container> containers = new DeleteContainerFromUserDAO(getConnection(), userId).access().getOutputParam();
                    for(Container c : containers){
                        LOGGER.info("Killing container: {}.", c.getInstanceId());
                        PodmanClientContainer pcc = new PodmanClientContainer(c);
                        pcc.killContainer();
                    }
                    LogContext.setAction(Actions.LOGOUT);
                    LogContext.setResource(req.getRequestURI());
                    LOGGER.info("User {} logged out.",user);
                    session.setAttribute("logout", true); // indicates a "manual" logout
                    session.invalidate(); //invalidates the session
                    LOGGER.info("Session successfully invalidated.");
                }
                else LOGGER.info("Logout attempt failed: the session was not authenticated."); //if the user was not logged in
            }
        } catch (IllegalStateException ex) {
            LOGGER.error("Invalid session: no user to log out.", ex);
        } catch (SQLException e) {
            LOGGER.error("SQL Exception: {}", e.getMessage());
            m = new Message("Database Error", "E500", e.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch (Exception e) {
        LOGGER.error("Unable to acquire the connection pool to the database through podman client.", e);
    }

        try {
            res.sendRedirect(req.getContextPath() + "/"); //redirection to the login/signup page
        } catch(IOException ex) {
            LOGGER.error("Unable to perform redirection after logout and session invalidation.");
            throw ex;
        } finally {
            LogContext.removeIPAddress();
            LogContext.removeAction();
            LogContext.removeUser();
            LogContext.removeResource();
        }
    }
}
