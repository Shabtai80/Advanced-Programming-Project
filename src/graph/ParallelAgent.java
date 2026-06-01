package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Wraps another {@link Agent} and processes callbacks on a dedicated worker thread.
 * This adapter allows agent execution to be decoupled from the publishing thread.
 */
public class ParallelAgent implements Agent {
    private final Agent agent;
    private final BlockingQueue<QueueItem> queue;
    private final Thread worker;
    private volatile boolean running;

    private static class QueueItem {
        private final String topic;
        private final Message msg;

        private QueueItem(String topic, Message msg) {
            this.topic = topic;
            this.msg = msg;
        }
    }

    /**
     * Creates a parallel wrapper around an existing agent.
     *
     * @param agent the wrapped agent that should process queued callbacks
     * @param capacity the maximum number of queued callback events
     */
    public ParallelAgent(Agent agent, int capacity) {
        this.agent = agent;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.running = true;
        this.worker = new Thread(() -> {
            while (running) {
                try {
                    QueueItem item = queue.take();
                    agent.callback(item.topic, item.msg);
                } catch (InterruptedException e) {
                    if (!running) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        this.worker.start();
    }

    /**
     * Returns the wrapped agent's name.
     *
     * @return the wrapped agent name
     */
    @Override
    public String getName() {
        return agent.getName();
    }

    /**
     * Resets the wrapped agent.
     */
    @Override
    public void reset() {
        agent.reset();
    }

    /**
     * Queues a callback for asynchronous processing by the worker thread.
     *
     * @param topic the topic that triggered the callback
     * @param msg the published message
     */
    @Override
    public void callback(String topic, Message msg) {
        try {
            queue.put(new QueueItem(topic, msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the worker thread and closes the wrapped agent.
     */
    @Override
    public void close() {
        running = false;
        worker.interrupt();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        agent.close();
    }
}
