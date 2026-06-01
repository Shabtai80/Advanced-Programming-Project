package servlets;

import configs.Graph;

/**
 * Stores the currently active graph snapshot shared by graph-related servlets.
 * This helper class allows upload and refresh endpoints to coordinate around
 * the same rendered runtime graph.
 */
public class GraphState {
    private static Graph currentGraph;

    /**
     * Creates a graph state holder.
     */
    public GraphState() {
    }

    /**
     * Replaces the currently stored graph snapshot.
     *
     * @param graph the graph that should become the active shared graph
     */
    public static synchronized void setGraph(Graph graph) {
        currentGraph = graph;
    }

    /**
     * Returns the currently stored graph snapshot.
     *
     * @return the active shared graph, or {@code null} if no graph has been stored yet
     */
    public static synchronized Graph getGraph() {
        return currentGraph;
    }
}
