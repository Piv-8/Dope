package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.GetImageDAO;
import it.unipd.dei.webapp.dope.dao.GetUserResourceAccessDAO;
import it.unipd.dei.webapp.dope.dao.InsertContainerDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static it.unipd.dei.webapp.dope.resource.Error.*;

import java.io.EOFException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class represents the REST resource responsible for handling requests to insert a new container into the system.
 * It extends {@link AbstractRR} and implements the logic to process and persist a new {@link Container}
 * based on the data received in the HTTP request.
 *
 * <p>The request must contain a JSON representation of a {@link Container} object. Upon successful validation and insertion,
 * the server responds with the created object and an HTTP 201 status code. If any error occurs during processing,
 * an appropriate error message and status code are returned.</p>
 */
public class InsertContainerRR extends AbstractRR {

    /**
     * Constructs a new {@link InsertContainerRR} instance.
     *
     * @param req the {@link HttpServletRequest} containing the client request
     * @param res the {@link HttpServletResponse} used to send the response to the client
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public InsertContainerRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.INSERT_CONTAINER, req, res);
    }

    /**
     * Handles the HTTP request to insert a new {@link Container} into the database.
     * This method performs the following steps:
     * <ul>
     *     <li>Logs contextual information such as IP address and user session.</li>
     *     <li>Parses the request body into a {@link Container} object using its {@code fromJSON()} method.</li>
     *     <li>(Future) Creates the container via Podman (external API).</li>
     *     <li>Persists the container using {@link InsertContainerDAO}.</li>
     *     <li>Sends back the created container and HTTP 201 status if successful, or a relevant error response otherwise.</li>
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
        Container c = null;
        Message m = null;
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(String.valueOf(req.getSession().getAttribute("userId")));

        try {
            // Parse the incoming JSON into a Container object
            final Container container = Container.fromJSON(req.getInputStream());

            if (container != null) {
                LogContext.setResource(String.valueOf(container.getInstanceId()));
                LogContext.setAction(Actions.INSERT_CONTAINER);

                Image img = new GetImageDAO(getConnection(), container.getImageRun()).access().getOutputParam();
                int user_id = new SearchUserDAO(getConnection(), -1, username, null, null, null, null).access().getOutputParam().get(0).getID_User();
                int access = new GetUserResourceAccessDAO(getConnection(), img.getID_image(), user_id, true).access().getOutputParam();
                if(access>0) {
                    // Create a DAO with the container and store it
                    c = new InsertContainerDAO(getConnection(), container).access().getOutputParam();


                    //if the image is running, it can't be used
                    if ("building".equalsIgnoreCase(img.getStatus())) {
                        LOGGER.warn("Cannot use the image {}: it is currently building", img.getID_image());
                        res.setStatus(HttpServletResponse.SC_CONFLICT);
                        m = new Message("Cannot use the image: it is currently building", ERROR_PROCESSING_RESOURCE, "The image is not ready yet");
                        m.toJSON(res.getOutputStream());
                        return;
                    }
                    // Create a container through the PODMAN API
                    PodmanClientContainer pcc = new PodmanClientContainer(c);
                    String podmanResponse = pcc.createContainer(null);

                    if (podmanResponse != null) {
                        LOGGER.info("Container {} successfully created", c.getInstanceId());
                        res.setStatus(HttpServletResponse.SC_CREATED);
                        c.toJSON(res.getOutputStream());
                    } else {
                        LOGGER.error("Error while creating the container");
                        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        m = new Message("Error while creating the container", ERROR_PROCESSING_RESOURCE, "Error while creating the container");
                        m.toJSON(res.getOutputStream());
                    }
                } else {
                    LOGGER.error("Error while creating the Container: User doesn't have access to the image");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    m = new Message("Error while creating the Container: User doesn't have access to the image", NOT_AUTHORIZED, "User not authorized");
                    m.toJSON(res.getOutputStream());
                }
            } else {
                throw new NullPointerException("Unable to parse JSON: no Container object found");
            }

        } catch (EOFException ex) {
            LOGGER.warn("Cannot create the container: No valid JSON found in the request", ex);
            m = new Message("Cannot create the container: No valid JSON found in the request", WRONG_RESOURCE_PROVIDED, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            LOGGER.error("Can't create the container, unexpected SQL error", ex);
            m = new Message("Can't create the container, unexpected SQL error", INTERNAL_SERVER_ERROR_SQL, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurred while creating the container", ex);
            m = new Message("An unexpected error occurred while creating the container", INTERNAL_SERVER_ERROR_TC, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
