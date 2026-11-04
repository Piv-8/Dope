package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.GetContainerDAO;
import it.unipd.dei.webapp.dope.dao.UpdateContainerDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.Error;
import it.unipd.dei.webapp.dope.resource.Message;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

/**
 * This class represents the REST resource responsible for handling requests to stop a container in the web application.
 * It extends the {@link AbstractRR} class and implements the logic to process the stopping of a container based on the data received
 * in the HTTP request.
 *
 * <p>The request is expected to contain the session of the authenticated user. The user must be authenticated to stop a container.
 * The container status will be updated to "stopped" in the database.
 * If any error occurs during the process, an appropriate error message will be returned to the client.</p>
 */
public class GetContainerLogsRR extends AbstractRR {

    /**
     * Constructs a new {@link GetContainerLogsRR} instance.
     *
     * @param req the {@link HttpServletRequest} containing the client request
     * @param res the {@link HttpServletResponse} used to send the response to the client
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public GetContainerLogsRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.GET_CONTAINER_LOGS, req, res);
    }

    /**
     * Handles the HTTP request to stop an existing {@link Container} in the system.
     * This method performs the following steps:
     * <ul>
     *     <li>Extracts the container ID from the URI.</li>
     *     <li>Attempts to stop the container using the {@link UpdateContainerDAO}.</li>
     *     <li>Responds with a success message and an HTTP 200 status code if the container is stopped successfully,
     *     or an error message if the container cannot be stopped.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Invalid or malformed container ID (400 Bad Request)</li>
     *     <li>Container not found in the system (404 Not Found)</li>
     *     <li>Database or system errors (500 Internal Server Error)</li>
     * </ul>
     *
     */
    @Override
    protected void doServe() throws IOException {
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        Message m = null;

        if (username == null) {
            // User without authentication
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            m = new Message("User not authenticated", Error.AUTHENTICATION_ERROR, "The user is not authenticated");
            throw new RuntimeException(m.getMessage());
        }

        try {
            // Get last container for user and get logs
            Container c = new GetContainerDAO(getConnection(),username).access().getOutputParam();
            PodmanClientContainer pcc = new PodmanClientContainer(c);
            res.setStatus(HttpServletResponse.SC_OK);
            m = new Message(pcc.getLogs());
            m.toJSON(res.getOutputStream());

        } catch (RuntimeException e) {
            if (m != null)
                m.toJSON(res.getOutputStream());
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m = new Message("An error occurred while killing the container", Error.INTERNAL_SERVER_ERROR_TC, "An error occurred while killing the container: " + e.getMessage());
            m.toJSON(res.getOutputStream());
        }
    }
}

