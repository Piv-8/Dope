package it.unipd.dei.webapp.dope.listener;

import it.unipd.dei.webapp.dope.dao.DeleteContainerFromUserDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import it.unipd.dei.webapp.dope.servlet.LogoutServlet;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static it.unipd.dei.webapp.dope.resource.Error.*;

/**
 * SessionExpiredListener listens for HTTP session expiration events.
 * <p>
 * When a session is destroyed, it checks if the session was ended by user logout or by timeout.
 * If a user session expires due to timeout, it cleans up resources by removing all containers associated with the user.
 * </p>
 */
@WebListener
public class SessionExpiredListener implements HttpSessionListener {

    /** Logger instance for this listener. */
    protected static final Logger LOGGER = LogManager.getLogger(SessionExpiredListener.class);

    /**
     * Handles session destruction events. Cleans up containers if the user session expired due to timeout.
     *
     * @param sessionEvent the session event containing the session to clean up
     */
    /**
     *Receives notifications that a session is about to be invalidated,due to a user logout or a timeout.
     *<p>If the session is being invalidated by the {@link jakarta.servlet.http.HttpSession#invalidate()} method called
     *in {@link LogoutServlet}, just logging operations are performed. The containers of the logged-in user are deleted in
     * {@link LogoutServlet}.
     *<p> If the session is being invalidated by a timeout, each container associated to the logged-in user is deleted from the database
     *and using the PodmanAPI through {@link PodmanClientContainer#killContainer()}.
     *@param sessionEvent the {@link HttpSessionEvent} containing the session that is being invalidated.
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        LogContext.setResource("/session-expired");
        HttpSession session = sessionEvent.getSession();
        String user = (String) session.getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        Integer userId = null;
        Message m = null;
        Boolean logout = (Boolean) session.getAttribute("logout");
        if (logout != null && logout) { //checks if the user manually logged out
            LogContext.setAction(Actions.SESSION_EXPIRED);
            LOGGER.info("User {} logged out manually.", user);
            session.removeAttribute("logout");
        } else { //session expired due to timeout
            if(user!=null){ //checks is the user is logged in
                try {
                    InitialContext cxt = new InitialContext();
                    DataSource ds = (DataSource) cxt.lookup("java:/comp/env/jdbc/dope");
                    try (Connection con = ds.getConnection()) {
                        LogContext.setUser(user);
                        userId = new SearchUserDAO(con, -1, user, null, null, null, null).access().getOutputParam().get(0).getID_User();
                        // when a user logs out, its container are deleted
                        DeleteContainerFromUserDAO deleteContainerDAO = new DeleteContainerFromUserDAO(ds.getConnection(), userId);
                        List<Container> containers = deleteContainerDAO.access().getOutputParam();
                        for(Container c : containers){
                            LOGGER.info("Killing container: {}.",c.getInstanceId());
                            PodmanClientContainer pcc = new PodmanClientContainer(c);
                            pcc.killContainer();
                        }
                        LogContext.setAction(Actions.SESSION_TIMEOUT);
                        LogContext.setResource("/session-expired");
                        LOGGER.info("User {} logged out.",user);
                        LOGGER.info("Session successfully invalidated.");
                    } catch (SQLException e) {
                        LOGGER.error("SQL Exception: {}", e.getMessage());
                        m = new Message("Database Error", INTERNAL_SERVER_ERROR_SQL, e.getMessage());
                    }
                } catch (NamingException e) {
                    LOGGER.error("Unable to acquire the connection pool to the database.", e);
                } catch (Exception e) {
                    LOGGER.error("Unable to acquire the connection pool to the database through podman client.", e);
                }
                finally{
                    LogContext.removeAction();
                    LogContext.removeResource();
                    LogContext.removeIPAddress();
                    LogContext.removeUser();
                }

            } else {
                LOGGER.info("Session expired but no user was logged in.");
            }
        }

    }
}
