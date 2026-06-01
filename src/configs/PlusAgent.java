package configs;

import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
import graph.Topic;

public class PlusAgent implements Agent {
    private final String[] subs;
    private final String[] pubs;
    private double x;
    private double y;

    public PlusAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
        this.x = 0;
        this.y = 0;

        TopicManagerSingleton.get().getTopic(subs[0]).subscribe(this);
        TopicManagerSingleton.get().getTopic(subs[1]).subscribe(this);
    }

    @Override
    public String getName() {
        return "PlusAgent";
    }

    @Override
    public void reset() {
        x = 0;
        y = 0;
    }

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

    @Override
    public void close() {
    }
}
