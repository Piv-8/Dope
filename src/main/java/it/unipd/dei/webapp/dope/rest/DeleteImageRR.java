package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.DeleteImageDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.dao.GetUserResourceAccessDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import static it.unipd.dei.webapp.dope.resource.Error.*;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * This class represents the REST resource responsible for handling requests to delete an {@link Image} from the system.
 * It extends {@link AbstractRR} and implements the logic to process the deletion of an {@link Image} resource based on its ID.
 *
 * <p>The request must contain the image ID as part of the URI. Upon successful deletion, the server responds with an HTTP 200 status code.
 * If the image is not found, or if any error occurs during the process, an appropriate error message and status code are returned.</p>
 */
public class DeleteImageRR extends AbstractRR {

    /**
     * Constructs a new {@code DeleteImageRR} instance.
     * <p>This constructor initializes the REST resource for deleting an image by setting up the necessary
     * information such as the HTTP request, response, and database connection.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public DeleteImageRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.DELETE_IMAGE, req, res);
    }

    /**
     * Handles the HTTP request to delete an {@link Image} from the system.
     * This method performs the following steps:
     * <ul>
     *     <li>Extracts the image ID from the URI of the request.</li>
     *     <li>Logs the image ID to be stored for contextual tracking.</li>
     *     <li>Attempts to delete the image from the database using {@link DeleteImageDAO}.</li>
     *     <li>Responds with an HTTP 200 status if the image is successfully deleted, or an error message if not.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Malformed or missing image ID (400 Bad Request)</li>
     *     <li>Image not found (404 Not Found)</li>
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
            // Extract image ID from the URI
            String path = req.getRequestURI();
            String imageIDString = path.substring(path.lastIndexOf("/protected/image/") + 17);
            int imageID = Integer.parseInt(imageIDString);

            // Check if image ID is valid
            LogContext.setResource(String.valueOf(imageID));
            LogContext.setAction(Actions.DELETE_IMAGE);

            int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();
            int access = new GetUserResourceAccessDAO(getConnection(), imageID, user_id, true).access().getOutputParam();

            if(access == 2) {


                DeleteImageDAO dao = new DeleteImageDAO(getConnection(), imageID);
                dao.access();

                if (dao.getOutputParam()) {
                    LOGGER.info("Image {} successfully deleted from DB.", imageID);
                    res.setStatus(HttpServletResponse.SC_OK);
                } else {
                    LOGGER.error("Image ID {} not existent. No images deleted from DB.", imageID);
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    m = new Message("Image ID " + imageID + " not existent. No images deleted from DB.", RESOURCE_NOT_FOUND, null);
                    m.toJSON(res.getOutputStream());
                }
            }else{
                LOGGER.error("User {} not authorized to delete image {}.", username, imageID);
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                m = new Message("User " + username + " not authorized to delete image " + imageID + ".", NOT_AUTHORIZED, null);
                m.toJSON(res.getOutputStream());
            }
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            LOGGER.warn("Cannot delete the image: wrong format for URI /api/protected/image/{imageID}.", ex);

            m = new Message("Cannot delete the image: wrong format for URI /api/protected/image/{imageID}.", WRONG_URI, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());

        } catch (SQLException ex) {
            LOGGER.error("Cannot delete the image: DB error");
            m = new Message("No images deleted from DB, SQL error", INTERNAL_SERVER_ERROR_SQL, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());

        } catch (Exception ae) {
            LOGGER.error("Cannot delete the image: authentication error.", ae);
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            m = new Message("Cannot delete the image: authentication error.", NOT_AUTHORIZED, ae.getMessage());
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
