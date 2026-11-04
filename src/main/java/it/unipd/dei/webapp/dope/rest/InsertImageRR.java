package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.CheckImageByTagAndRepoDAO;
import it.unipd.dei.webapp.dope.dao.InsertImageDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.dao.GetUserResourceAccessDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.*;
import static it.unipd.dei.webapp.dope.resource.Error.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;



/**
 * This class represents the REST resource responsible for handling requests to insert a new image into the system.
 * It extends {@link AbstractRR} and implements the logic to process and persist a new {@link Image}
 * based on the data received in the HTTP request.
 *
 * <p>The request must contain a JSON representation of an {@link Image} object. Upon successful validation and insertion,
 * the server responds with the created object and an HTTP 201 status code. If any error occurs during processing,
 * an appropriate error message and status code are returned.</p>
 */

public class InsertImageRR extends AbstractRR {

    /**
     * Constructs a new {@code InsertImageRR} instance.
     *
     * @param req        the {@link HttpServletRequest} that contains the client's request.
     * @param res        the {@link HttpServletResponse} used to send the response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public InsertImageRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.INSERT_IMAGE, req, res);
    }

    /**
     * Handles the HTTP request to insert a new {@link Image} into the database.
     * This method performs the following steps:
     * <ul>
     *     <li>Logs contextual information such as IP address and user session.</li>
     *     <li>Parses the request body into an {@link Image} object using its {@code fromJSON()} method.</li>
     *     <li>(Future) Creates the image via Podman (external API).</li>
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
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(username);
        Image i = null;
        Message m = null;

        try {
            // Parse the incoming JSON into an Image object
            final Image image = Image.fromJSON(req.getInputStream());
            // An image can't be ready when it's inserted into the DB
            image.setStatus("building");

            if (image != null) {
                LogContext.setAction(Actions.INSERT_IMAGE);
                LogContext.setResource(String.valueOf(image.getID_image()));

                int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();
                int access = new GetUserResourceAccessDAO(getConnection(), image.getRepo(), user_id, false).access().getOutputParam();

                if(access == 2) {
                    if(!(new CheckImageByTagAndRepoDAO(getConnection(), image.getTag(), image.getRepo()).access().getOutputParam()))
                    {
                        i = new InsertImageDAO(getConnection(), image).access().getOutputParam();

                        if (i != null) {
                            LOGGER.info("Image {} successfully created on DB, still to be built", i.getID_image());
                            res.setStatus(HttpServletResponse.SC_CREATED);
                            i.toJSON(res.getOutputStream());
                        } else {
                            LOGGER.error("Error while creating the image");
                            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            m = new Message("Error while creating the image", ERROR_PROCESSING_RESOURCE, "Error while creating the image");
                            m.toJSON(res.getOutputStream());
                        }
                    }else{
                        LOGGER.error("Can't create the Image {}, it already exists an Image with TAG {} in the repository {}", image.getID_image(), image.getTag(), image.getRepo());
                        res.setStatus(HttpServletResponse.SC_CONFLICT);
                        m = new Message("Can't create the Image, it already exists", CONFLICT,
                                "Can't create the Image " + image.getID_image() + ", it already exists an Image with TAG " + image.getTag() + " in the repository " + image.getRepo() );
                        m.toJSON(res.getOutputStream());
                    }
                }else{
                    LOGGER.error("User {} doesn't have access to the repo {}", username, image.getRepo());
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    m = new Message("User " + username + " doesn't have access to the repo " + image.getRepo(), NOT_AUTHORIZED,
                            "User " + username + " doesn't have access to the repo " + image.getRepo());
                    m.toJSON(res.getOutputStream());
                }
            } else {
                throw new NullPointerException("Unable to parse JSON: no Image object found");
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
