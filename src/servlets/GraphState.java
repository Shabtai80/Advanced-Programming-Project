package servlets;

import configs.Graph;

public class GraphState {
    private static Graph currentGraph;

    public static synchronized void setGraph(Graph graph) {
        currentGraph = graph;
    }

    public static synchronized Graph getGraph() {
        return currentGraph;
    }
}
