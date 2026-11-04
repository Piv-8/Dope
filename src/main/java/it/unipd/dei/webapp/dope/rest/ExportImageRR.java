package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.GetImageDAO;
import it.unipd.dei.webapp.dope.dao.GetUserResourceAccessDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientImage;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import static it.unipd.dei.webapp.dope.resource.Error.*;

/**
 * This class represents the REST resource responsible for handling requests to export an {@link Image} from the system.
 * It extends {@link AbstractRR} and implements the logic to process the export of an {@link Image} resource based on the image ID.
 *
 * <p>The request must contain the image ID as part of the URI. Upon successful export request, the server responds with the exported image
 * as a downloadable file. If the image is not found or any error occurs during processing, an appropriate error message
 * and status code are returned.</p>
 */

public class ExportImageRR extends AbstractRR {

    /**
     * Constructs a new {@code ExportImageRR} instance.
     * <p>This constructor initializes the REST resource for exporting an image by setting up
     * the necessary information such as the HTTP request and response.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public ExportImageRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.GET_IMAGE, req, res);
    }

    /**
     * Handles the HTTP request to export an {@link Image} from the database.
     * This method performs the following steps:
     * <ul>
     *     <li>Extracts the image ID from the URI of the request.</li>
     *     <li>Logs the contextual information, such as user ID and IP address.</li>
     *     <li>Attempts to retrieve the image from the database using the {@link GetImageDAO}.</li>
     *     <li>Exports the image if found and responds with the image file and HTTP 200 status code.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Malformed or missing image ID in the URI (400 Bad Request)</li>
     *     <li>Image not found in the database (404 Not Found)</li>
     *     <li>Database errors or unexpected failures (500 Internal Server Error)</li>
     *     <li>Authentication errors (401 Unauthorized)</li>
     * </ul>
     *
     * @throws IOException if an error occurs while reading or writing the HTTP response.
     */
    @Override
    protected void doServe() throws IOException {
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(username);
        Image img = null;
        Message m = null;

        try {
            // Extract image ID from the URI
            String path = req.getRequestURI();
            //Strip the base URL from the path
            path = path.substring(path.lastIndexOf("exportImage"));

            // Get the image ID from the path
            int imageID = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
            LogContext.setAction(Actions.GET_IMAGE);
            LogContext.setResource(String.valueOf(imageID));

            int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();
            int access = new GetUserResourceAccessDAO(getConnection(), imageID, user_id, true).access().getOutputParam();

            if(access > 0){
                img = new GetImageDAO(getConnection(), imageID).access().getOutputParam();

                if (img != null && imageID == img.getID_image()) {
                    if(img.getStatus() != "building"){
                        PodmanClientImage pci = new PodmanClientImage(img);
                        byte[] tarImg = pci.exportImage();
                        if(tarImg != null) {
                            res.setStatus(HttpServletResponse.SC_OK);
                            res.setContentType("application/x-tar");
                            res.setHeader("Content-Disposition", "attachment; filename=\"" + img.getTag() + ".tar\"");
                            res.getOutputStream().write(tarImg);
                            res.getOutputStream().flush();
                        }else{
                            LOGGER.error("Couldn't export the image, podman client image failed");
                            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            m = new Message("Error while exporting image: " + imageID + ", internal server error", INTERNAL_SERVER_ERROR_TC, null);
                            m.toJSON(res.getOutputStream());
                        }
                    }else{
                        LOGGER.error("Error while exporting the image, image not built yet");
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        m = new Message("Error while exporting image: " + imageID + ", image not built yet", WRONG_RESOURCE_PROVIDED, null);
                        m.toJSON(res.getOutputStream());
                    }
                } else {
                    LOGGER.error("Error while getting the image, not found");
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    m = new Message("Error while getting image: " + imageID + ", not found", RESOURCE_NOT_FOUND, null);
                    m.toJSON(res.getOutputStream());
                }
            }else{
                LOGGER.error("User " + username + " does not have access to image " + imageID);
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                m = new Message("User " + username + " does not have access to image " + imageID, NOT_AUTHORIZED, null);
                m.toJSON(res.getOutputStream());
            }

        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            LOGGER.warn("Cannot read the image: wrong format for URI /image/{imageID}.", ex);

            m = new Message("Cannot read the image: wrong format for URI /image/{imageID}.", WRONG_URI, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            LOGGER.error("Cannot read the image: unexpected database error.", ex);
            m = new Message("Cannot read the image: unexpected database error.", ERROR_PROCESSING_RESOURCE, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch (Exception ae) {
            LOGGER.error("Cannot read the image internal server error.", ae);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m = new Message("Cannot read the image: internal server error.", INTERNAL_SERVER_ERROR_TC, ae.getMessage());
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
