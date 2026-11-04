package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.*;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.podman.PodmanClientImage;
import it.unipd.dei.webapp.dope.resource.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static it.unipd.dei.webapp.dope.resource.Error.*;

/**
 * This class represents the REST resource responsible for handling requests to insert a new image into the system.
 * It extends {@link AbstractRR} and implements the logic to process and persist a new {@link Image}
 * based on the data received in the HTTP request.
 *
 * <p>The request must contain a JSON representation of an {@link Image} object. Upon successful validation and insertion,
 * the server responds with the created object and an HTTP 201 status code. If any error occurs during processing,
 * an appropriate error message and status code are returned.</p>
 */

public class CommitContainerRR extends AbstractRR {

    /**
     * Constructs a new {@code InsertImageRR} instance.
     *
     * @param req        the {@link HttpServletRequest} that contains the client's request.
     * @param res        the {@link HttpServletResponse} used to send the response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public CommitContainerRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.COMMIT_CONTAINER, req, res);
    }

    /**
     * Handles the HTTP request to insert a new {@link Image} into the database.
     * This method performs the following steps:
     * <ul>
     *     <li>Logs contextual information such as IP address and user session.</li>
     *     <li>Parses the request body into an {@link Image} object using its {@code fromJSON()} method.</li>
     *     <li>Commits the image via Podman (external API).</li>
     *     <li>Persists the image using {@link InsertImageDAO}.</li>
     *     <li>Sends back the created image and HTTP 201 status if successful, or a relevant error response otherwise.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Malformed or missing JSON input (400 Bad Request)</li>
     *     <li>Duplicate entries (409 Conflict for SQLState 23505)</li>
     *     <li>Database errors or unexpected failures (500 Internal Server Error)</li>
     * </ul>
     *
     * @throws IOException if an I/O error occurs while reading the request or writing the response.
     */
    @Override
    protected void doServe() throws IOException {

        LogContext.setIPAddress(req.getRemoteAddr());
        String username = req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE).toString();
        LogContext.setUser(username);
        Image i = null;
        Message m = null;

        try {
            // Parse the incoming JSON into an Image object
            final Image image = Image.fromJSON(req.getInputStream());

            int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();
            int access = new GetUserResourceAccessDAO(getConnection(), image.getRepo(), user_id, false).access().getOutputParam();
            Boolean duplicate = new CheckImageByTagAndRepoDAO(getConnection(), image.getTag(), image.getRepo()).access().getOutputParam();

            if (access == 2 && !duplicate) {
                LogContext.setAction(Actions.COMMIT_CONTAINER);
                LogContext.setResource(String.valueOf(image.getID_image()));

                // TODO: Create image with Podman through REST API

                i = new InsertImageDAO(getConnection(), image).access().getOutputParam();
                PodmanClientContainer pcc = new PodmanClientContainer(new GetContainerDAO(getConnection(), username).access().getOutputParam());
                String new_id = pcc.commitImage(i.getID_image());

                if (new_id != null) {
                    // Add layers to DB
                    PodmanClientImage pci = new PodmanClientImage(i);
                    List<Layer> layers = pci.getLayers();
                    for (Layer l : layers) {
                        new InsertLayerDAO(getConnection(), l).access();
                    }
                    LOGGER.info("Image successfully created");
                    res.setStatus(HttpServletResponse.SC_CREATED);
                    i.toJSON(res.getOutputStream());
                } else {
                    // Delete the image if the commit fails
                    new DeleteImageDAO(getConnection(), i.getID_image()).access();
                    LOGGER.error("Error while creating the image");
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    m = new Message("Error while creating the image", ERROR_PROCESSING_RESOURCE, "Error while creating the image");
                    m.toJSON(res.getOutputStream());
                }
            } else {
                if(access < 2) {
                    LOGGER.error("User not authorized to commit on selected repo");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    m = new Message("User not authorized to commit on selected repo", NOT_AUTHORIZED, "User not authorized");
                    m.toJSON(res.getOutputStream());
                }else if(duplicate){
                    LOGGER.error("Image already exists with that tag and repo");
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    m = new Message("Error, can't commit the image", INTERNAL_SERVER_ERROR_SQL, "An image with the same tag and repo already exists");
                    m.toJSON(res.getOutputStream());
                }
                else{
                    LOGGER.error("Error while committing the image, image null");
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    m = new Message("Error while committing the image", ERROR_PROCESSING_RESOURCE, "Error while committing the image");
                    m.toJSON(res.getOutputStream());
                }
            }
        } catch (EOFException ex) {
            LOGGER.warn("Cannot create the Image: No valid JSON found in the request", ex);
            m = new Message("Cannot create the Image: No valid JSON found in the request", WRONG_RESOURCE_PROVIDED, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState())) {
                LOGGER.warn("Can't create the Image, it already exists");
                m = new Message("Can't create the Image, it already exists", CONFLICT, ex.getMessage());
                res.setStatus(HttpServletResponse.SC_CONFLICT);
            } else {
                LOGGER.error("Can't create the Image, unexpected SQL error", ex);
                m = new Message("Can't create the Image, unexpected SQL error", INTERNAL_SERVER_ERROR_SQL, ex.getMessage());
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                m.toJSON(res.getOutputStream());
            }
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurred while creating the image", ex);
            m = new Message("An unexpected error occurred while creating the image", INTERNAL_SERVER_ERROR_TC, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        }
    }
}
