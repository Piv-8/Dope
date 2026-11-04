package it.unipd.dei.webapp.dope.servlet;

import it.unipd.dei.webapp.dope.dao.*;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientContainer;
import it.unipd.dei.webapp.dope.resource.*;
import it.unipd.dei.webapp.dope.resource.Error;
import it.unipd.dei.webapp.dope.rest.GetContainerRR;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.NamingException;
import java.io.IOException;
import java.sql.SQLException;

import static it.unipd.dei.webapp.dope.resource.Error.ERROR_PROCESSING_RESOURCE;
import static it.unipd.dei.webapp.dope.resource.Error.NOT_AUTHORIZED;

/**
 * Servlet responsible for handling requests related to container creation using an image.
 *
 * <p>This servlet processes HTTP GET requests to retrieve an image by its ID and
 * prepares the data necessary for creating a container. The image details are
 * then forwarded to a JSP page for further processing.
 * If the image is not found or if an error occurs, an appropriate message is returned.</p>
 */
public class ContainerServlet extends AbstractDatabaseServlet {

    /**
     * Handles HTTP GET requests for creating a container using a specified image.
     *
     * <p>This method retrieves the image details using the image ID provided in the request parameter.
     * It also checks if the user exists in the database and prepares the necessary attributes for
     * creating a container. If the image is found and the user is valid, the details are forwarded
     * to the container creation page. If any errors occur, an appropriate error message is returned.</p>
     *
     * @param req the HTTP request containing the image ID parameter.
     * @param res the HTTP response to send the result or error message.
     * @throws ServletException if an error occurs while processing the request.
     * @throws IOException if an error occurs while writing the response.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
        //Set logs
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setAction(Actions.USE_CONTAINER);
        LogContext.setResource(req.getRequestURI());
        String user = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setUser(user);

        Message m = null;
        Image src_image = null;
        int id_user = -1;
        int image_id = -1;

        if(req.getParameter("id_image") != null){
            image_id = Integer.parseInt(req.getParameter("id_image"));
        }

        LOGGER.info("Container request for image {}.", image_id);
        // Get Objects
        try {
            src_image = new GetImageDAO(getConnection(), image_id).access().getOutputParam();
            id_user = new SearchUserDAO(getConnection(), -1, user, null, null, null, null).access().getOutputParam().get(0).getID_User();
            int access = new GetUserResourceAccessDAO(getConnection(), src_image.getID_image(), id_user, true).access().getOutputParam();

            if(access>0) {
                //if the image is running, it can't be used
                if ("building".equalsIgnoreCase(src_image.getStatus())) {
                    LOGGER.warn("Cannot use the image {}: it is currently building", image_id);
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    m = new Message("Cannot use the image: it is currently building", Error.WRONG_RESOURCE_PROVIDED, "The image is not ready yet");
                    m.toJSON(res.getOutputStream());
                    return;
                }

                Container container = new GetContainerDAO(getConnection(), user).access().getOutputParam();
                //First of all kill all previous containers
                new PodmanClientContainer(container).killContainer();
                new DeleteContainerFromUserDAO(getConnection(), id_user).access();

                if (src_image != null && id_user > -1) {
                    // Set attributes necessary to create a container
                    req.setAttribute("tag", src_image.getTag());
                    req.setAttribute("id_image", src_image.getID_image());
                    req.setAttribute("id_user", id_user);
                    // Dispatch a forward request
                    req.getRequestDispatcher("/protected/jsp/container.jsp").forward(req, res);
                } else {
                    m = new Message("Cannot get the image: not found.", Error.RESOURCE_NOT_FOUND, null);
                    LOGGER.error("Cannot get the image: not found.");
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    m.toJSON(res.getOutputStream());
                }
            } else {
                LOGGER.error("Error while creating the Container: User doesn't have access to the image");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                m = new Message("Error while creating the Container: User doesn't have access to the image", NOT_AUTHORIZED, "User not authorized");
                m.toJSON(res.getOutputStream());
            }
        } catch (SQLException e) {
            m = new Message("Cannot get the image: unexpected error while accessing the database.", Error.INTERNAL_SERVER_ERROR_SQL,
                    e.getMessage());
            LOGGER.error("Cannot get the image: unexpected error while accessing the database.", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch (Exception e) {
            m = new Message("Cannot get the image: unexpected error while accessing the database through podman handler.", Error.INTERNAL_SERVER_ERROR_SQL,
                    e.getMessage());
            LOGGER.error("Cannot get the image: unexpected error while accessing the database through podman handler.", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } finally{
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}
