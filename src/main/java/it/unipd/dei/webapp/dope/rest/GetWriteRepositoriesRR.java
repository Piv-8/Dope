package it.unipd.dei.webapp.dope.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipd.dei.webapp.dope.dao.GetUserResourceAccessDAO;
import it.unipd.dei.webapp.dope.dao.ListRepositoriesDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static it.unipd.dei.webapp.dope.resource.Error.*;


/**
 * This class represents the REST resource responsible for handling requests to retrieve all {@link Repository} from the system for the authenticated user.
 * It extends {@link AbstractRR} and implements the logic to process the retrieval of all {@link Repository} resources based on the authenticated user.
 *
 * <p>Upon successful retrieval, the server responds with the array object containing the {@link Repository}
 * and an HTTP 200 status code. If any error occurs during processing, an appropriate error message
 * and status code are returned.</p>
 */
public class GetWriteRepositoriesRR extends AbstractRR {

    /**
     * Constructs a new {@code GetRepositoriesRR} instance.
     * <p>This constructor initializes the REST resource for retrieving repositories by setting up
     * the necessary information such as the HTTP request, response, and database connection.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public GetWriteRepositoriesRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.GET_REPOSITORY, req, res);
    }

    /**
     * Handles the HTTP request to retrieve the list of {@link Repository} from the database.
     * This method performs the following steps:
     * <ul>
     *     <li>Extracts the user ID from the session.</li>
     *     <li>Logs the contextual information, such as user ID and IP address.</li>
     *     <li>Attempts to retrieve the Repositories from the database using the {@link ListRepositoriesDAO}.</li>
     *     <li>Responds with the requested resources and HTTP 200 status code if successful, or an error message if not found.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
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
        List< Repository> repos = null;
        Message m = null;

        try {
            int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();

            repos = new ListRepositoriesDAO(getConnection(), user_id).access().getOutputParam();

            if (repos != null) {
                LOGGER.info("Repos retrieved");
                res.setStatus(HttpServletResponse.SC_OK);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                StringBuilder output = new StringBuilder("[");

                boolean first = true;
                for (Repository repo : repos) {
                    int access = new GetUserResourceAccessDAO(getConnection(), repo.getID_repo(), user_id, false)
                            .access().getOutputParam();

                    if (access == 2) {
                        baos.reset();
                        repo.toJSON(baos);

                        if (!first) {
                            output.append(",");
                        } else {
                            first = false;
                        }

                        output.append(baos.toString());
                    }
                }

                output.append("]");

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(output.toString());

                // Send custom JSON
                JsonFactory jsonFactory = mapper.getFactory();
                jsonFactory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
                jsonFactory.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
                final JsonGenerator jg = jsonFactory.createGenerator(res.getOutputStream());
                jg.writeTree(jsonNode);
                jg.flush();
            } else {
                LOGGER.error("Error while getting the repos");
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                m = new Message("Error while getting the repos", INTERNAL_SERVER_ERROR_SQL, null);
                m.toJSON(res.getOutputStream());
            }

        } catch (SQLException ex) {
            LOGGER.error("Cannot read the repositories: unexpected database error.", ex);
            m = new Message("Cannot read the image: unexpected database error.", ERROR_PROCESSING_RESOURCE, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch (Exception ae) {
            LOGGER.error("Cannot read the repos: unknown error.", ae);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m = new Message("Cannot read the repos: unknown error.", NOT_AUTHORIZED, ae.getMessage());
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
