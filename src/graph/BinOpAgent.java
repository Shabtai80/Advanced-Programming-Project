package graph;

import java.util.function.BinaryOperator;

/**
 * Reusable agent that combines two numeric input topics with a binary operation
 * and publishes the computed result to an output topic.
 */
public class BinOpAgent implements Agent {
    private final String name;
    private final String firstInputTopic;
    private final String secondInputTopic;
    private final String outputTopic;
    private final BinaryOperator<Double> operation;
    private Message firstMessage;
    private Message secondMessage;

    /**
     * Creates a binary-operation agent and registers it with the relevant topics.
     *
     * @param name the display name of the agent
     * @param firstInputTopic the first input topic name
     * @param secondInputTopic the second input topic name
     * @param outputTopic the output topic name
     * @param operation the numeric operation to apply to the two input values
     */
    public BinOpAgent(
        String name,
        String firstInputTopic,
        String secondInputTopic,
        String outputTopic,
        BinaryOperator<Double> operation
    ) {
        this.name = name;
        this.firstInputTopic = firstInputTopic;
        this.secondInputTopic = secondInputTopic;
        this.outputTopic = outputTopic;
        this.operation = operation;

        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        topicManager.getTopic(firstInputTopic).subscribe(this);
        topicManager.getTopic(secondInputTopic).subscribe(this);
        topicManager.getTopic(outputTopic).addPublisher(this);

        reset();
    }

    /**
     * Returns the display name of the agent.
     *
     * @return the agent name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Resets both cached input values to zero.
     */
    @Override
    public void reset() {
        firstMessage = new Message(0);
        secondMessage = new Message(0);
    }

    /**
     * Updates the matching input value and publishes a result when both inputs
     * contain valid numeric messages.
     *
     * @param topic the topic that supplied the new message
     * @param msg the received message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (firstInputTopic.equals(topic)) {
            firstMessage = msg;
        } else if (secondInputTopic.equals(topic)) {
            secondMessage = msg;
        } else {
            return;
        }

        if (!Double.isNaN(firstMessage.asDouble) && !Double.isNaN(secondMessage.asDouble)) {
            double result = operation.apply(firstMessage.asDouble, secondMessage.asDouble);
            TopicManagerSingleton.get().getTopic(outputTopic).publish(new Message(result));
        }
    }

    /**
     * Unsubscribes the agent from its input topics and removes its publisher
     * registration from the output topic.
     */
    @Override
    public void close() {
        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        topicManager.getTopic(firstInputTopic).unsubscribe(this);
        topicManager.getTopic(secondInputTopic).unsubscribe(this);
        topicManager.getTopic(outputTopic).removePublisher(this);
    }
}
