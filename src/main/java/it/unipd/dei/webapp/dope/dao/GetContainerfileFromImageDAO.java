package it.unipd.dei.webapp.dope.dao;

import it.unipd.dei.webapp.dope.resource.Actions;
import it.unipd.dei.webapp.dope.resource.Containerfile;
import it.unipd.dei.webapp.dope.resource.LogContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Retrieves a {@link Containerfile} from the database by its name.
 *
 * <p>This DAO performs a query on the {@code containerfile} table using the provided name as a key.
 * If a match is found, it returns the corresponding {@link Containerfile} object; otherwise, it returns {@code null}.</p>
 */
public class GetContainerfileFromImageDAO extends AbstractDAO<Containerfile> {

    /** The SQL query used to retrieve a containerfile by its name. */
    private static final String STATEMENT = "SELECT \"name\", \"from\", \"cmd\", \"json_obj\", \"image_built\" FROM dope.\"containerfile\" WHERE \"image_built\" = ?";

    /** The name of the containerfile to be retrieved. */
    private final int id_image;

    /**
     * Creates a new {@code GetContainerfileDAO} object.
     *
     * @param con  the connection to be used for accessing the database.
     * @param id_image the name of the containerfile to be retrieved.
     */
    public GetContainerfileFromImageDAO(final Connection con, final int id_image){
        super(con);
        this.id_image = id_image;
    }

    /**
     * Executes the query to retrieve the containerfile by its name.
     *
     * <p>If a containerfile with the given name is found, it is stored in the {@code outputParam} field;
     * otherwise, {@code outputParam} remains {@code null}.</p>
     *
     * @throws Exception if a database access error occurs.
     */
    @Override
    protected void doAccess() throws Exception {
    LogContext.setAction(Actions.GET_CONTAINERFILE_FROM_IMAGE);
        LogContext.setResource("Containerfile of Image ID:" + id_image);

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        Containerfile cf = null;

        try{
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setInt(1,id_image);
            rs = pstmt.executeQuery();

            if(rs.next()){
                cf = new Containerfile(
                        rs.getString("name"),
                        rs.getString("from"),
                        rs.getString("cmd"),
                        rs.getString("json_obj"),
                        rs.getInt("image_built")
                );
                LOGGER.info("ContainerFile {} successfully read from DB.", cf.getName());
            }
            else
                LOGGER.info("ContainerFile of Image {} not found.", id_image);
        }finally{
            if(rs != null){
                rs.close();
            }
            if(pstmt != null){
                pstmt.close();
            }
        }

        LogContext.removeAction();
        LogContext.removeResource();

        outputParam = cf;
    }
}
