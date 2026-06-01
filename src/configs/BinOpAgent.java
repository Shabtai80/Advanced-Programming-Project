package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import java.util.function.BinaryOperator;

public class BinOpAgent implements Agent {
    private final String name;
    private final String firstInputTopic;
    private final String secondInputTopic;
    private final String outputTopic;
    private final BinaryOperator<Double> operation;
    private Message firstMessage;
    private Message secondMessage;

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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void reset() {
        firstMessage = new Message(0);
        secondMessage = new Message(0);
    }

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

    @Override
    public void close() {
        TopicManagerSingleton.TopicManager topicManager = TopicManagerSingleton.get();
        topicManager.getTopic(firstInputTopic).unsubscribe(this);
        topicManager.getTopic(secondInputTopic).unsubscribe(this);
        topicManager.getTopic(outputTopic).removePublisher(this);
    }
}
