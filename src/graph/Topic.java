package graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a named message channel in the publish/subscribe graph.
 * A topic tracks subscribing agents, publishing agents, and the most recently
 * published message so the current system state can be inspected or visualized.
 */
public class Topic {
    /**
     * The externally visible topic name.
     */
    public final String name;
    List<Agent> subs;
    List<Agent> pubs;
    private Message lastMessage;

    Topic(String name) {
        this.name = name;
        this.subs = new ArrayList<>();
        this.pubs = new ArrayList<>();
    }

    /**
     * Subscribes an agent to this topic so it will receive future publications.
     *
     * @param a the agent to subscribe
     */
    public void subscribe(Agent a) {
        if (!subs.contains(a)) {
            subs.add(a);
        }
    }

    /**
     * Removes a subscribed agent from this topic.
     *
     * @param a the agent to unsubscribe
     */
    public void unsubscribe(Agent a) {
        subs.remove(a);
    }

    /**
     * Publishes a message to this topic, stores it as the latest value, and
     * notifies all subscribed agents.
     *
     * @param msg the message to publish
     */
    public void publish(Message msg) {
        lastMessage = msg;
        for (Agent agent : new ArrayList<>(subs)) {
            agent.callback(name, msg);
        }
    }

    /**
     * Returns the most recently published message for this topic.
     *
     * @return the latest message, or {@code null} if nothing has been published yet
     */
    public Message getLastMessage() {
        return lastMessage;
    }

    /**
     * Updates the stored latest message for this topic without notifying subscribers.
     *
     * @param lastMessage the message to store as the latest topic value
     */
    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    /**
     * Registers an agent as a publisher of this topic.
     *
     * @param a the publishing agent to add
     */
    public void addPublisher(Agent a) {
        if (!pubs.contains(a)) {
            pubs.add(a);
        }
    }

    /**
     * Removes an agent from the list of topic publishers.
     *
     * @param a the publishing agent to remove
     */
    public void removePublisher(Agent a) {
        pubs.remove(a);
    }
}
