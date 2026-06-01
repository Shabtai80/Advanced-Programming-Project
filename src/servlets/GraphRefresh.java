package servlets;

import configs.Graph;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import server.RequestParser.RequestInfo;
import views.HtmlGraphWriter;

public class GraphRefresh implements Servlet {
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        if (toClient == null) {
            return;
        }

        Graph graph = GraphState.getGraph();
        if (graph == null) {
            String body = "<!DOCTYPE html><html><body><h1>No graph loaded yet.</h1>"
                    + "<p>Please deploy a configuration first.</p></body></html>";
            writeResponse(toClient, body.getBytes(StandardCharsets.UTF_8));
            return;
        }

        List<String> htmlLines = HtmlGraphWriter.getGraphHTML(graph);
        String html = String.join(System.lineSeparator(), htmlLines);
        writeResponse(toClient, html.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
    }

    private void writeResponse(OutputStream toClient, byte[] body) throws IOException {
        String headers = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        toClient.write(headers.getBytes(StandardCharsets.UTF_8));
        toClient.write(body);
    }
}
