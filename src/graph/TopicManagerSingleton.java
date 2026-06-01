package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides global access to the project's shared topic registry.
 * The singleton is used by agents, servlets, and graph-building code to
 * retrieve or create topics that participate in the running system.
 */
public class TopicManagerSingleton {
    /**
     * Creates access to the singleton topic manager.
     */
    public TopicManagerSingleton() {
    }

    /**
     * Returns the singleton topic manager instance.
     *
     * @return the shared topic manager
     */
    public static TopicManager get() {
        return TopicManager.instance;
    }

    /**
     * Stores and manages all topics known to the running application.
     */
    public static class TopicManager {
        private static final TopicManager instance = new TopicManager();
        private final ConcurrentHashMap<String, Topic> topics;

        private TopicManager() {
            this.topics = new ConcurrentHashMap<>();
        }

        /**
         * Returns the topic with the supplied name, creating it if necessary.
         *
         * @param name the unique topic name
         * @return the existing or newly created topic
         */
        public Topic getTopic(String name) {
            return topics.computeIfAbsent(name, Topic::new);
        }

        /**
         * Returns all currently known topics.
         *
         * @return a live collection view of the registered topics
         */
        public Collection<Topic> getTopics() {
            return topics.values();
        }

        /**
         * Removes all topics from the registry.
         */
        public void clear() {
            topics.clear();
        }
    }
}
