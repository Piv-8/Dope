package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.GetContainerDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Container;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static it.unipd.dei.webapp.dope.resource.Error.*;

/**
 * This class handles REST requests for executing commands in a container.
 * <p>
 * It processes HTTP requests to execute commands in a user's container by:
 * <ol>
 *   <li>Extracting the command from the query parameters</li>
 *   <li>Retrieving the user's container from the database</li>
 *   <li>Executing the specified command in the container</li>
 *   <li>Returning the command output as a JSON response</li>
 * </ol>
 * <p>
 * The endpoint expects a query parameter "cmd" containing the command to execute.
 * <p>
 * URL pattern: /protected/container/exec?cmd={command}
 */
public class ExecContainerRR extends AbstractRR{
    
    /**
     * Creates a new REST resource for executing commands in a container.
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public ExecContainerRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.EXEC_CONTAINER_COMMAND, req, res);
    }

    /**
     * Processes the request to execute a command in the user's container.
     * <p>
     * This method:
     * <ol>
     *   <li>Extracts the command from the query parameters</li>
     *   <li>Retrieves the user's container from the database</li>
     *   <li>Executes the command in the container using PodmanClientContainer</li>
     *   <li>Returns the command output as a JSON response</li>
     * </ol>
     * <p>
     * Possible HTTP status codes:
     * <ul>
     *   <li>200 OK: Command executed successfully</li>
     *   <li>400 Bad Request: Invalid or missing command parameter</li>
     *   <li>404 Not Found: Container not found for the user</li>
     *   <li>500 Internal Server Error: Database error or command execution failure</li>
     * </ul>
     *
     * @throws IOException if any error occurs while writing the response.
     */
    @Override
    protected void doServe() throws IOException {
        Container c = null;
        Message m = null;

        try{
            //Get command from query
            String query = req.getQueryString();
            Map<String, String> queryParams = Pattern.compile("&")
                .splitAsStream(query)
                .map(s -> s.split("="))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));

            String command = queryParams.get("cmd");
            LOGGER.info("Command: " + command);
            command = java.net.URLDecoder.decode(command, StandardCharsets.UTF_8);

            if(command.isEmpty()){
                throw new IndexOutOfBoundsException("Missing required command query parameter");
            }


            String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
            c = new GetContainerDAO(getConnection(), username).access().getOutputParam();

            if(c != null){
                LogContext.setResource(String.valueOf(c.getInstanceId()));
                LOGGER.info("Container successfully retrieved");
                PodmanClientContainer pcc = new PodmanClientContainer(c);
                String output = pcc.executeCommand(command);
                res.setStatus(HttpServletResponse.SC_OK);
                m = new Message(output);
                m.toJSON(res.getOutputStream());
            }else{
                LOGGER.error("Error while getting the container, not found");
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                m = new Message("Error while getting container: not found", RESOURCE_NOT_FOUND,null);
                m.toJSON(res.getOutputStream());
            }
        }catch (IndexOutOfBoundsException | NumberFormatException ex) {
            LOGGER.warn("Cannot read the container: wrong format for URI /protected/container/exec?cmd={command}.", ex);

            m = new Message("Cannot read the container: wrong format for URI /protected/container/exec?cmd={command}.", WRONG_URI,
                    ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            LOGGER.error("Cannot read the container: unexpected database error.", ex);

            m = new Message("Cannot read the container: unexpected database error.", ERROR_PROCESSING_RESOURCE, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch(Exception e){
            LOGGER.error("Couldn't exec the command: %s", e.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m = new Message("Cannot exec the command.", INTERNAL_SERVER_ERROR_TC, e.getMessage());
            m.toJSON(res.getOutputStream());
        }
    }
}
