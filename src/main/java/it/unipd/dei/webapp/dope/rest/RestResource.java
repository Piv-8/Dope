package it.unipd.dei.webapp.dope.rest;

import java.io.IOException;

/**
 * Represents a REST resource that can process incoming HTTP requests.
 * <p>This interface defines a contract for classes that handle HTTP requests and generate appropriate responses.</p>
 */
public interface RestResource {

    /**
     * Serves a REST request by processing it and generating an appropriate response.
     * <p>Implementing classes should handle specific HTTP methods (GET, POST, PUT, DELETE) and ensure the response
     * is properly generated and sent to the client. Any errors during the process should be handled appropriately.</p>
     *
     * @throws IOException if an error occurs during communication between the client and server.
     */
    void serve() throws IOException;

}
