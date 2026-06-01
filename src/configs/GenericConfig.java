package configs;

import graph.Agent;
import graph.ParallelAgent;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GenericConfig implements Config {
    private String confFile;
    private final List<ParallelAgent> agents = new ArrayList<>();

    public void setConfFile(String confFile) {
        this.confFile = confFile;
    }

    @Override
    public void create() {
        List<String> rawLines;
        try {
            rawLines = Files.readAllLines(Paths.get(confFile));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config file: " + confFile, e);
        }

        List<String> lines = new ArrayList<>();
        for (String line : rawLines) {
            if (!line.trim().isEmpty()) {
                lines.add(line.trim());
            }
        }

        if (lines.size() % 3 != 0) {
            throw new IllegalArgumentException("Configuration must contain groups of 3 non-empty lines");
        }

        for (int i = 0; i < lines.size(); i += 3) {
            String className = lines.get(i);
            String[] subs = lines.get(i + 1).split(",");
            String[] pubs = lines.get(i + 2).split(",");

            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> constructor = clazz.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) constructor.newInstance(subs, pubs);
                ParallelAgent parallelAgent = new ParallelAgent(agent, 10);
                agents.add(parallelAgent);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to create agent: " + className, e);
            }
        }
    }

    @Override
    public String getName() {
        return "GenericConfig";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void close() {
        for (ParallelAgent agent : agents) {
            agent.close();
        }
        agents.clear();
    }
}
