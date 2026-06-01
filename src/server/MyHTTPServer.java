package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import servlets.Servlet;

/**
 * Default {@link HTTPServer} implementation for the project.
 * This server listens on a TCP port, parses incoming HTTP requests, selects
 * the best matching registered servlet by HTTP method and URI prefix, and
 * handles requests concurrently by using a fixed-size thread pool.
 */
public class MyHTTPServer extends Thread implements HTTPServer {
    private final int port;
    private volatile boolean stop;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final Map<String, Servlet> getServlets;
    private final Map<String, Servlet> postServlets;
    private final Map<String, Servlet> deleteServlets;

    /**
     * Creates a server that listens on the supplied port and processes requests
     * with a fixed number of worker threads.
     *
     * @param port the TCP port on which the server should listen
     * @param nThreads the number of worker threads available for request handling
     */
    public MyHTTPServer(int port, int nThreads) {
        this.port = port;
        this.stop = false;
        this.threadPool = Executors.newFixedThreadPool(nThreads);
        this.getServlets = new ConcurrentHashMap<>();
        this.postServlets = new ConcurrentHashMap<>();
        this.deleteServlets = new ConcurrentHashMap<>();
    }

    /**
     * Registers a servlet for a specific HTTP method and URI prefix.
     * The longest matching registered URI prefix is used when dispatching requests.
     *
     * @param httpCommand the HTTP method to register, such as {@code GET}, {@code POST}, or {@code DELETE}
     * @param uri the URI prefix to associate with the servlet
     * @param s the servlet that should handle matching requests
     */
    @Override
    public void addServlet(String httpCommand, String uri, Servlet s) {
        Map<String, Servlet> servletMap = getServletMap(httpCommand);
        if (servletMap != null && uri != null && s != null) {
            servletMap.put(uri, s);
        }
    }

    /**
     * Removes a servlet mapping for a specific HTTP method and URI prefix.
     *
     * @param httpCommand the HTTP method whose mapping should be removed
     * @param uri the URI prefix whose mapping should be removed
     */
    @Override
    public void removeServlet(String httpCommand, String uri) {
        Map<String, Servlet> servletMap = getServletMap(httpCommand);
        if (servletMap != null && uri != null) {
            servletMap.remove(uri);
        }
    }

    /**
     * Opens the server socket and continuously accepts client connections until
     * the server is closed.
     */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000);

            while (!stop) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClient(clientSocket));
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    if (!stop) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            if (!stop) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the server, closes the listening socket, shuts down the worker pool,
     * and closes each registered servlet once.
     */
    @Override
    public void close() {
        stop = true;

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        threadPool.shutdown();

        Set<Servlet> closedServlets = new HashSet<>();
        closeServlets(getServlets, closedServlets);
        closeServlets(postServlets, closedServlets);
        closeServlets(deleteServlets, closedServlets);
    }

    private Map<String, Servlet> getServletMap(String httpCommand) {
        if (httpCommand == null) {
            return null;
        }

        if ("GET".equalsIgnoreCase(httpCommand)) {
            return getServlets;
        }

        if ("POST".equalsIgnoreCase(httpCommand)) {
            return postServlets;
        }

        if ("DELETE".equalsIgnoreCase(httpCommand)) {
            return deleteServlets;
        }

        return null;
    }

    private Servlet getMatchingServlet(String httpCommand, String requestUri) {
        Map<String, Servlet> servletMap = getServletMap(httpCommand);
        if (servletMap == null || requestUri == null) {
            return null;
        }

        String normalizedUri = requestUri;
        int queryIndex = normalizedUri.indexOf('?');
        if (queryIndex >= 0) {
            normalizedUri = normalizedUri.substring(0, queryIndex);
        }

        Servlet bestMatch = null;
        int bestLength = -1;
        for (Map.Entry<String, Servlet> entry : servletMap.entrySet()) {
            String registeredUri = entry.getKey();
            if (registeredUri == null) {
                continue;
            }

            if (normalizedUri.startsWith(registeredUri) && registeredUri.length() > bestLength) {
                bestMatch = entry.getValue();
                bestLength = registeredUri.length();
            }
        }

        return bestMatch;
    }

    private void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket) {
            System.out.println("[MyHTTPServer] Request received from " + socket.getRemoteSocketAddress());
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            RequestParser.RequestInfo ri = RequestParser.parseRequest(reader);
            if (ri == null) {
                System.out.println("[MyHTTPServer] Request parsing returned null.");
                return;
            }

            System.out.println("[MyHTTPServer] Requested URI: " + ri.getUri());
            OutputStream outputStream = socket.getOutputStream();
            Servlet servlet = getMatchingServlet(ri.getHttpCommand(), ri.getUri());
            if (servlet != null) {
                servlet.handle(ri, outputStream);
            } else {
                writeNotFound(outputStream);
            }
            outputStream.flush();
            System.out.println("[MyHTTPServer] Response flushed; socket will be closed.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("[MyHTTPServer] Socket closed.");
        }
    }

    private void writeNotFound(OutputStream outputStream) throws IOException {
        String response = "HTTP/1.1 404 Not Found\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: 0\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private void closeServlets(Map<String, Servlet> servletMap, Set<Servlet> closedServlets) {
        for (Servlet servlet : servletMap.values()) {
            if (servlet == null || !closedServlets.add(servlet)) {
                continue;
            }

            try {
                servlet.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
