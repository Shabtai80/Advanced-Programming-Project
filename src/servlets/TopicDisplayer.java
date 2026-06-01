package servlets;

import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import server.RequestParser.RequestInfo;

/**
 * Displays the current set of topics and their latest published values as an HTML table.
 * The servlet can also publish a new message to a topic when the request supplies
 * the appropriate parameters.
 */
public class TopicDisplayer implements Servlet {
    /**
     * Creates a topic display servlet.
     */
    public TopicDisplayer() {
    }

    /**
     * Optionally publishes a message to a topic and then renders the current topic
     * state as an HTML table.
     *
     * @param ri the parsed request information
     * @param toClient the output stream connected to the client
     * @throws IOException if writing the response fails
     */
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        if (ri == null || toClient == null) {
            return;
        }

        String topicName = ri.getParameters().get("topic");
        String messageText = ri.getParameters().get("message");

        if (topicName != null && !topicName.isEmpty() && messageText != null) {
            TopicManagerSingleton.get().getTopic(topicName).publish(new Message(messageText));
        }

        byte[] body = buildHtmlTable().getBytes(StandardCharsets.UTF_8);
        writeResponse(toClient, body);
    }

    /**
     * Closes the servlet.
     * This implementation does not hold external resources, so closing is a no-op.
     *
     * @throws IOException never thrown during normal operation
     */
    @Override
    public void close() throws IOException {
    }

    private String buildHtmlTable() {
        List<Topic> topics = new ArrayList<>(TopicManagerSingleton.get().getTopics());
        topics.sort(Comparator.comparing(topic -> topic.name));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>Topics</title>");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;margin:0;padding:16px;background:#fff;color:#1f2933;}");
        html.append("table{border-collapse:collapse;width:100%;}");
        html.append("th,td{border:1px solid #d9e2ec;padding:8px 10px;text-align:left;}");
        html.append("th{background:#eef2f7;}");
        html.append("tr:nth-child(even){background:#f8fafc;}");
        html.append("</style></head><body>");
        html.append("<h1>Current Topic Values</h1>");
        html.append("<table>");
        html.append("<tr><th>Topic name</th><th>Last value/message</th></tr>");

        for (Topic topic : topics) {
            html.append("<tr><td>")
                    .append(escapeHtml(topic.name))
                    .append("</td><td>")
                    .append(escapeHtml(formatMessage(topic.getLastMessage())))
                    .append("</td></tr>");
        }

        html.append("</table></body></html>");
        return html.toString();
    }

    private String formatMessage(Message message) {
        return message == null ? "" : message.asText;
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

    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
