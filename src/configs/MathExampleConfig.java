package configs;

/**
 * Example configuration that creates a small arithmetic processing graph for
 * demonstration and testing purposes.
 */
public class MathExampleConfig implements Config {
    /**
     * Creates the example arithmetic agent graph.
     */
    public MathExampleConfig() {
    }

    /**
     * Instantiates the example agents used by this configuration.
     */
    @Override
    public void create() {
        new BinOpAgent("plus", "A", "B", "R1", (x, y) -> x + y);
        new BinOpAgent("minus", "A", "B", "R2", (x, y) -> x - y);
        new BinOpAgent("mul", "R1", "R2", "R3", (x, y) -> x * y);
    }

    /**
     * Returns the name of this example configuration.
     *
     * @return the configuration name
     */
    @Override
    public String getName() {
        return "Math Example";
    }

    /**
     * Returns the version of this example configuration.
     *
     * @return the configuration version
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Closes the configuration.
     * This implementation does not hold external resources.
     */
    @Override
    public void close() {
    }
}
