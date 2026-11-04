package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Layer;
import it.unipd.dei.webapp.dope.resource.LogContext;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Inserts a new {@link Layer} into the database.
 *
 * <p>This DAO performs an insert operation on the {@code layer} table.
 * It adds a new layer with the provided layer ID, size, and image owner.
 * The inserted layer's details are returned after the insertion.</p>
 */
public class InsertLayerDAO extends AbstractDAO<Layer> {

    /** The SQL query used to insert a new layer into the database. */
    private static final String STATEMENT = """
        INSERT INTO dope."layer"("id_layer", "size", "image_owner")
        VALUES (?, ?, ?)
        RETURNING *""";

    /** The {@link Layer} object to be inserted into the database. */
    private final Layer layer;

    /**
     * Creates a new {@code InsertLayerDAO} object.
     *
     * @param con   the connection to be used for accessing the database.
     * @param layer the layer object to insert.
     */
    public InsertLayerDAO(final Connection con, final Layer layer) {
        super(con);
        this.layer = layer;
    }

    /**
     * Executes the insert query to add the new layer to the database.
     *
     * <p>If the insertion is successful, the newly created layer object is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.
     * The details of the newly created layer are logged.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
        LogContext.setAction(Actions.INSERT_LAYER);
        LogContext.setResource("Layer ID:" + layer.getID_Layer());

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        Layer l = null;

        try {
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setString(1, layer.getID_Layer());
            pstmt.setInt(2, layer.getSize());
            pstmt.setInt(3, layer.getImage_owner());

            rs = pstmt.executeQuery(); // Performing the insert operation

            if (rs.next()) {
                l = new Layer(
                        rs.getString("id_layer"),
                        rs.getInt("size"),
                        rs.getInt("image_owner")
                );
                LOGGER.info("Layer ID {} successfully inserted.", l.getID_Layer());
            } else
            {
                LOGGER.error("Failed to insert Layer. No result returned.");
                throw new SQLException("Failed to insert the Layer.");
            }
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
