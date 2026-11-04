package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.GetContainerfileFromImageDAO;
import it.unipd.dei.webapp.dope.dao.SearchUserDAO;
import it.unipd.dei.webapp.dope.dao.GetUserResourceAccessDAO;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Containerfile;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import it.unipd.dei.webapp.dope.resource.Error;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * This class represents the REST resource responsible for handling requests to retrieve a containerfile from an image from the system.
 * It extends {@link AbstractRR} and implements the logic to fetch a containerfile from an image from the database based on the image ID.
 *
 * <p>The request must include the image ID in the URL. Upon successful retrieval, the server responds with the {@link Containerfile} object
 * and an HTTP 200 status code. If the containerfile is not found, an HTTP 404 status code is returned with an error message.
 * In case of invalid input or other errors, the appropriate error response and status code are returned.</p>
 */
public class GetContainerFileFromImageRR extends AbstractRR{

    /**
     * Constructs a new {@code GetContainerFileFromImageRR} instance.
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public GetContainerFileFromImageRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.GET_CONTAINERFILE, req, res);
    }

    @Override
    protected void doServe() throws IOException {
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setAction(Actions.GET_CONTAINERFILE);
        LogContext.setUser(username);

        Containerfile cf = null;
        Message m = null;

        try {
            //Extract image ID from the URI
            String path = req.getRequestURI();
            path = path.substring(path.lastIndexOf("containerfile/"));
            int imageID = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));
            LogContext.setResource("/api/containerfile/" + imageID);

            int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();
            int access = new GetUserResourceAccessDAO(getConnection(), imageID, user_id, true).access().getOutputParam();

            if(access >0){
                cf = new GetContainerfileFromImageDAO(getConnection(), imageID).access().getOutputParam();

                if(cf != null){
                    LOGGER.info("Containerfile " + cf.getName() + " retrieved");
                    res.setStatus(HttpServletResponse.SC_OK);
                    cf.toJSON(res.getOutputStream());
                }else{
                    LOGGER.error("Cannot get containerfile: containerfile not found");
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    m = new Message("Containerfile not found", Error.RESOURCE_NOT_FOUND, "Containerfile not found");
                    m.toJSON(res.getOutputStream());
                }

            }else{
                LOGGER.error("User not authorized to access containerfile");
                m = new Message("User not authorized to access containerfile", Error.AUTHENTICATION_ERROR, "User not authorized to access containerfile");
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                m.toJSON(res.getOutputStream());
            }
        } catch (SQLException e) {
            LOGGER.error("Cannot get containerfile: unexpected database error", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m = new Message("Error getting containerfile from DB", Error.INTERNAL_SERVER_ERROR_SQL, "Error getting containerfile from DB");
        } catch (NumberFormatException e) {
            LOGGER.error("Cannot get containerfile: malformed image ID", e);
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            m = new Message("Error getting containerfile", Error.NOT_AUTHORIZED, "Malformed image ID");
        }finally {
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}
