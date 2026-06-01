package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class TopicManagerSingleton {
    public static TopicManager get() {
        return TopicManager.instance;
    }

    public static class TopicManager {
        private static final TopicManager instance = new TopicManager();
        private final ConcurrentHashMap<String, Topic> topics;

        private TopicManager() {
            this.topics = new ConcurrentHashMap<>();
        }

        public Topic getTopic(String name) {
            return topics.computeIfAbsent(name, Topic::new);
        }

        public Collection<Topic> getTopics() {
            return topics.values();
        }

        public void clear() {
            topics.clear();
        }
    }
}
