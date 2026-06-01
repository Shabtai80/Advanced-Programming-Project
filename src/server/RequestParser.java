package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RequestParser {

    public static class RequestInfo {
        private final String httpCommand;
        private final String uri;
        private final String[] uriSegments;
        private final Map<String, String> parameters;
        private final Map<String, String> headers;
        private final byte[] content;

        public RequestInfo(String httpCommand, String uri, String[] uriSegments,
                Map<String, String> parameters, Map<String, String> headers, byte[] content) {
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments;
            this.parameters = parameters;
            this.headers = headers;
            this.content = content;
        }

        public String getHttpCommand() {
            return httpCommand;
        }

        public String getUri() {
            return uri;
        }

        public String[] getUriSegments() {
            return uriSegments;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public byte[] getContent() {
            return content;
        }
    }

    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {
        if (reader == null) {
            return null;
        }

        try {
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return null;
            }

            requestLine = requestLine.trim();
            if (requestLine.isEmpty()) {
                return null;
            }

            String[] requestParts = requestLine.split("\\s+");
            if (requestParts.length < 2) {
                return null;
            }

            String httpCommand = requestParts[0];
            String uri = requestParts[1];
            if (httpCommand.isEmpty() || uri.isEmpty()) {
                return null;
            }

            int contentLength = 0;
            Map<String, String> headers = new LinkedHashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()) != null) {
                if (headerLine.isEmpty()) {
                    break;
                }

                int separatorIndex = headerLine.indexOf(':');
                if (separatorIndex > 0) {
                    String headerName = headerLine.substring(0, separatorIndex).trim();
                    String headerValue = headerLine.substring(separatorIndex + 1).trim();
                    headers.put(headerName, headerValue);
                    if ("Content-Length".equalsIgnoreCase(headerName)) {
                        try {
                            contentLength = Integer.parseInt(headerValue);
                        } catch (NumberFormatException e) {
                            contentLength = 0;
                        }
                    }
                 }
            }

            Map<String, String> parameters = new LinkedHashMap<>(parseParameters(getQueryPart(uri)));
            byte[] rawContent = readRemainingContent(reader, contentLength);
            String contentType = getHeaderIgnoreCase(headers, "Content-Type");
            byte[] content;
            if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
                content = rawContent;
            } else {
                List<String> remainingLines = splitLines(rawContent);
                int contentStartIndex = parseAdditionalParameters(remainingLines, parameters);
                content = readContent(remainingLines, contentStartIndex);
            }
            String[] uriSegments = parseUriSegments(uri);

            return new RequestInfo(httpCommand, uri, uriSegments, parameters, headers, content);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String[] parseUriSegments(String uri) {
        String path = getPathPart(uri);
        path = trimSlashes(path);
        if (path.isEmpty()) {
            return new String[0];
        }

        String[] rawSegments = path.split("/");
        List<String> segments = new ArrayList<>();
        for (String segment : rawSegments) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        return segments.toArray(new String[0]);
    }

    private static String getPathPart(String uri) {
        int queryIndex = uri.indexOf('?');
        return queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
    }

    private static String getQueryPart(String uri) {
        int queryIndex = uri.indexOf('?');
        if (queryIndex < 0 || queryIndex + 1 >= uri.length()) {
            return "";
        }
        return uri.substring(queryIndex + 1);
    }

    private static String trimSlashes(String path) {
        int start = 0;
        int end = path.length();

        while (start < end && path.charAt(start) == '/') {
            start++;
        }

        while (end > start && path.charAt(end - 1) == '/') {
            end--;
        }

        return path.substring(start, end);
    }

    private static Map<String, String> parseParameters(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> parameters = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isEmpty()) {
                continue;
            }

            int equalsIndex = pair.indexOf('=');
            if (equalsIndex < 0) {
                parameters.put(pair, "");
                continue;
            }

            String key = pair.substring(0, equalsIndex);
            String value = pair.substring(equalsIndex + 1);
            if (!key.isEmpty()) {
                parameters.put(key, value);
            }
        }

        return parameters;
    }

    private static byte[] readRemainingContent(BufferedReader reader, int contentLength) throws IOException {
        if (contentLength <= 0) {
            return new byte[0];
        }

        char[] buffer = new char[contentLength];
        int offset = 0;
        while (offset < contentLength) {
            int read = reader.read(buffer, offset, contentLength - offset);
            if (read < 0) {
                break;
            }
            offset += read;
        }

        return new String(buffer, 0, offset).getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> splitLines(byte[] rawContent) {
        List<String> lines = new ArrayList<>();
        if (rawContent.length == 0) {
            return lines;
        }

        String body = new String(rawContent, StandardCharsets.UTF_8);
        Collections.addAll(lines, body.split("\\R", -1));
        return lines;
    }

    private static int parseAdditionalParameters(List<String> remainingLines, Map<String, String> parameters) {
        int index = 0;
        while (index < remainingLines.size()) {
            String line = remainingLines.get(index);
            if (line.isEmpty()) {
                return index + 1;
            }

            int equalsIndex = line.indexOf('=');
            if (equalsIndex >= 0) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                if (!key.isEmpty()) {
                    parameters.put(key, stripQuotes(value));
                }
            }
            index++;
        }

        return index;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static byte[] readContent(List<String> remainingLines, int startIndex) {
        if (startIndex >= remainingLines.size()) {
            return new byte[0];
        }

        StringBuilder contentBuilder = new StringBuilder();
        for (int i = startIndex; i < remainingLines.size(); i++) {
            String line = remainingLines.get(i);
            if (line.isEmpty()) {
                break;
            }

            contentBuilder.append(line).append('\n');
        }

        return contentBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String getHeaderIgnoreCase(Map<String, String> headers, String headerName) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
