package configs;

import graph.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node {
    public enum Kind {
        TOPIC,
        AGENT
    }

    private String name;
    private String displayName;
    private Kind kind;
    private List<Node> edges;
    private Message message;

    public Node(String name) {
        this(name, name, null);
    }

    public Node(String name, String displayName, Kind kind) {
        this.name = name;
        this.displayName = displayName;
        this.kind = kind;
        this.edges = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public List<Node> getEdges() {
        return edges;
    }

    public void setEdges(List<Node> edges) {
        this.edges = edges;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void addEdge(Node n) {
        edges.add(n);
    }

    public boolean hasCycles() {
        return hasCycles(new HashSet<>(), new HashSet<>());
    }

    private boolean hasCycles(Set<Node> visited, Set<Node> recursionStack) {
        if (recursionStack.contains(this)) {
            return true;
        }
        if (visited.contains(this)) {
            return false;
        }

        visited.add(this);
        recursionStack.add(this);

        for (Node edge : edges) {
            if (edge.hasCycles(visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(this);
        return false;
    }
}
