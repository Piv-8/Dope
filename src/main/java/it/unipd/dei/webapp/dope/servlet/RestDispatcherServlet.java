package it.unipd.dei.webapp.dope.servlet;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.LogContext;
import it.unipd.dei.webapp.dope.resource.Message;
import it.unipd.dei.webapp.dope.rest.*;
import static it.unipd.dei.webapp.dope.resource.Error.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;


/**
 * This class is a servlet responsible for dispatching REST requests to the appropriate resource handler.
 * It extends {@link AbstractDatabaseServlet} and processes various REST endpoints related to containers, images,
 * container files, layers, users, and repositories. The servlet routes the incoming HTTP requests to the relevant
 * handler based on the requested URI and HTTP method (e.g., POST, GET, PUT, DELETE).
 *
 * <p>The servlet checks if the request matches predefined paths for resources such as containers, images, and others,
 * and invokes the corresponding handler. If an unknown resource or unsupported method is encountered, it returns an error response.</p>
 *
 * <p>It also handles errors by logging them and returning appropriate error messages to the client.</p>
 */
public final class RestDispatcherServlet extends AbstractDatabaseServlet {

    private static final String JSON_UTF_8_MEDIA_TYPE = "application/json; charset=utf-8";


    /**
     * Dispatches the incoming HTTP request to the appropriate handler based on the requested URI and method.
     * <p>This method processes the HTTP request and determines the correct resource handler based on the URI</p>
     *
     * <p>If no matching resource is found, it returns an HTTP 404 status with an error message.</p>
     *
     * @param req the HTTP request.
     * @param res the HTTP response.
     * @throws IOException if an I/O error occurs during request processing.
     */
    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setAction(Actions.REST_DISPATCH);
        LogContext.setResource(req.getRequestURI());

        final OutputStream out = res.getOutputStream();

        // if endpoint contains api/protected then require user session
        // else allow anonymous access

        //Process all the REST endpoints
        try {
            if (processContainerFile(req, res)) {
                return;
            } else if (processImage(req, res)) {
                return;
            } else if (processContainer(req, res)) {
                return;
            } else if (processUser(req, res)) {
                return;
            } else if (processRepository(req, res)) {
                return;
            } else {
                //TODO: Standardize error messages through a resource
                final Message m = new Message("An error occurred, operation unknown", RESOURCE_NOT_FOUND, "Operation Unknown");
                LOGGER.error("Unknown resource requested: {}.", req.getRequestURI());
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                m.toJSON(out);
            }
        } catch (Throwable t) {
            LOGGER.error("Unexpected error while processing the REST request.", t);
            final Message m = new Message("Unexpected error.", INTERNAL_SERVER_ERROR_SQL, t.getMessage());
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.setContentType(JSON_UTF_8_MEDIA_TYPE);
            m.toJSON(out);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }

