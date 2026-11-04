package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Layer;
import it.unipd.dei.webapp.dope.resource.LogContext;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Retrieves a {@link Layer} from the database by its unique identifier.
 *
 * <p>This DAO performs a query on the {@code layer} table using the provided layer ID.
 * If a layer is found, it returns the corresponding {@link Layer} object; otherwise, it returns {@code null}.</p>
 */
public class GetLayerDAO extends AbstractDAO<Layer> {

    /** The SQL query used to retrieve a layer by its ID. */
    private static final String STATEMENT = """
        SELECT "id_layer", "size", "image_owner"
        FROM dope."layer"
        WHERE "id_layer" = ?""";

    /** The ID of the layer to be retrieved. */
    private final String id_layer;

    /**
     * Creates a new {@code GetLayerDAO} object.
     *
     * @param con       the connection to be used for accessing the database.
     * @param id_layer  the ID of the layer to retrieve.
     */
    public GetLayerDAO(final Connection con, final String id_layer) {
        super(con);
        this.id_layer = id_layer;
    }

    /**
     * Executes the query to retrieve the layer with the specified ID.
     *
     * <p>If a layer with the given ID is found, it is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.GET_LAYER);
        LogContext.setResource("Layer ID:" + id_layer);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Layer l = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setString(1, id_layer);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                l = new Layer(
                        rs.getString("id_layer"),
                        rs.getInt("size"),
                        rs.getInt("image_owner")
                );
                LOGGER.info("Layer ID {} successfully retrieved.", l.getID_Layer());

            }
            else
                LOGGER.info("Layer ID {} not found.", id_layer);
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

        outputParam = l;
    }
}


