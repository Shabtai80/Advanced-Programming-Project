package server;

import servlets.Servlet;

/**
 * Defines the reusable contract for a lightweight HTTP server that dispatches
 * incoming requests to registered {@link Servlet} handlers.
 * Implementations are responsible for accepting client connections, mapping
 * HTTP methods and URI prefixes to servlets, and managing the server lifecycle.
 */
public interface HTTPServer extends Runnable {
    /**
     * Registers a servlet for the given HTTP method and URI prefix.
     *
     * @param httpCommand the HTTP method to associate with the servlet, such as {@code GET} or {@code POST}
     * @param uri the URI or URI prefix that should be routed to the servlet
     * @param s the servlet instance that should handle matching requests
     */
    public void addServlet(String httpCommand, String uri, Servlet s);

    /**
     * Removes a previously registered servlet mapping.
     *
     * @param httpCommand the HTTP method from which the mapping should be removed
     * @param uri the URI or URI prefix whose mapping should be removed
     */
    public void removeServlet(String httpCommand, String uri);

    /**
     * Starts the server so it can begin accepting client requests.
     */
    public void start();

    /**
     * Stops the server and releases any resources held by the implementation.
     */
    public void close();
}
