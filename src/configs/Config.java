package configs;

/**
 * Defines the lifecycle of a reusable runtime configuration.
 * A configuration knows how to create a set of runtime components, expose
 * metadata about itself, and release the resources it created.
 */
public interface Config {
    /**
     * Creates and initializes the runtime objects described by the configuration.
     */
    void create();

    /**
     * Returns the configuration name.
     *
     * @return the logical configuration name
     */
    String getName();

    /**
     * Returns the configuration version.
     *
     * @return the version number of the configuration
     */
    int getVersion();

    /**
     * Closes the configuration and releases its runtime resources.
     */
    void close();
}
