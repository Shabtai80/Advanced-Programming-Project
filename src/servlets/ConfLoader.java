package servlets;

import configs.GenericConfig;
import configs.Graph;
import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import server.RequestParser.RequestInfo;
import views.HtmlGraphWriter;

/**
 * Accepts uploaded configuration files, recreates the runtime graph from the
 * uploaded definition, and returns the rendered graph view.
 * This servlet is the bridge between the reusable HTTP API and the project's
 * agent/topic configuration model.
 */
public class ConfLoader implements Servlet {
    private static final String UPLOAD_DIR = "uploaded_configs";

    private final Path uploadRoot;
    private GenericConfig currentConfig;

    /**
     * Creates a configuration upload servlet that stores uploaded files under
     * the project's upload directory.
     */
    public ConfLoader() {
        this.uploadRoot = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
    }

    /**
     * Processes a multipart configuration upload, rebuilds the active graph,
     * preserves existing topic values when possible, and writes the generated
     * graph HTML back to the client.
     *
     * @param ri the parsed upload request
     * @param toClient the output stream connected to the client
     * @throws IOException if the uploaded file cannot be stored or the response cannot be written
     */
    @Override
    public synchronized void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        if (ri == null || toClient == null) {
            return;
        }

        try {
            UploadedConfig uploadedConfig = extractUploadedConfig(ri);
            Path savedFile = saveUploadedConfig(uploadedConfig);
            Map<String, Message> preservedTopicValues = snapshotTopicValues();

            if (currentConfig != null) {
                currentConfig.close();
            }
            TopicManagerSingleton.get().clear();

            GenericConfig config = new GenericConfig();
            config.setConfFile(savedFile.toString());
            config.create();
            currentConfig = config;
            restoreTopicValues(preservedTopicValues);

            Graph graph = new Graph();
            graph.createFromTopics();
            GraphState.setGraph(graph);

            List<String> htmlLines = HtmlGraphWriter.getGraphHTML(graph);
            String html = String.join(System.lineSeparator(), htmlLines);
            writeResponse(toClient, "200 OK", html.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IllegalStateException e) {
            writeErrorResponse(toClient, e.getMessage());
        }
    }

    /**
     * Closes the currently active configuration and releases its resources.
     *
     * @throws IOException if closing the active configuration fails
     */
    @Override
    public synchronized void close() throws IOException {
        if (currentConfig != null) {
            currentConfig.close();
            currentConfig = null;
        }
    }

    private UploadedConfig extractUploadedConfig(RequestInfo ri) {
        String contentType = getHeaderIgnoreCase(ri.getHeaders(), "Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Upload request must use multipart/form-data.");
        }

        String boundary = extractBoundary(contentType);
        String body = new String(ri.getContent(), StandardCharsets.UTF_8);
        String delimiter = "--" + boundary;
        String[] parts = body.split(java.util.regex.Pattern.quote(delimiter));

        for (String part : parts) {
            String trimmedPart = part.trim();
            if (trimmedPart.isEmpty() || trimmedPart.equals("--")) {
                continue;
            }

            int headerEnd = part.indexOf("\r\n\r\n");
            int separatorLength = 4;
            if (headerEnd < 0) {
                headerEnd = part.indexOf("\n\n");
                separatorLength = 2;
            }

            if (headerEnd < 0) {
                continue;
            }

            String headersText = part.substring(0, headerEnd);
            String content = part.substring(headerEnd + separatorLength);
            String disposition = findHeader(headersText, "Content-Disposition");
            if (disposition == null || !disposition.contains("name=\"config\"")) {
                continue;
            }

            String filename = extractDispositionValue(disposition, "filename");
            if (filename == null || filename.isEmpty()) {
                throw new IllegalArgumentException("Uploaded configuration file is missing a filename.");
            }

            String cleanedContent = stripTrailingBoundaryNewlines(content);
            return new UploadedConfig(sanitizeFilename(filename), cleanedContent.getBytes(StandardCharsets.UTF_8));
        }

        throw new IllegalArgumentException("Could not find uploaded file field named config.");
    }

    private Path saveUploadedConfig(UploadedConfig uploadedConfig) throws IOException {
        Files.createDirectories(uploadRoot);
        Path savedFile = uploadRoot.resolve(uploadedConfig.fileName).normalize();
        if (!savedFile.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid uploaded filename.");
        }

        Files.write(savedFile, uploadedConfig.content);
        return savedFile;
    }

    private Map<String, Message> snapshotTopicValues() {
        Map<String, Message> values = new LinkedHashMap<>();
        for (Topic topic : TopicManagerSingleton.get().getTopics()) {
            Message lastMessage = topic.getLastMessage();
            if (lastMessage != null) {
                values.put(topic.name, lastMessage);
            }
        }
        return values;
    }

    private void restoreTopicValues(Map<String, Message> preservedTopicValues) {
        for (Map.Entry<String, Message> entry : preservedTopicValues.entrySet()) {
            TopicManagerSingleton.get().getTopic(entry.getKey()).setLastMessage(entry.getValue());
        }
    }

    private String extractBoundary(String contentType) {
        String[] parts = contentType.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("boundary=")) {
                String boundary = trimmed.substring("boundary=".length());
                if (boundary.startsWith("\"") && boundary.endsWith("\"") && boundary.length() >= 2) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                if (!boundary.isEmpty()) {
                    return boundary;
                }
            }
        }

        throw new IllegalArgumentException("Multipart boundary is missing.");
    }

    private String findHeader(String headersText, String headerName) {
        String[] lines = headersText.split("\\R");
        for (String line : lines) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }

            String currentName = line.substring(0, separatorIndex).trim();
            if (currentName.equalsIgnoreCase(headerName)) {
                return line.substring(separatorIndex + 1).trim();
            }
        }
        return null;
    }

    private String extractDispositionValue(String disposition, String key) {
        String token = key + "=\"";
        int start = disposition.indexOf(token);
        if (start < 0) {
            return null;
        }

        int valueStart = start + token.length();
        int valueEnd = disposition.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return null;
        }

        return disposition.substring(valueStart, valueEnd);
    }

    private String sanitizeFilename(String filename) {
        return Paths.get(filename).getFileName().toString();
    }

    private String stripTrailingBoundaryNewlines(String content) {
        String cleaned = content;
        while (cleaned.endsWith("\r\n")) {
            cleaned = cleaned.substring(0, cleaned.length() - 2);
        }
        while (cleaned.endsWith("\n")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private String getHeaderIgnoreCase(Map<String, String> headers, String headerName) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void writeErrorResponse(OutputStream toClient, String message) throws IOException {
        String body = "<!DOCTYPE html><html><body><h1>Upload failed</h1><p>"
                + escapeHtml(message == null ? "Unknown error." : message)
                + "</p></body></html>";
        writeResponse(toClient, "400 Bad Request", body.getBytes(StandardCharsets.UTF_8));
    }

    private void writeResponse(OutputStream toClient, String status, byte[] body) throws IOException {
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

    private static class UploadedConfig {
        private final String fileName;
        private final byte[] content;

        private UploadedConfig(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }
    }
}
