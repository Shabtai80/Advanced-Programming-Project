package servlets;

import java.io.IOException;
import java.io.OutputStream;
import server.RequestParser.RequestInfo;

/**
 * Defines the minimal servlet contract used by the project's HTTP server.
 * A servlet receives parsed request data together with the client output stream
 * and is responsible for writing a complete HTTP response.
 */
public interface Servlet {
    /**
     * Handles a single HTTP request and writes the response to the client stream.
     *
     * @param ri the parsed request information
     * @param toClient the output stream connected to the client
     * @throws IOException if writing the response fails
     */
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;

    /**
     * Releases any resources owned by the servlet.
     *
     * @throws IOException if the servlet cannot be closed cleanly
     */
    void close() throws IOException;
}
