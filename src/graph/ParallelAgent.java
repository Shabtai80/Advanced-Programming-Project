package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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

    @Override
    public String getName() {
        return agent.getName();
    }

    @Override
    public void reset() {
        agent.reset();
    }

    @Override
    public void callback(String topic, Message msg) {
        try {
            queue.put(new QueueItem(topic, msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

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
