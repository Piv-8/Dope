package it.unipd.dei.webapp.dope.servlet;

import it.unipd.dei.webapp.dope.dao.*;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.*;
import it.unipd.dei.webapp.dope.resource.Error;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Servlet responsible for retrieving and displaying the details of an image.
 *
 * <p>This servlet processes GET requests for fetching and displaying the details
 * of a specific image. It retrieves the image metadata (e.g., tag, description,
 * status) from the database, as well as the associated layers and the user
 * who uploaded the image. If the image is not found, it returns a 404 error
 * response. If any database errors occur, it returns a 500 error response. </p>
 */
public class ImageDetailServlet extends AbstractDatabaseServlet {

    /**
     * Handles GET requests to retrieve and display the details of a specific image.
     *
     * <p>This method retrieves the image details (e.g., tag, description, status,
     * repository), the user who uploaded the image, and the associated layers from
     * the database. If the image is found, the details are set as request attributes
     * and forwarded to the "imageDetails.jsp" page. If the image is not found or if
     * there is a database error, an appropriate error message is returned.</p>
     *
     * @param req the HTTP request containing the image ID and user session.
     * @param res the HTTP response used to send the image details or error message.
     * @throws ServletException if an error occurs while processing the request.
     * @throws IOException if an error occurs while writing the response.
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        //Set logs
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setAction(Actions.SHOW_IMAGE_DETAIL);
        LogContext.setResource(req.getRequestURI());
        String user = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setUser(user);

        Message m = null;
        Image src_img = null;
        Repository img_repo = null;
        User img_user = null;

        List<Layer> layers = null;

        int img_id = Integer.parseInt(req.getParameter("id_image"));
        LOGGER.info("Detail request for image with ID: {} ", img_id);

        try {
            int user_id = new SearchUserDAO(getConnection(), -1, user, null, null,null, null).access().getOutputParam().get(0).getID_User();
            int access = new GetUserResourceAccessDAO(getConnection(), img_id, user_id, true).access().getOutputParam();
            if(access > 0) {
                src_img = new GetImageDAO(getConnection(), img_id).access().getOutputParam();

                if (src_img != null) {
                    // Set attributes necessary to show an image
                    req.setAttribute("tag", src_img.getTag());
                    req.setAttribute("description", src_img.getDescription());
                    req.setAttribute("status", src_img.getStatus());
                    req.setAttribute("id_image", src_img.getID_image());

                    img_repo = new GetRepositoryDAO(getConnection(), src_img.getRepo()).access().getOutputParam();

                    req.setAttribute("repo", img_repo);

                    img_user = new GetUserfromImageDAO(getConnection(), img_id).access().getOutputParam();

                    if (img_user != null) {

                        req.setAttribute("user", img_user);
                        req.setAttribute("id_user", img_user.getID_User());
                        //TODO: Handle Layers
                        layers = new ListLayersFromImageDAO(getConnection(), src_img).access().getOutputParam();

                        // Process each layer to update the ID_Layer if it contains "localhost"
                        for (Layer layer : layers) {
                            String idLayer = layer.getID_Layer();
                            LOGGER.info("Original ID_Layer: {}", idLayer);

                            if (idLayer.contains("localhost")) {
                                try {
                                    String[] parts = idLayer.split("/");
                                    if (parts.length == 2) {
                                        // Extract original id_layer
                                        String[] idTagParts = parts[1].split(":");
                                        int localImageId = Integer.parseInt(idTagParts[0]); // only the numeric ID

                                        LOGGER.info("Parsed local image ID: {}", localImageId);

                                        Image localImage = new GetImageDAO(getConnection(), localImageId).access().getOutputParam();

                                        if (localImage != null) {
                                            String tag = localImage.getTag();
                                            int repoId = localImage.getRepo();

                                            Repository localRepo = new GetRepositoryDAO(getConnection(), repoId).access().getOutputParam();

                                            if (localRepo != null) {
                                                // Build new ID_Layer in the form: "3:latest/repoName/tag"
                                                String newIdLayer = localRepo.getName() + "/" + tag +"   (ID:" + localImageId + ")";
                                                LOGGER.info("Replacing ID_Layer '{}' with '{}'", idLayer, newIdLayer);

                                                layer.setID_Layer(newIdLayer);
                                            } else {
                                                LOGGER.warn("Repository not found for ID: {}", repoId);
                                            }
                                        } else {
                                            LOGGER.warn("Image not found for ID: {}", localImageId);
                                        }
                                    } else {
                                        LOGGER.warn("ID_Layer '{}' does not have exactly 2 parts", idLayer);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Error while transforming ID_Layer '{}': {}", idLayer, e.getMessage(), e);
                                }
                            }
                        }



                        req.setAttribute("layers", layers);

                        //build the string into the layer field:
                        //String label =  img_repo.name + "/" + src_img.getTag() + " (ID: " + img_id + ")";
                        //req.setAttribute("label", label);



                        // Dispatch a forward request
                        req.getRequestDispatcher("/protected/jsp/imageDetails.jsp").forward(req, res);
                    }
                } else {
                    m = new Message("Cannot get the image: not found.", Error.RESOURCE_NOT_FOUND, null);
                    LOGGER.error("Cannot get the image: not found.");
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    m.toJSON(res.getOutputStream());
                }
            }else {
                m = new Message("User not authorized to get the image", Error.NOT_AUTHORIZED, "User not authorized to get the image");
                LOGGER.error("User not authorized to get the image");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                m.toJSON(res.getOutputStream());
            }
        } catch (SQLException e) {
            m = new Message("Cannot get the image: unexpected error while accessing the database.", Error.INTERNAL_SERVER_ERROR_SQL,
                    e.getMessage());
            LOGGER.error("Cannot get the image: unexpected error while accessing the database.", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        }
        finally{
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}