            LogContext.removeIPAddress();
        }

    }

    /**
     * Processes incoming HTTP requests for container-related operations. This method handles various HTTP methods (POST, PUT, GET)
     * for the `/api/container` endpoint and its sub-paths (such as `/api/container/start`, `/api/container/stop`, etc.).
     *
     * <p>Depending on the requested method and path, it invokes the corresponding resource handler to either create, update, retrieve,
     * or control containers. If the request does not match any allowed operations, an appropriate error message is returned.</p>
     *
     * @param req the HTTP request containing the details of the container operation.
     * @param res the HTTP response where the result or error message will be written.
     * @return {@code true} if the request was successfully handled; {@code false} if the request does not match the container operations.
     * @throws Exception if an error occurs during processing the request.
     */
    private boolean processContainer(final HttpServletRequest req, final HttpServletResponse res) throws Exception {
        final String method = req.getMethod();

        String path = req.getRequestURI();
        Message m = null;

        if (path.lastIndexOf("/api/protected/container") <= 0) {
            return false;
        }

        // Strip the /api/protected/container from the path
        path = path.substring(path.lastIndexOf("container") + 9);
        // Either inserts new Container with POST or updates existing Container with PUT
        if (path.isEmpty() || path.charAt(0) == '/') {
            switch (method) {
                case "POST":
                    if (path.contains("start")) {
                        new StartContainerRR(req, res).serve();
                    } else if (path.contains("kill")) {
                        new KillContainerRR(req, res).serve();
                    } else if (path.contains("commit")) {
                        new CommitContainerRR(req, res).serve();
                    } else if(path.contains("exec")){
                        new ExecContainerRR(req, res).serve();
                    } else if (path.contains("log")){
                        new GetContainerLogsRR(req, res).serve();
                    }else
                        new InsertContainerRR(req, res).serve();
                    break;
                case "GET":
                    new GetContainerRR(req, res).serve();
                    break;
                default:
                    m = new Message("Operation not allowed for URI /api/protected/container.", METHOD_NOT_ALLOWED, "Method not allowed");
                    LOGGER.warn("Unsupported operation for URI /api/protected/container: {}.", method);
                    res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                    m.toJSON(res.getOutputStream());
                    break;
            }
        }

        return true;
    }

    /**
     * Processes incoming HTTP requests for image-related operations. This method handles various HTTP methods (POST, PUT, GET, DELETE)
     * for the `/api/image` endpoint and its sub-paths (such as `/api/image/{id}`).
     *
     * <p>Depending on the requested method and path, it invokes the corresponding resource handler to either create, update, retrieve,
     * or delete images. If the request does not match any allowed operations, an appropriate error message is returned.</p>
     *
     * @param req the HTTP request containing the details of the image operation.
     * @param res the HTTP response where the result or error message will be written.
     * @return {@code true} if the request was successfully handled; {@code false} if the request does not match the image operations.
     * @throws IOException  if an input or output error occurs during the request processing.
     * @throws SQLException if any database-related error occurs during the image operation.
     */
    private boolean processImage(final HttpServletRequest req, final HttpServletResponse res) throws IOException, SQLException, ServletException {
        final String method = req.getMethod();

        String path = req.getRequestURI();
        Message m = null;

        if(path.contains("exportImage")){
            path = path.substring(path.lastIndexOf("exportImage") + 11);
            if(path.charAt(0) == '/'){
                if (method.equals("GET")){
                    new ExportImageRR(req,res).serve();
                }else{
                    m = new Message("Operation not allowed for URI /api/protected/exportImage.", "E405", "Method not allowed");
                    LOGGER.warn("Unsupported operation for URI /api/protected/exportImage: {}.", method);
                    res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                    m.toJSON(res.getOutputStream());
                }
            }

        }else if(path.contains("image")){
            // Strip the /api/image from the path
            path = path.substring(path.lastIndexOf("image") + 5);
            // Either inserts new Image with POST or updates existing Image with PUT
            if (path.isEmpty() || path.charAt(0) == '/') {
                switch (method) {
                    case "POST":
                        new InsertImageRR(req, res).serve();
                        break;

                    case "GET":
                        new GetImageRR(req, res).serve();
                        break;

                    case "DELETE":
                        new DeleteImageRR(req, res).serve();
                        break;

                    default:
                        m = new Message("Operation not allowed for URI /api/protected/image.", METHOD_NOT_ALLOWED, "Method not allowed");
                        LOGGER.warn("Unsupported operation for URI /api/protected/image: {}.", method);
                        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                        m.toJSON(res.getOutputStream());
                        break;
                }
            }
        }else{
            return false;
        }

        return true;
    }


    /**
     * Processes incoming HTTP requests for container file-related operations. This method handles HTTP methods (POST, PUT)
     * for the `/api/containerfile` endpoint and its sub-paths (such as `/api/containerfile/{id}`).
     *
     * <p>Depending on the requested method and path, it invokes the corresponding resource handler to either create or update
     * a container file. If the request does not match any allowed operations, an appropriate error message is returned.</p>
     *
     * @param req the HTTP request containing the details of the container file operation.
     * @param res the HTTP response where the result or error message will be written.
     * @return {@code true} if the request was successfully handled; {@code false} if the request does not match the container file operations.
     * @throws Exception if an error occurs while processing the container file operation.
     */
    private boolean processContainerFile(final HttpServletRequest req, final HttpServletResponse res) throws Exception {
        final String method = req.getMethod();

        String path = req.getRequestURI();
        Message m = null;

        if(path.contains("/api/protected/buildimage")){
            // Strip the path
            path = path.substring(path.lastIndexOf("buildimage") + 10);

            // Builds a new image through a containerfile
            if (path.isEmpty() || path.charAt(0) == '/') {
                switch (method) {
                    case "POST":
                        new InsertContainerfileRR(req, res).serve();
                        break;
                    default:
                        m = new Message("Operation not allowed for URI /api/protected/buildimage.", METHOD_NOT_ALLOWED, "Method not allowed");
                        LOGGER.warn("Unsupported operation for URI /api/protected/buildimage: {}.", method);
                        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                        m.toJSON(res.getOutputStream());
                        break;
                }
            }
        }else if(path.contains("/api/protected/containerfile")) {
            // Strip the path
            path = path.substring(path.lastIndexOf("containerfile") + 13);

            if(path.charAt(0) == '/' || path.length() == 0) {
                switch (method) {
                    case "GET":
                        new GetContainerFileFromImageRR(req, res).serve();
                        break;
                    case "POST":
                        new InsertContainerfileRR(req, res).serve();
                    default:
                        m = new Message("Operation not allowed for URI /api/protected/containerfile.", METHOD_NOT_ALLOWED, "Method not allowed");
                        LOGGER.warn("Unsupported operation for URI /api/protected/containerfile: {}.", method);
                        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                        m.toJSON(res.getOutputStream());
                        break;
                }
            }

        }else{
            return false;
        }
        return true;
    }


    /**
     * Processes incoming HTTP requests for user-related operations. This method handles HTTP methods (POST)
     * for the `/api/user` endpoint and its sub-paths (such as `/api/user/{id}`).
     *
     * <p>The method supports the creation of new users via the POST method. Other operations, such as retrieving or
     * searching for users, are currently not implemented but could be extended in the future.
     * If the requested operation is not allowed, an appropriate error message is returned.</p>
     *
     * @param req the HTTP request containing the details of the user operation.
     * @param res the HTTP response where the result or error message will be written.
     * @return {@code true} if the request was successfully handled; {@code false} if the request does not match the user operations.
     * @throws IOException  if an I/O error occurs while processing the user operation.
     * @throws SQLException if an error occurs while accessing the database for the user operation.
     */
    private boolean processUser(final HttpServletRequest req, final HttpServletResponse res) throws IOException, SQLException, ServletException {
        final String method = req.getMethod();

        String path = req.getRequestURI();
        Message m = null;

        if (path.lastIndexOf("api/user") <= 0) {
            return false;
        }

        // Strip the /api/user from the path
        path = path.substring(path.lastIndexOf("user") + 4);
        // Either inserts new User with POST or lists existing Users with GET
        if (path.isEmpty() || path.charAt(0) == '/') {
            switch (method) {
                case "POST":
                    new InsertUserRR(req, res).serve();
                    break;
                default:
                    m = new Message("Operation not allowed for URI /api/user.", METHOD_NOT_ALLOWED, "Method not allowed");
                    LOGGER.warn("Unsupported operation for URI /api/user: {}.", method);
                    res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                    m.toJSON(res.getOutputStream());
                    break;
            }
        }
        return true;
    }


    /**
     * Processes incoming HTTP requests for repository-related operations. This method handles HTTP methods (DELETE)
     * for the `/api/repository` endpoint and its sub-paths (such as `/api/repository/{id}`).
     *
     * <p>The method supports deleting a repository via the DELETE method. If the requested operation is not allowed,
     * an appropriate error message is returned.</p>
     *
     * @param req the HTTP request containing the details of the repository operation.
     * @param res the HTTP response where the result or error message will be written.
     * @return {@code true} if the request was successfully handled; {@code false} if the request does not match the repository operations.
     * @throws IOException  if an I/O error occurs while processing the repository operation.
     * @throws SQLException if an error occurs while accessing the database for the repository operation.
     */
    private boolean processRepository(final HttpServletRequest req, final HttpServletResponse res) throws IOException, SQLException, ServletException {
        final String method = req.getMethod();

        String path = req.getRequestURI();
        Message m = null;

        if (path.lastIndexOf("/api/protected/repository") <= 0) {
            return false;
        }

        // Strip the /api/repository from the path
        path = path.substring(path.lastIndexOf("repository") + 10);
        // Deletes Repository with DELETE
        if (path.isEmpty() || path.charAt(0) == '/') {
            switch (method) {
                case "GET":
                    new GetWriteRepositoriesRR(req,res).serve();
                    break;

                case "DELETE":
                    new DeleteRepositoryRR(req, res).serve();
                    break;

                case "POST":
                    new InsertRepositoryRR(req,res).serve();
                    break;

                default:
                    m = new Message("Operation not allowed for URI /api/protected/repository.", METHOD_NOT_ALLOWED, "Method not allowed");
                    LOGGER.warn("Unsupported operation for URI /api/protected/repository: {}.", method);
                    res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    res.setContentType(JSON_UTF_8_MEDIA_TYPE);
                    m.toJSON(res.getOutputStream());
                    break;
            }
        }
        return true;
    }
}
