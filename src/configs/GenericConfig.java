package configs;

import graph.Agent;
import graph.ParallelAgent;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic configuration loader that creates agents from a plain-text
 * configuration file by using reflection.
 * This class enables external developers to deploy agent graphs without
 * recompiling the HTTP server layer.
 */
public class GenericConfig implements Config {
    private String confFile;
    private final List<ParallelAgent> agents = new ArrayList<>();

    /**
     * Creates an empty generic configuration loader.
     */
    public GenericConfig() {
    }

    /**
     * Sets the path to the configuration file that should be loaded.
     *
     * @param confFile the configuration file path
     */
    public void setConfFile(String confFile) {
        this.confFile = confFile;
    }

    /**
     * Reads the configured file, instantiates the declared agents, and wraps
     * them in {@link ParallelAgent} executors.
     *
     * @throws IllegalStateException if the file cannot be read or an agent cannot be created
     * @throws IllegalArgumentException if the file format is invalid
     */
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

    /**
     * Returns the name of this configuration implementation.
     *
     * @return the configuration name
     */
    @Override
    public String getName() {
        return "GenericConfig";
    }

    /**
     * Returns the version number of this configuration implementation.
     *
     * @return the configuration version
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Closes every created parallel agent and clears the managed runtime state.
     */
    @Override
    public void close() {
        for (ParallelAgent agent : agents) {
            agent.close();
        }
        agents.clear();
    }
}
