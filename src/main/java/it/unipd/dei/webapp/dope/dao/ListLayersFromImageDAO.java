package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Image;
import it.unipd.dei.webapp.dope.resource.Layer;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves a list of {@link Layer} objects associated with a given {@link Image} from the database.
 *
 * <p>This DAO performs a query on the {@code layer} table to retrieve layers associated with a specific image.
 * The layers are identified by the image's ID, and their details are returned as a list of {@link Layer} objects.</p>
 */
public class ListLayersFromImageDAO extends AbstractDAO<List<Layer>> {

    /** The SQL query used to retrieve layers associated with the given image. */
    private static final String STATEMENT = "SELECT l.* FROM dope.\"layer\" l JOIN dope.\"image\" i ON l.\"image_owner\" = i.\"id_image\" WHERE i.\"id_image\"= ?";

    /** The {@link Image} object whose associated layers are to be retrieved. */
    private final Image image;

    /**
     * Creates a new {@code ListLayersFromImageDAO} object.
     *
     * @param con   the connection to be used for accessing the database.
     * @param image the image whose layers are to be retrieved.
     */
    public ListLayersFromImageDAO(Connection con, final Image image) {
        super(con);
        this.image = image;
    }

    /**
     * Executes the query to retrieve the list of layers associated with the given image.
     *
     * <p>If the query is successful, a list of {@link Layer} objects is populated and stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}. The details of the retrieved layers are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.LIST_LAYERS_FROM_IMAGE);
        LogContext.setResource("Layers of Image ID:" + image.getID_image());

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Layer l = null;
        List<Layer> layers = new ArrayList<>();

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1, image.getID_image());
            rs = pstmt.executeQuery();

            while (rs.next()) {
                l = new Layer(
                        rs.getString("id_layer"),
                        rs.getInt("size"),
                        rs.getInt("image_owner")
                );
                layers.add(l);
            }
            if(!layers.isEmpty())
                LOGGER.info("List of Layers of the Image {} successfully retrieved. Retrieved {} Layers", image.getID_image(), layers.size());
            else
                LOGGER.info("Layers were not read from DB.");
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

        outputParam = layers;
    }
}

