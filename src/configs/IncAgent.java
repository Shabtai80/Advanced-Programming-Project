package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import graph.Topic;

/**
 * Sample agent that increments incoming numeric messages and publishes the
 * result to its configured output topic.
 */
public class IncAgent implements Agent {
    private final String[] subs;
    private final String[] pubs;

    /**
     * Creates an increment agent and subscribes it to its single input topic.
     *
     * @param subs the subscribed input topic names
     * @param pubs the published output topic names
     */
    public IncAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;

        TopicManagerSingleton.get().getTopic(subs[0]).subscribe(this);
    }

    /**
     * Returns the display name of the agent.
     *
     * @return the agent name
     */
    @Override
    public String getName() {
        return "IncAgent";
    }

    /**
     * Resets the agent.
     * This implementation has no internal state to reset.
     */
    @Override
    public void reset() {
    }

    /**
     * Increments a numeric input message and publishes the result.
     *
     * @param topic the topic that triggered the callback
     * @param msg the received message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) {
            return;
        }

        TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(msg.asDouble + 1));
    }

    /**
     * Closes the agent.
     * This implementation does not hold external resources.
     */
    @Override
    public void close() {
    }
}
