package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import graph.Topic;

/**
 * Sample agent that sums the latest values from two input topics and publishes
 * the result to its configured output topic.
 */
public class PlusAgent implements Agent {
    private final String[] subs;
    private final String[] pubs;
    private double x;
    private double y;

    /**
     * Creates a plus agent and subscribes it to its two input topics.
     *
     * @param subs the subscribed input topic names
     * @param pubs the published output topic names
     */
    public PlusAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
        this.x = 0;
        this.y = 0;

        TopicManagerSingleton.get().getTopic(subs[0]).subscribe(this);
        TopicManagerSingleton.get().getTopic(subs[1]).subscribe(this);
    }

    /**
     * Returns the display name of the agent.
     *
     * @return the agent name
     */
    @Override
    public String getName() {
        return "PlusAgent";
    }

    /**
     * Resets the cached numeric inputs to zero.
     */
    @Override
    public void reset() {
        x = 0;
        y = 0;
    }

    /**
     * Updates one of the cached inputs and publishes the current sum.
     *
     * @param topic the topic that triggered the callback
     * @param msg the received message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) {
            return;
        }

        if (subs[0].equals(topic)) {
            x = msg.asDouble;
        } else if (subs[1].equals(topic)) {
            y = msg.asDouble;
        } else {
            return;
        }

        TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(x + y));
    }

    /**
     * Closes the agent.
     * This implementation does not hold external resources.
     */
    @Override
    public void close() {
    }
}
