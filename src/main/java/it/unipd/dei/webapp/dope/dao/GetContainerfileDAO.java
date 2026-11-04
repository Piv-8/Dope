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
public class GetContainerfileDAO extends AbstractDAO<Containerfile> {

    /** The SQL query used to retrieve a containerfile by its name. */
    private static final String STATEMENT = "SELECT \"name\", \"from\", \"cmd\", \"json_obj\", \"image_built\" FROM dope.\"containerfile\" WHERE \"name\" = ?";

    /** The name of the containerfile to be retrieved. */
    private final String name;

    /**
     * Creates a new {@code GetContainerfileDAO} object.
     *
     * @param con  the connection to be used for accessing the database.
     * @param name the name of the containerfile to be retrieved.
     */
    public GetContainerfileDAO(final Connection con, final String name){
        super(con);
        this.name = name;
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
        LogContext.setAction(Actions.GET_CONTAINERFILE);
        LogContext.setResource("Containerfile NAME:" + name);

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        Containerfile cf = null;

        try{
            pstmt = con.prepareStatement(STATEMENT);
            pstmt.setString(1,name);
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
                LOGGER.info("ContainerFile {} not found.", name);
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
