package views;

import configs.Graph;
import configs.Node;
import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class HtmlGraphWriter {
    private static final int MIN_SVG_WIDTH = 850;
    private static final int MIN_SVG_HEIGHT = 500;
    private static final int RECT_WIDTH = 150;
    private static final int RECT_HEIGHT = 64;
    private static final int CIRCLE_RADIUS = 42;
    private static final int GRAPH_PADDING = 80;
    private static final int TOPIC_X = 140;
    private static final int AGENT_X = 620;
    private static final int ROW_GAP = 105;
    private static final int TOP_START_Y = 90;

    public static List<String> getGraphHTML(Graph graph) {
        String template = loadTemplate();
        GraphLayout layout = buildLayout(graph);
        String html = template
                .replace("{{SVG_WIDTH}}", String.valueOf(layout.svgWidth))
                .replace("{{SVG_HEIGHT}}", String.valueOf(layout.svgHeight))
                .replace("{{EDGES}}", layout.edgesSvg)
                .replace("{{NODES}}", layout.nodesSvg);
        return Arrays.asList(html.split("\\R", -1));
    }

    private static String loadTemplate() {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("html_files", "graph.html"));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load html_files/graph.html", e);
        }
    }

    private static GraphLayout buildLayout(Graph graph) {
        List<Node> nodes = new ArrayList<>(graph);
        nodes.sort(Comparator.comparing(Node::getDisplayName, Comparator.nullsFirst(String::compareTo))
                .thenComparing(Node::getName, Comparator.nullsFirst(String::compareTo)));

        LayoutData layoutData = buildTwoColumnLayout(nodes);

        StringBuilder edgesSvg = new StringBuilder();
        StringBuilder nodesSvg = new StringBuilder();

        for (Node node : nodes) {
            Point from = layoutData.positions.get(node);
            for (Node edge : node.getEdges()) {
                Point to = layoutData.positions.get(edge);
                if (from != null && to != null) {
                    edgesSvg.append(buildEdgeSvg(node, edge, from, to)).append('\n');
                }
            }
        }

        for (Node node : nodes) {
            Point point = layoutData.positions.get(node);
            if (point != null) {
                nodesSvg.append(buildNodeSvg(node, point)).append('\n');
            }
        }

        return new GraphLayout(nodesSvg.toString(), edgesSvg.toString(), layoutData.svgWidth, layoutData.svgHeight);
    }

    private static LayoutData buildTwoColumnLayout(List<Node> nodes) {
        List<Node> topics = filterNodesByKind(nodes, Node.Kind.TOPIC);
        List<Node> agents = filterNodesByKind(nodes, Node.Kind.AGENT);
        int maxCount = Math.max(topics.size(), agents.size());
        int svgWidth = Math.max(MIN_SVG_WIDTH, AGENT_X + RECT_WIDTH + GRAPH_PADDING + 80);
        int svgHeight = Math.max(MIN_SVG_HEIGHT, TOP_START_Y + Math.max(0, maxCount - 1) * ROW_GAP + GRAPH_PADDING + 80);
        Map<Node, Point> positions = new LinkedHashMap<>();

        placeNodesInColumn(topics, positions, TOPIC_X, svgHeight);
        placeNodesInColumn(agents, positions, AGENT_X, svgHeight);

        return new LayoutData(positions, svgWidth, svgHeight);
    }

    private static List<Node> filterNodesByKind(List<Node> nodes, Node.Kind kind) {
        List<Node> filteredNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node.getKind() == kind) {
                filteredNodes.add(node);
            }
        }
        return filteredNodes;
    }

    private static void placeNodesInColumn(List<Node> nodes, Map<Node, Point> positions, int x, int svgHeight) {
        if (nodes.isEmpty()) {
            return;
        }

        int columnHeight = Math.max(0, nodes.size() - 1) * ROW_GAP;
        int startY = Math.max(TOP_START_Y, (svgHeight - columnHeight) / 2);
        for (int i = 0; i < nodes.size(); i++) {
            positions.put(nodes.get(i), new Point(x, startY + i * ROW_GAP));
        }
    }

    private static String buildEdgeSvg(Node fromNode, Node toNode, Point from, Point to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0.0) {
            length = 1.0;
        }

        Point startPoint = edgeAnchor(fromNode, from, dx / length, dy / length);
        Point endPoint = edgeAnchor(toNode, to, -dx / length, -dy / length);

        return "<line class=\"edge\" x1=\"" + startPoint.x + "\" y1=\"" + startPoint.y
                + "\" x2=\"" + endPoint.x + "\" y2=\"" + endPoint.y + "\"></line>";
    }

    private static Point edgeAnchor(Node node, Point center, double directionX, double directionY) {
        if (node.getKind() == Node.Kind.TOPIC) {
            double scaleX = directionX == 0.0 ? Double.POSITIVE_INFINITY : (RECT_WIDTH / 2.0) / Math.abs(directionX);
            double scaleY = directionY == 0.0 ? Double.POSITIVE_INFINITY : (RECT_HEIGHT / 2.0) / Math.abs(directionY);
            double scale = Math.min(scaleX, scaleY);
            return new Point(
                    (int) Math.round(center.x + directionX * scale),
                    (int) Math.round(center.y + directionY * scale));
        }
        return new Point(
                (int) Math.round(center.x + directionX * CIRCLE_RADIUS),
                (int) Math.round(center.y + directionY * CIRCLE_RADIUS));
    }

    private static String buildNodeSvg(Node node, Point point) {
        String label = escapeHtml(getDisplayName(node));
        if (node.getKind() == Node.Kind.TOPIC) {
            int rectX = point.x - RECT_WIDTH / 2;
            int rectY = point.y - RECT_HEIGHT / 2;
            String valueText = buildTopicValueSvg(node, point, rectY);
            return "<g>"
                    + valueText
                    + "<rect class=\"topic\" x=\"" + rectX + "\" y=\"" + rectY
                    + "\" width=\"" + RECT_WIDTH + "\" height=\"" + RECT_HEIGHT
                    + "\" rx=\"10\" ry=\"10\"></rect>"
                    + "<text class=\"node-label\" x=\"" + point.x + "\" y=\"" + point.y + "\">"
                    + label + "</text>"
                    + "</g>";
        }

        return "<g>"
                + "<circle class=\"agent\" cx=\"" + point.x + "\" cy=\"" + point.y
                + "\" r=\"" + CIRCLE_RADIUS + "\"></circle>"
                + "<text class=\"node-label\" x=\"" + point.x + "\" y=\"" + point.y + "\">"
                + label + "</text>"
                + "</g>";
    }

    private static String getDisplayName(Node node) {
        return node.getDisplayName() == null ? "" : node.getDisplayName();
    }

    private static String buildTopicValueSvg(Node node, Point point, int rectY) {
        String topicName = getDisplayName(node);
        System.out.println("[HtmlGraphWriter] Topic node displayName=" + topicName + ", internalName=" + node.getName());
        if (topicName.isEmpty()) {
            System.out.println("[HtmlGraphWriter] Topic lookup skipped: empty display name.");
            return "";
        }

        Topic topic = findExistingTopic(topicName);
        System.out.println("[HtmlGraphWriter] Topic found=" + (topic != null));
        if (topic == null) {
            System.out.println("[HtmlGraphWriter] Extracted last value/message=<topic not found>");
            return "";
        }

        Message lastMessage = topic.getLastMessage();
        if (lastMessage == null || lastMessage.asText == null || lastMessage.asText.isEmpty()) {
            System.out.println("[HtmlGraphWriter] Extracted last value/message=<null or empty>");
            return "";
        }

        System.out.println("[HtmlGraphWriter] Extracted last value/message=" + lastMessage.asText);

        return "<text class=\"topic-value\" x=\"" + point.x + "\" y=\"" + (rectY - 10) + "\">"
                + escapeHtml(lastMessage.asText)
                + "</text>";
    }

    private static Topic findExistingTopic(String topicName) {
        for (Topic topic : TopicManagerSingleton.get().getTopics()) {
            if (topic.name.equals(topicName)) {
                return topic;
            }
        }
        return null;
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static class GraphLayout {
        private final String nodesSvg;
        private final String edgesSvg;
        private final int svgWidth;
        private final int svgHeight;

        private GraphLayout(String nodesSvg, String edgesSvg, int svgWidth, int svgHeight) {
            this.nodesSvg = nodesSvg;
            this.edgesSvg = edgesSvg;
            this.svgWidth = svgWidth;
            this.svgHeight = svgHeight;
        }
    }

    private static class LayoutData {
        private final Map<Node, Point> positions;
        private final int svgWidth;
        private final int svgHeight;

        private LayoutData(Map<Node, Point> positions, int svgWidth, int svgHeight) {
            this.positions = positions;
            this.svgWidth = svgWidth;
            this.svgHeight = svgHeight;
        }
    }

    private static class Point {
        private final int x;
        private final int y;

        private Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
