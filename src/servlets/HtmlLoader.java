package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import server.RequestParser.RequestInfo;

public class HtmlLoader implements Servlet {
    private static final String APP_PREFIX = "/app/";

    private final Path htmlRoot;

    public HtmlLoader(String htmlFolder) {
        this.htmlRoot = Paths.get(htmlFolder).toAbsolutePath().normalize();
    }

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        if (ri == null || toClient == null) {
            return;
        }

        String uri = ri.getUri();
        System.out.println("[HtmlLoader] Requested URI: " + uri);
        if (uri == null) {
            System.out.println("[HtmlLoader] Resolved file path: <none>");
            System.out.println("[HtmlLoader] File exists: false");
            writeNotFound(toClient, "Requested file was not found.");
            return;
        }

        String path = extractPath(uri);
        if (!path.startsWith(APP_PREFIX) || path.contains("..")) {
            System.out.println("[HtmlLoader] Resolved file path: <rejected>");
            System.out.println("[HtmlLoader] File exists: false");
            writeNotFound(toClient, "Requested file was not found.");
            return;
        }

        String relativeFile = path.substring(APP_PREFIX.length());
        if (relativeFile.isEmpty()) {
            relativeFile = "index.html";
        }

        Path requestedFile = htmlRoot.resolve(relativeFile).normalize();
        boolean exists = requestedFile.startsWith(htmlRoot) && Files.isRegularFile(requestedFile);
        System.out.println("[HtmlLoader] Resolved file path: " + requestedFile);
        System.out.println("[HtmlLoader] File exists: " + exists);
        if (!exists) {
            writeNotFound(toClient, "Requested file was not found.");
            return;
        }

        byte[] body = Files.readAllBytes(requestedFile);
        writeResponse(toClient, "200 OK", body);
    }

    @Override
    public void close() throws IOException {
    }

    private String extractPath(String uri) {
        int queryIndex = uri.indexOf('?');
        return queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
    }

    private void writeNotFound(OutputStream toClient, String message) throws IOException {
        String body = "<!DOCTYPE html><html><body><h1>File not found</h1><p>"
                + escapeHtml(message)
                + "</p></body></html>";
        writeResponse(toClient, "404 Not Found", body.getBytes(StandardCharsets.UTF_8));
    }

    private void writeResponse(OutputStream toClient, String status, byte[] body) throws IOException {
        System.out.println("[HtmlLoader] Response body byte length: " + body.length);
        String headers = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        toClient.write(headers.getBytes(StandardCharsets.UTF_8));
        toClient.write(body);
    }

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
