package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.GetContainerDAO;
import it.unipd.dei.webapp.dope.dao.UpdateContainerDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.*;
import it.unipd.dei.webapp.dope.resource.Error;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * This class represents the REST resource responsible for handling requests to start a container in the web application.
 * It extends the {@link AbstractRR} class and implements the logic to process the starting of a container based on the data received
 * in the HTTP request.
 *
 * <p>The request is expected to contain the session of the authenticated user. The user must be authenticated to start a container.
 * The container status will be updated to "running" in the database.
 * If any error occurs during the process, an appropriate error message will be returned to the client.</p>
 */
public class StartContainerRR extends AbstractRR {

    /**
     * Constructs a new {@link StartContainerRR} instance.
     *
     * @param req the {@link HttpServletRequest} containing the client request
     * @param res the {@link HttpServletResponse} used to send the response to the client
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public StartContainerRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.START_CONTAINER, req, res);
    }

    /**
     * Handles the HTTP request to start an existing {@link Container} in the system.
     * This method performs the following steps:
     * <ul>
     *     <li>Extracts the container ID from the URI.</li>
     *     <li>Attempts to start the container using the {@link UpdateContainerDAO}.</li>
     *     <li>Responds with a success message and an HTTP 200 status code if the container is started successfully,
     *     or an error message if the container cannot be started.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Invalid or malformed container ID (400 Bad Request)</li>
     *     <li>Container not found in the system (404 Not Found)</li>
     *     <li>Database or system errors (500 Internal Server Error)</li>
     * </ul>
     *
     * @throws IOException if an error occurs while reading or writing the HTTP response.
     */
    @Override
    public void doServe() throws IOException {

        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(username);

        Message m = null;

        try {
            // Get the container in order to start it
            Container c = new GetContainerDAO(getConnection(), username).access().getOutputParam();


            if(c != null){
                PodmanClientContainer pcc = new PodmanClientContainer(c);

                LogContext.setAction(Actions.START_CONTAINER);
                LogContext.setResource("Container ID:" + c.getInstanceId());

                pcc.getStatus();

                if(pcc.startContainer() != 204){
                    m = new Message("Couldn't start container in podman instance", Error.INTERNAL_SERVER_ERROR_TC, "An error occurred while starting the container in podman instance");
                    throw new RuntimeException("Couldn't start container");
                };

                //Update the status
                String status = pcc.getStatus();
                c = new UpdateContainerDAO(getConnection(), username, new Container(c.getInstanceId(), status, -1, -1)).access().getOutputParam();

                res.setStatus(HttpServletResponse.SC_OK);
                m = new Message("Container started successfully");
                m.toJSON(res.getOutputStream());
            }else{
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                m = new Message("Couldn't start the container");
                m.toJSON(res.getOutputStream());
            }
        } catch (RuntimeException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (m != null)
                m.toJSON(res.getOutputStream());
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m = new Message("Error while starting container", Error.INTERNAL_SERVER_ERROR_TC, "An error occurred while starting the container");
            m.toJSON(res.getOutputStream());
        }
    }
}

