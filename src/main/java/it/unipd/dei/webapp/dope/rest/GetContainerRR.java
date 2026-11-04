package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.GetContainerDAO;
import it.unipd.dei.webapp.dope.dao.UpdateContainerDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.Actions;
import static it.unipd.dei.webapp.dope.resource.Error.*;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class represents the REST resource responsible for handling requests to retrieve a {@link Container} from the system.
 * It extends {@link AbstractRR} and implements the logic to fetch a {@link Container} from the database based on the container ID.
 *
 * <p>The request must include the container ID in the URL. Upon successful retrieval, the server responds with the {@link Container} object
 * and an HTTP 200 status code. If the container is not found, an HTTP 404 status code is returned with an error message.
 * In case of invalid input or other errors, the appropriate error response and status code are returned.</p>
 */
public class GetContainerRR extends AbstractRR {

    /**
     * Constructs a new {@code GetContainerRR} instance.
     * <p>This constructor initializes the REST resource for retrieving a container by setting up
     * the necessary information such as the HTTP request, response, and database connection.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public GetContainerRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.GET_CONTAINER, req, res);
    }

    /**
     * Handles the HTTP request to retrieve a {@link Container} from the system.
     * This method performs the following steps:
     * <ul>
     *     <li>Extracts the container ID from the request URI.</li>
     *     <li>Retrieves the corresponding {@link Container} from the database using {@link GetContainerDAO}.</li>
     *     <li>Responds with the container if found, or an error message if the container is not found or any other error occurs.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Malformed or missing container ID in the URI (400 Bad Request)</li>
     *     <li>Container not found (404 Not Found)</li>
     *     <li>Database errors or unexpected failures (500 Internal Server Error)</li>
     *     <li>Authentication errors (401 Unauthorized)</li>
     * </ul>
     *
     * @throws IOException if an I/O error occurs while reading or writing the HTTP response.
     */
    @Override
    protected void doServe() throws IOException {
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setAction(Actions.GET_CONTAINER);
        LogContext.setUser(username);

        Container c = null;
        Message m = null;

        try {
            // Extract container ID from the URI
            String path = req.getRequestURI();
            //Strip the base URL from the path
            path = path.substring(path.lastIndexOf("protected/container"));

            //Get the container from DAO
            c = new GetContainerDAO(getConnection(), username).access().getOutputParam();

            if (c != null) {
                //Inspect the container through podman and update its status
                PodmanClientContainer pcc = new PodmanClientContainer(c);
                String status = pcc.getStatus();

                //Get updated status
                c.setStatus(status);
                c = new UpdateContainerDAO(getConnection(), username, c).access().getOutputParam();

                LogContext.setResource("Container ID:" + c.getInstanceId());

                LOGGER.info("Container {} successfully retrieved", c.getInstanceId());
                res.setStatus(HttpServletResponse.SC_OK);
                c.toJSON(res.getOutputStream());
            } else {
                LOGGER.error("Error while getting the container, not found");
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                m = new Message("Error while getting container: not found", RESOURCE_NOT_FOUND, "Error while getting the container, not found");
                m.toJSON(res.getOutputStream());
            }
        } catch (IndexOutOfBoundsException | NumberFormatException ex) {
            LOGGER.warn("Cannot read the container: wrong format for URI /protected/container/{containerID}.", ex);
            m = new Message("Cannot read the container: wrong format for URI /protected/container/{containerID}.", WRONG_URI, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            LOGGER.error("Cannot read the container: unexpected database error.", ex);
            m = new Message("Cannot read the container: unexpected database error.", ERROR_PROCESSING_RESOURCE, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch(Exception e){
            LOGGER.error("Cannot read the container: unexpected error.", e);

            m = new Message("Cannot read the container: unexpected error.", ERROR_PROCESSING_RESOURCE, e.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        }finally{
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}