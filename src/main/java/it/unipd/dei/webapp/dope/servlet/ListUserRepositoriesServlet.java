package it.unipd.dei.webapp.dope.servlet;

import it.unipd.dei.webapp.dope.dao.*;
import it.unipd.dei.webapp.dope.filter.ProtectedResourceFilter;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.Repository;
import it.unipd.dei.webapp.dope.resource.LogContext;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

/**
 * Servlet responsible for displaying a list of user repositories and handling
 * repository-related actions, such as adding and deleting repositories.
 *
 * This servlet processes both GET and POST requests to:
 * <ul>
 *   <li>Display a list of repositories associated with the logged-in user.</li>
 *   <li>Display images from a specific repository if requested.</li>
 *   <li>Handle repository-related actions like adding or deleting repositories.</li>
 * </ul>
 * If the user is not logged in, they are redirected to the login page.
 * If the repository actions are successful, appropriate messages are shown to the user.
 */
public class ListUserRepositoriesServlet extends AbstractDatabaseServlet {

    /**
     * Handles GET requests to retrieve and display the list of repositories
     * associated with the logged-in user. If the action is to list images from
     * a specific repository, it also retrieves the images from that repository.
     *
     * <p>This method checks if the user is logged in, retrieves the user's repository
     * list from the database, and forwards the request to the "logged_homepage.jsp" page.</p>
     *
     * @param req the HTTP request containing parameters for actions and repository IDs.
     * @param res the HTTP response used to display the repository list and images.
     * @throws ServletException if an error occurs while processing the request.
     * @throws IOException if an error occurs while writing the response.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String user = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);

        // Set LogContext
        LogContext.setIPAddress(req.getRemoteAddr());
        LogContext.setUser(user);
        LogContext.setAction(Actions.LIST_REPOSITORIES);
        LogContext.setResource(req.getRequestURI());
        String action = req.getParameter("action");

        if (user == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {

            HttpSession session = req.getSession();
            Integer userId = (Integer) new SearchUserDAO(getConnection(), -1, user, null, null, null, null).access().getOutputParam().get(0).getID_User();
            session.setAttribute("userId", userId);
            if ("getImages".equals(action)) {
                final Connection conn = getConnection();
                int repoId = Integer.parseInt(req.getParameter("repoId"));

                LogContext.setAction(Actions.LIST_IMAGES_FROM_REPOSITORIES);

                ListImagesFromRepositoryDAO imageDAO = new ListImagesFromRepositoryDAO(conn, repoId);
                imageDAO.access();
                List<Image> images = imageDAO.getOutputParam();

                req.setAttribute("images", images);
                req.setAttribute("expandedRepoId", repoId);
            }

            LogContext.setAction(Actions.LIST_REPOSITORIES);
            ListRepositoriesDAO repoDAO = new ListRepositoriesDAO(getConnection(), userId);
            repoDAO.access();
            List<Repository> repositories = repoDAO.getOutputParam();
            req.setAttribute("repositories", repositories);

            req.getRequestDispatcher("/jsp/logged_homepage.jsp").forward(req, res);

        } catch (Exception e) {
            throw new ServletException("Error loading page", e);
        } finally {
            // Clear LogContext
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }

    /**
     * Handles POST requests to add or delete repositories for the logged-in user.
     *
     * <p>This method processes the actions related to repository management, such as adding
     * new repositories and deleting existing ones. It also checks if the user is the owner
     * of a repository before allowing deletion. After performing the action, it updates
     * the repository list and forwards the request to the "logged_homepage.jsp" page.</p>
     *
     * @param req the HTTP request containing the repository name and action parameters.
     * @param res the HTTP response used to display the outcome of the repository action.
     * @throws ServletException if an error occurs while processing the request.
     * @throws IOException if an error occurs while writing the response.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        // Set LogContext
        LogContext.setIPAddress(req.getRemoteAddr());
        String user = (String) req.getSession().getAttribute(ProtectedResourceFilter.USER_ATTRIBUTE);
        LogContext.setUser(user);
        LogContext.setResource(req.getRequestURI());
        Integer repoId = null;
        Integer imgId = null;

        try {
            String toastMessage = "";
            String action = req.getParameter("action");
            String visibility = req.getParameter("visibility");
            if (req.getParameter("repoId") != null) {
                repoId = Integer.parseInt((req.getParameter("repoId")));
            }
            if (req.getParameter("imgId") != null) {
                imgId = Integer.parseInt((req.getParameter("imgId")));
            }
            String repoName = req.getParameter("repo");
            HttpSession session = req.getSession();
            if (user == null) {
                res.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            int userId = new SearchUserDAO(getConnection(), -1, user, null, null, null, null).access().getOutputParam().get(0).getID_User();
            session.setAttribute("userId", userId);
            if ("deleteRepository".equals(action)) {
                if (repoId != null) {
                    GetUserResourceAccessDAO isOwnerDAO = new GetUserResourceAccessDAO(getConnection(), repoId, userId, false);
                    isOwnerDAO.access();
                    if (isOwnerDAO.getOutputParam() == 2) {
                        LogContext.setAction(Actions.DELETE_REPOSITORY);
                        Repository repo = new GetRepositoryDAO(getConnection(), repoId).access().getOutputParam();
                        String deletedRepoName = repo.getName();
                        DeleteRepositoryDAO deleteRepoDAO = new DeleteRepositoryDAO(getConnection(), repoId);
                        deleteRepoDAO.access();
                        toastMessage = "Repository " + deletedRepoName + " successfully deleted.";
                    } else {
                        toastMessage = "You are not the owner of this repository. Deletion not allowed.";
                    }
                }
            }

            else if ("deleteImage".equals(action)) {
                if (imgId != null && repoId != null) {
                    GetUserResourceAccessDAO isOwnerDAO = new GetUserResourceAccessDAO(getConnection(), imgId, userId, true);
                    isOwnerDAO.access();
                    if (isOwnerDAO.getOutputParam() == 2) {
                        LogContext.setAction(Actions.DELETE_IMAGE);
                        DeleteImageDAO deleteImageDAO = new DeleteImageDAO(getConnection(), imgId);
                        deleteImageDAO.access();
                        toastMessage = "Image successfully deleted.";
                    } else {
                        toastMessage = "You are not the owner of this image. Deletion not allowed.";
                    }
                }
            }

             else {
                if (repoName != null && !repoName.trim().isEmpty()) {
                    Connection connect = getConnection();
                    ListRepositoriesDAO repoDAO = new ListRepositoriesDAO(connect, userId);
                    repoDAO.access();
                    List<Repository> repositories = repoDAO.getOutputParam();

                    boolean exists = repositories.stream().anyMatch(r -> r.getName().equals(repoName));

                    if (exists) {
                        toastMessage = "A repository with this name already exists.";
                    } else {
                        LogContext.setAction(Actions.INSERT_REPOSITORY);
                        Repository newRepo = new Repository((UUID.randomUUID().toString()).hashCode(), repoName, visibility, userId);
                        new InsertRepositoryDAO(getConnection(), newRepo).access();
                        toastMessage = "Repository successfully created.";
                    }
                }
            }

            ListRepositoriesDAO repoDAO = new ListRepositoriesDAO(getConnection(), userId);
            repoDAO.access();
            List<Repository> repositories = repoDAO.getOutputParam();

            if (repoId != null && imgId != null) {
                ListImagesFromRepositoryDAO imageDAO = new ListImagesFromRepositoryDAO(getConnection(), repoId);
                imageDAO.access();
                List<Image> images = imageDAO.getOutputParam();
                req.setAttribute("images", images);
            }

            req.setAttribute("toastMessage", toastMessage);
            req.setAttribute("repositories", repositories);

            req.getRequestDispatcher("/jsp/logged_homepage.jsp").forward(req, res);

        } catch (Exception e) {
            throw new ServletException("Error handling repository/image action", e);
        } finally {
            LogContext.removeIPAddress();
            LogContext.removeUser();
            LogContext.removeAction();
            LogContext.removeResource();
        }
    }
}