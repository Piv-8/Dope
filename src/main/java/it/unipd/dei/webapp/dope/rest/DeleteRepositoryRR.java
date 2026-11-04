package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.DeleteRepositoryDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.dao.GetUserResourceAccessDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.Actions;
import static it.unipd.dei.webapp.dope.resource.Error.*;

import it.unipd.dei.webapp.dope.resource.Repository;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import it.unipd.dei.webapp.dope.resource.Image;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * This class represents the REST resource responsible for handling requests to delete a {@link Repository} and its associated {@link Image} resources from the system.
 * It extends {@link AbstractRR} and implements the logic to process the deletion of a {@link Repository} resource and all associated {@link Image} resources based on the repository ID.
 *
 * <p>The request must contain the repository ID as part of the URI. Upon successful deletion, the server responds with an HTTP 200 status code.
 * If the repository is not found, or if any error occurs during the process, an appropriate error message and status code are returned.</p>
 */
public class DeleteRepositoryRR extends AbstractRR {

    /**
     * Constructs a new {@code DeleteRepositoryRR} instance.
     * <p>This constructor initializes the REST resource for deleting a repository and its associated images by setting up
     * the necessary information such as the HTTP request, response, and database connection.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public DeleteRepositoryRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.DELETE_REPOSITORY, req, res);
    }

    /**
     * Handles the HTTP request to delete a {@link Repository} and all its associated {@link Image} from the system.
     * This method performs the following steps:
     * <ul>
     *     <li>Extracts the repository ID from the URI of the request.</li>
     *     <li>Logs the repository ID to be stored for contextual tracking.</li>
     *     <li>Attempts to delete the repository and its associated images from the database using {@link DeleteRepositoryDAO}.</li>
     *     <li>Responds with an HTTP 200 status code if deletion is successful, or an appropriate error message and status code if not.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Malformed or missing repository ID (400 Bad Request)</li>
     *     <li>Repository not found (404 Not Found)</li>
     *     <li>Database errors or unexpected failures (500 Internal Server Error)</li>
     * </ul>
     *
     * @throws IOException if any I/O error occurs during the process of handling the request or sending the response.
     */
    @Override
    protected void doServe() throws IOException {
        String username = req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE).toString();
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(username);

        Message m = null;
        try {
            // Extract repository ID from the URI
            String path = req.getRequestURI();
            String repositoryIDString = path.substring(path.lastIndexOf("/protected/repository/") + 22);
            int repositoryID = Integer.parseInt(repositoryIDString);

            // Check if repository ID is valid
            LogContext.setResource(String.valueOf(repositoryID));
            LogContext.setAction(Actions.DELETE_REPOSITORY);

            int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();
            int access = new GetUserResourceAccessDAO(getConnection(), repositoryID, user_id, false).access().getOutputParam();

            if(access == 2){
                DeleteRepositoryDAO dao = new DeleteRepositoryDAO(getConnection(), repositoryID);
                dao.access();

                if (dao.getOutputParam()) {
                    LOGGER.info("Repository {} and all its associated images successfully deleted from DB.", repositoryID);
                    res.setStatus(HttpServletResponse.SC_OK);
                } else {
                    LOGGER.error("Repository ID {} not existent. No repositories or images deleted from DB.", repositoryID);
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    m = new Message("Repository ID " + repositoryID + " not existent. No repositories or images deleted from DB.", RESOURCE_NOT_FOUND, null);
                    m.toJSON(res.getOutputStream());
                }
            }else{
                LOGGER.error("User {} does not have permission to delete repository {}.", username, repositoryID);
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                m = new Message("User " + username + " does not have permission to delete repository " + repositoryID + ".", NOT_AUTHORIZED, null);
                m.toJSON(res.getOutputStream());
            }
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            LOGGER.warn("Cannot read the repository: wrong format for URI /repository/{repositoryID}.", ex);
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m = new Message("Cannot read the repository: wrong format for URI /repository/{repositoryID}.", WRONG_URI, null);
            m.toJSON(res.getOutputStream());
        } catch (SQLException e) {
            LOGGER.error("Error while deleting the repository and its associated images.", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m = new Message("Error while deleting the repository and its associated images.", ERROR_PROCESSING_RESOURCE, null);
            m.toJSON(res.getOutputStream());
        } finally {
            // Clear LogContext
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}
