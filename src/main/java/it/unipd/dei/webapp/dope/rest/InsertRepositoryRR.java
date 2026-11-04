package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.InsertRepositoryDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
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
 * Manages the REST endpoint for inserting a new repository into the system.
 *
 * <p> This resource is accessible only to authenticated users and expects a JSON payload
 * representing a {@link Repository} object. If the request is valid and the repository
 * is successfully inserted, it returns the created resource with an HTTP 201 (Created) status.
 * In case of errors, an appropriate error response is returned.</p>
 *
 * Supported operations:
 * <ul>
 *     <li>Method: POST</li>
 *     <li>URL: /api/repository</li>
 *     <li>Authentication: Required (via {@link ProtectedResourceFilter})</li>
 *     <li>Request body: JSON representing a {@link Repository}</li>
 *     <li>Response: the created {@link Repository} in JSON format</li>
 * </ul>
 */
public class InsertRepositoryRR extends AbstractRR {

    /**
     * Creates a new {@code InsertRepositoryRR} object for handling a repository insertion request.
     *
     * @param req the {@link HttpServletRequest} object representing the client request.
     * @param res the {@link HttpServletResponse} object for sending the response.
     * @throws ServletException if an error occurs while processing the request.
     */
    public InsertRepositoryRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.INSERT_REPOSITORY, req, res);
    }

    /**
     * Processes the HTTP POST request to insert a new repository.
     * This method:
     * <ul>
     *     <li>Retrieves the authenticated user from the session.</li>
     *     <li>Parses the incoming request body into a {@link Repository} object.</li>
     *     <li>Attempts to insert the repository into the database via {@link InsertRepositoryDAO}.</li>
     *     <li>Returns a 201 response with the created repository on success.</li>
     *     <li>Returns appropriate error messages and codes in case of failure:
     *         <ul>
     *             <li>400 if JSON is malformed or empty</li>
     *             <li>409 if the repository already exists (SQLState 23505)</li>
     *             <li>500 for generic database or internal errors</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @throws IOException if an I/O error occurs while reading the request or writing the response.
     */
    @Override
    protected void doServe() throws IOException {
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(username);

        Repository r = null;
        User u = null;
        Message m = null;

        try {
            final Repository repo = Repository.fromJSON(req.getInputStream());

            u = new SearchUserDAO(getConnection(), -1, username, null, null, null, null).access().getOutputParam().get(0);

            if (repo != null && u != null) {
                if(repo.getOwner_ID() == u.getID_User()) {
                    r = new InsertRepositoryDAO(getConnection(), repo).access().getOutputParam();

                    LogContext.setAction(Actions.INSERT_REPOSITORY);
                    LogContext.setResource(String.valueOf(repo.getID_repo()));

                    if (r != null) {
                        LOGGER.info("Repository {} successfully created on DB", r.getID_repo());
                        res.setStatus(HttpServletResponse.SC_CREATED);
                        r.toJSON(res.getOutputStream());
                    } else {
                        LOGGER.error("Error while creating the Repository");
                        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        m = new Message("Error while creating the Repository", ERROR_PROCESSING_RESOURCE, "Error while creating the Repository");
                        m.toJSON(res.getOutputStream());
                    }
                }else {
                    LOGGER.error("A User can create a Repository only for himself");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    m = new Message("User " + username + " cannot create the repository " + repo.getName() + " for the User " + repo.getOwner_ID(), NOT_AUTHORIZED,
                            "A User can create a Repository only for himself");
                    m.toJSON(res.getOutputStream());
                }
            } else {
                throw new NullPointerException("Unable to parse JSON: no Repository object found");
            }
        } catch (EOFException ex) {
            LOGGER.warn("Cannot create the Repository: No valid JSON found in the request", ex);
            m = new Message("Cannot create the Repository: No valid JSON found in the request", WRONG_RESOURCE_PROVIDED, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState())) {
                LOGGER.warn("Can't create the Repository, it already exists");
                m = new Message("Can't create the Repository, it already exists", CONFLICT, ex.getMessage());
                res.setStatus(HttpServletResponse.SC_CONFLICT);
            } else {
                LOGGER.error("Can't create the Repository, unexpected SQL error", ex);
                m = new Message("Can't create the Repository, unexpected SQL error", INTERNAL_SERVER_ERROR_SQL, ex.getMessage());
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                m.toJSON(res.getOutputStream());
            }
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurred while creating the Repository", ex);
            m = new Message("An unexpected error occurred while creating the Repository", INTERNAL_SERVER_ERROR_TC, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        }
    }
}