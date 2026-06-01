package graph;

/**
 * Describes an executable graph component that can consume topic messages,
 * publish results, and participate in the project runtime.
 */
public interface Agent {
    /**
     * Returns the agent name used for display and graph construction.
     *
     * @return the human-readable agent name
     */
    String getName();

    /**
     * Resets the internal state of the agent.
     */
    void reset();

    /**
     * Invokes the agent in response to a published topic message.
     *
     * @param topic the topic name that triggered the callback
     * @param msg the message that was published
     */
    void callback(String topic, Message msg);

    /**
     * Releases any resources held by the agent.
     */
    void close();
}
