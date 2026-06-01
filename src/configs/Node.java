package configs;

import graph.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a single node in the rendered runtime graph.
 * Nodes model either topics or agents and keep outgoing edges plus optional
 * message metadata used by the visualization layer.
 */
public class Node {
    /**
     * Identifies the semantic type of a graph node.
     */
    public enum Kind {
        /**
         * A topic node in the runtime graph.
         */
        TOPIC,
        /**
         * An agent node in the runtime graph.
         */
        AGENT
    }

    private String name;
    private String displayName;
    private Kind kind;
    private List<Node> edges;
    private Message message;

    /**
     * Creates a node whose internal and display names are the same.
     *
     * @param name the internal and display name of the node
     */
    public Node(String name) {
        this(name, name, null);
    }

    /**
     * Creates a node with explicit internal name, display name, and kind.
     *
     * @param name the internal node identifier
     * @param displayName the label shown in rendered output
     * @param kind the semantic node type
     */
    public Node(String name, String displayName, Kind kind) {
        this.name = name;
        this.displayName = displayName;
        this.kind = kind;
        this.edges = new ArrayList<>();
    }

    /**
     * Returns the internal node name.
     *
     * @return the internal node identifier
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the internal node name.
     *
     * @param name the new internal node identifier
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the display label for the node.
     *
     * @return the display label
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Updates the display label for the node.
     *
     * @param displayName the new display label
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the node kind.
     *
     * @return the semantic node type
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Updates the node kind.
     *
     * @param kind the new semantic node type
     */
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    /**
     * Returns the outgoing edges of this node.
     *
     * @return the list of adjacent destination nodes
     */
    public List<Node> getEdges() {
        return edges;
    }

    /**
     * Replaces the outgoing edges of this node.
     *
     * @param edges the new list of outgoing edges
     */
    public void setEdges(List<Node> edges) {
        this.edges = edges;
    }

    /**
     * Returns the message associated with this node, if any.
     *
     * @return the attached message, or {@code null} if none is set
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Associates a message with this node.
     *
     * @param message the message to attach
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Adds an outgoing edge from this node to the supplied destination.
     *
     * @param n the destination node
     */
    public void addEdge(Node n) {
        edges.add(n);
    }

    /**
     * Determines whether this node participates in a directed cycle.
     *
     * @return {@code true} if a cycle is reachable from this node; otherwise {@code false}
     */
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
