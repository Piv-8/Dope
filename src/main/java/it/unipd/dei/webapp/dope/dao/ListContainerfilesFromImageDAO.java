package it.unipd.dei.webapp.dope.dao;
import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Containerfile;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves a list of {@link Containerfile} objects associated with a given {@link Image} from the database.
 *
 * <p>This DAO performs a query on the {@code containerfile} table to retrieve containerfiles associated with a specific image.
 * The containerfiles are identified by the image's ID, and their details are returned as a list of {@link Containerfile} objects.</p>
 */
public class ListContainerfilesFromImageDAO extends AbstractDAO<List<Containerfile>> {

    /** The SQL query used to retrieve containerfiles associated with the given image. */
    private static final String STATEMENT = "SELECT c.* FROM dope.\"containerfile\" c JOIN dope.\"image\" i ON c.\"image_built\" = i.\"id_image\" WHERE i.\"id_image\"= ?";

    /** The {@link Image} object whose associated containerfiles are to be retrieved. */
    private final Image image;

    /**
     * Creates a new {@code ListContainerfilesFromImageDAO} object.
     *
     * @param con   the connection to be used for accessing the database.
     * @param image the image whose containerfiles are to be retrieved.
     */
    protected ListContainerfilesFromImageDAO(final Connection con, final Image image) {
        super(con);
        this.image = image;
    }

    /**
     * Executes the query to retrieve the list of containerfiles associated with the given image.
     *
     * <p>If the query is successful, a list of {@link Containerfile} objects is populated and stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}. The details of the retrieved containerfiles are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.LIST_CONTAINERS_FROM_IMAGE);
        LogContext.setResource("Container of Image ID:" + image.getID_image());


        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Containerfile cont = null;
        List<Containerfile> containerfile = new ArrayList<>();

        try {
            //prepare the query with the Image
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, image.getID_image());

            rs = pstmt.executeQuery();
            while (rs.next()) {
                cont = new Containerfile(
                        rs.getString("name"),
                        rs.getString("from"),
                        rs.getString("cmd"),
                        rs.getString("json_obj"),
                        rs.getInt("image_built")
                );
                containerfile.add(cont);
            }

            if(!containerfile.isEmpty())
                LOGGER.info("List of ContainerFile successfully retrieved from the Image. Retrieved {} ContainerFiles", containerfile.size());
            else
                LOGGER.info("There are no Containerfile for the Image {}.", image.getID_image());
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
        }

        LogContext.removeAction();
        LogContext.removeResource();

        this.outputParam = containerfile;
    }
}
