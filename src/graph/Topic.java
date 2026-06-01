package graph;

import java.util.ArrayList;
import java.util.List;

public class Topic {
    public final String name;
    List<Agent> subs;
    List<Agent> pubs;
    private Message lastMessage;

    Topic(String name) {
        this.name = name;
        this.subs = new ArrayList<>();
        this.pubs = new ArrayList<>();
    }

    public void subscribe(Agent a) {
        if (!subs.contains(a)) {
            subs.add(a);
        }
    }

    public void unsubscribe(Agent a) {
        subs.remove(a);
    }

    public void publish(Message msg) {
        lastMessage = msg;
        for (Agent agent : new ArrayList<>(subs)) {
            agent.callback(name, msg);
        }
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void addPublisher(Agent a) {
        if (!pubs.contains(a)) {
            pubs.add(a);
        }
    }

    public void removePublisher(Agent a) {
        pubs.remove(a);
    }
}
