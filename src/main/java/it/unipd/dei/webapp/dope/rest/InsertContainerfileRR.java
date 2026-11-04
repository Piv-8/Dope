package it.unipd.dei.webapp.dope.rest;

import it.unipd.dei.webapp.dope.dao.*;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.podman.PodmanClientImage;
import it.unipd.dei.webapp.dope.resource.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static it.unipd.dei.webapp.dope.resource.Error.*;

import java.io.EOFException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This class represents the REST resource responsible for handling requests to insert a new containerfile into the web application.
 * It extends the {@link AbstractRR} class and implements the logic to process and persist a new {@link Containerfile}
 * based on the data received in the HTTP request.
 *
 * <p>The request must contain a JSON representation of a {@link Containerfile} object. Upon successful validation and insertion,
 * the server responds with the created object and an HTTP 201 status code. If any error occurs during processing, an appropriate
 * error message and status code are returned.</p>
 */
public class InsertContainerfileRR extends AbstractRR {

    /**
     * Constructs a new {@link InsertContainerfileRR} instance.
     *
     * @param req the {@link HttpServletRequest} containing the client request
     * @param res the {@link HttpServletResponse} used to send the response to the client
     * @throws ServletException     if an error occurs while acquiring the database connection.
     */
    public InsertContainerfileRR(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        super(Actions.INSERT_CONTAINERFILE, req, res);
    }

    /**
     * Handles the HTTP request to insert a new {@link Containerfile} into the system.
     * This method performs the following steps:
     * <ul>
     *     <li>Parses the incoming JSON request body into a {@link Containerfile} object.</li>
     *     <li>Logs the containerfile name to be stored for contextual tracking.</li>
     *     <li>Attempts to insert the containerfile in the database using the {@link InsertContainerfileDAO}.</li>
     *     <li>Responds with a success message and the created containerfile if insertion is successful, or an error message if not.</li>
     * </ul>
     *
     * Error handling includes:
     * <ul>
     *     <li>Malformed or missing JSON input (400 Bad Request)</li>
     *     <li>Database errors or unexpected failures (500 Internal Server Error)</li>
     * </ul>
     *
     * @throws IOException if any I/O error occurs during the process of handling the request or sending the response.
     */
    @Override
    protected void doServe() throws IOException {
        Containerfile cf = null;
        Message m = null;
        String username = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);

        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(username);

        try {
            // Parsing the incoming JSON into a Containerfile object
            final Containerfile cf_tmp = Containerfile.fromJSON(req.getInputStream());

            // Check if the parsed Containerfile is not null before proceeding
            if (cf_tmp != null) {
                LogContext.setResource(cf_tmp.getName());
                LogContext.setAction(Actions.INSERT_CONTAINERFILE);

                int user_id = new SearchUserDAO(getConnection(), -1, username, null, null,null, null).access().getOutputParam().get(0).getID_User();
                int access = new GetUserResourceAccessDAO(getConnection(), cf_tmp.getImage_Built(), user_id, true).access().getOutputParam();

                if(access == 2) {

                    // Create a DAO with the containerfile and store it
                    cf = new InsertContainerfileDAO(getConnection(), cf_tmp).access().getOutputParam();


                    if (cf != null) {
                        LOGGER.info("Containerfile {} successfully stored", cf.getName());

                        //After creating the containerfile, build the image
                        Image i = new GetImageDAO(getConnection(), cf.getImage_Built()).access().getOutputParam();

                        //if image is running, it can't be used
                        String from_field = cf.getFrom();
                        String[] parts = from_field.split("/");
                        //Need to check whether there is a /, if there is check if the repo is localhost, if it's localhost check if the image
                        //exists and is not building
                        if(parts.length > 1 && parts[0].equalsIgnoreCase("localhost") && parts[1] != null){

                            int id_image = Integer.parseInt(parts[1]);

                            Image img = new GetImageDAO(getConnection(),id_image).access().getOutputParam();
                            if("building".equalsIgnoreCase(img.getStatus())){
                                new DeleteContainerfileDAO(getConnection(), cf.getName()).access();
                                LOGGER.warn("Cannot use the selected FROM image {}: it is currently building", i.getID_image());
                                res.setStatus(HttpServletResponse.SC_CONFLICT);
                                m = new Message("Cannot use the selected FROM image: it is currently building", ERROR_PROCESSING_RESOURCE, "The image is not ready yet");
                                m.toJSON(res.getOutputStream());
                                return;
                            }
                        }

                        PodmanClientImage pci = new PodmanClientImage(i);
                        String imageTag = pci.buildImage(cf);
                        //After building the image, set it as ready and store the layers
                        if (imageTag != null) {
                            i.setStatus("ready");
                            new UpdateImageDAO(getConnection(), i).access();
                            for (Layer l : pci.getLayers()) {
                                new InsertLayerDAO(getConnection(), l).access();
                            }
                            res.setStatus(HttpServletResponse.SC_CREATED);
                            cf.toJSON(res.getOutputStream());
                        } else {
                            //If the image can't be built delete the containerfile and the image
                            new DeleteContainerfileDAO(getConnection(), cf.getName()).access();
                            new DeleteImageDAO(getConnection(), cf.getImage_Built()).access();
                            LOGGER.error("Couldn't build the image");
                            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            m = new Message("Couldn't build the image", ERROR_PROCESSING_RESOURCE, "Couldn't build the image");
                            m.toJSON(res.getOutputStream());
                        }
                    } else {
                        LOGGER.error("Error while creating the Containerfile");
                        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        m = new Message("Error while creating the Containerfile", ERROR_PROCESSING_RESOURCE, "Error while creating the Containerfile");
                        m.toJSON(res.getOutputStream());
                    }
                }else{
                    LOGGER.error("Error while creating the Containerfile: User doesn't have access to the image");
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    m = new Message("Error while creating the Containerfile: User doesn't have access to the image", NOT_AUTHORIZED, "User doesn't have access to the image");
                    m.toJSON(res.getOutputStream());
                }
            } else {
                throw new EOFException("Unable to parse JSON: no Containerfile object found");
            }
        } catch (EOFException ex) {
            LOGGER.warn("Cannot create the Containerfile: No valid JSON found in the request", ex);
            m = new Message("Cannot create the Containerfile: No valid JSON found in the request", WRONG_RESOURCE_PROVIDED, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            m.toJSON(res.getOutputStream());
        } catch (SQLException ex) {
            LOGGER.error("Can't create the Containerfile, unexpected SQL error", ex);
            m = new Message("Can't create the Containerfile, unexpected SQL error", INTERNAL_SERVER_ERROR_SQL, ex.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            m.toJSON(res.getOutputStream());
        } catch (Exception e) {
            LOGGER.error("Error while building the image through podman", e);
            m = new Message("Error while building the image through podman", INTERNAL_SERVER_ERROR_TC, e.getMessage());
            m.toJSON(res.getOutputStream());
            throw new RuntimeException(e);
        } finally {
            // Clear LogContext
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}

