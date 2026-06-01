package configs;

import graph.TopicManagerSingleton;
import graph.Topic;
import graph.Agent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory directed graph representation of the project's runtime topology.
 * The graph is built from topics and agents so it can be analyzed for cycles
 * and rendered for the HTTP-based visualization layer.
 */
public class Graph extends ArrayList<Node> {
    /**
     * Creates an empty runtime graph.
     */
    public Graph() {
    }

    /**
     * Determines whether any node in the graph participates in a cycle.
     *
     * @return {@code true} if the graph contains at least one cycle; otherwise {@code false}
     */
    public boolean hasCycles() {
        for (Node node : this) {
            if (node.hasCycles()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rebuilds the graph from the topics currently registered in the
     * {@link TopicManagerSingleton} singleton.
     *
     * @throws IllegalStateException if reflective access to runtime topology data fails
     */
    public void createFromTopics() {
        clear();

        Map<String, Node> topicNodesByName = new HashMap<>();
        Set<Agent> agents = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Agent, Integer> agentOrderByIdentity = new IdentityHashMap<>();
        Map<String, Integer> agentNameCounts = new HashMap<>();

        for (Topic topic : TopicManagerSingleton.get().getTopics()) {
            getOrCreateTopicNode(topicNodesByName, topic.name);

            for (Agent agent : getAgents(topic, "subs")) {
                registerAgent(agent, agents, agentOrderByIdentity);
            }

            for (Agent agent : getAgents(topic, "pubs")) {
                registerAgent(agent, agents, agentOrderByIdentity);
            }
        }

        Map<Agent, Node> agentNodesByIdentity = createAgentNodes(agents, agentOrderByIdentity, agentNameCounts);

        for (Map.Entry<Agent, Node> entry : agentNodesByIdentity.entrySet()) {
            Agent agent = entry.getKey();
            Node agentNode = entry.getValue();

            for (String inputTopicName : getAgentInputTopics(agent)) {
                Node topicNode = getOrCreateTopicNode(topicNodesByName, inputTopicName);
                addEdge(topicNode, agentNode);
            }

            for (String outputTopicName : getAgentOutputTopics(agent)) {
                Node topicNode = getOrCreateTopicNode(topicNodesByName, outputTopicName);
                addEdge(agentNode, topicNode);
            }
        }
    }

    private Node getOrCreateTopicNode(Map<String, Node> topicNodesByName, String topicName) {
        return getOrCreateNode(topicNodesByName, "T:" + topicName, topicName, Node.Kind.TOPIC);
    }

    private void registerAgent(Agent agent, Set<Agent> agents, Map<Agent, Integer> agentOrderByIdentity) {
        if (agents.add(agent)) {
            agentOrderByIdentity.put(agent, agentOrderByIdentity.size() + 1);
        }
    }

    private Map<Agent, Node> createAgentNodes(
        Set<Agent> agents,
        Map<Agent, Integer> agentOrderByIdentity,
        Map<String, Integer> agentNameCounts
    ) {
        Map<Agent, Node> agentNodesByIdentity = new IdentityHashMap<>();
        List<Agent> orderedAgents = new ArrayList<>(agents);
        orderedAgents.sort((left, right) -> Integer.compare(
                agentOrderByIdentity.getOrDefault(left, Integer.MAX_VALUE),
                agentOrderByIdentity.getOrDefault(right, Integer.MAX_VALUE)));

        for (Agent agent : orderedAgents) {
            String baseName = agent.getName();
            int instanceIndex = agentNameCounts.getOrDefault(baseName, 0) + 1;
            agentNameCounts.put(baseName, instanceIndex);

            String internalId = "A:" + baseName + "#" + instanceIndex;
            String displayName = agentNameCounts.get(baseName) > 1 || hasMultipleAgentsWithName(orderedAgents, baseName)
                    ? baseName + " #" + instanceIndex
                    : baseName;

            Node agentNode = new Node(internalId, displayName, Node.Kind.AGENT);
            add(agentNode);
            agentNodesByIdentity.put(agent, agentNode);
        }

        return agentNodesByIdentity;
    }

    private boolean hasMultipleAgentsWithName(List<Agent> agents, String agentName) {
        int count = 0;
        for (Agent agent : agents) {
            if (agentName.equals(agent.getName()) && ++count > 1) {
                return true;
            }
        }
        return false;
    }

    private Node getOrCreateNode(Map<String, Node> nodesByName, String nodeName, String displayName, Node.Kind kind) {
        Node node = nodesByName.get(nodeName);
        if (node == null) {
            node = new Node(nodeName, displayName, kind);
            nodesByName.put(nodeName, node);
            add(node);
        }
        return node;
    }

    private void addEdge(Node from, Node to) {
        if (!from.getEdges().contains(to)) {
            from.addEdge(to);
        }
    }

    private List<String> getAgentInputTopics(Agent agent) {
        List<String> topics = new ArrayList<>();
        topics.addAll(getStringArrayField(agent, "subs"));
        topics.addAll(getNamedStringFields(agent, "inputTopic"));
        return deduplicate(topics);
    }

    private List<String> getAgentOutputTopics(Agent agent) {
        List<String> topics = new ArrayList<>();
        topics.addAll(getStringArrayField(agent, "pubs"));
        topics.addAll(getNamedStringFields(agent, "outputTopic"));
        return deduplicate(topics);
    }

    private List<String> getStringArrayField(Agent agent, String fieldName) {
        try {
            Field field = agent.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(agent);
            if (!(value instanceof String[])) {
                return List.of();
            }

            List<String> topics = new ArrayList<>();
            for (String topicName : (String[]) value) {
                if (topicName != null && !topicName.isBlank()) {
                    topics.add(topicName.trim());
                }
            }
            return topics;
        } catch (ReflectiveOperationException e) {
            return List.of();
        }
    }

    private List<String> getNamedStringFields(Agent agent, String fieldNamePart) {
        List<String> topics = new ArrayList<>();
        for (Field field : agent.getClass().getDeclaredFields()) {
            if (field.getType() != String.class || !field.getName().contains(fieldNamePart)) {
                continue;
            }

            field.setAccessible(true);
            try {
                String value = (String) field.get(agent);
                if (value != null && !value.isBlank()) {
                    topics.add(value.trim());
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to read agent field: " + field.getName(), e);
            }
        }
        return topics;
    }

    private List<String> deduplicate(Collection<String> values) {
        Set<String> uniqueValues = new LinkedHashSet<>(values);
        return new ArrayList<>(uniqueValues);
    }

    @SuppressWarnings("unchecked")
    private List<Agent> getAgents(Topic topic, String fieldName) {
        try {
            Field field = Topic.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (List<Agent>) field.get(topic);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read topic field: " + fieldName, e);
        }
    }
}
