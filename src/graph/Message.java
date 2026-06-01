package graph;

import java.util.Arrays;
import java.util.Date;

/**
 * Immutable message payload used for communication between topics and agents.
 * A message exposes the original bytes together with convenient text, numeric,
 * and timestamp views for downstream processing and inspection.
 */
public class Message {
    /**
     * The immutable raw payload bytes.
     */
    public final byte[] data;

    /**
     * The payload interpreted as text.
     */
    public final String asText;

    /**
     * The payload interpreted as a numeric value, or {@link Double#NaN} when parsing fails.
     */
    public final double asDouble;

    /**
     * The creation timestamp of the message.
     */
    public final Date date;

    /**
     * Creates a message from raw bytes.
     *
     * @param data the message payload bytes
     */
    public Message(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
        this.asText = new String(this.data);
        double parsedValue;
        try {
            parsedValue = Double.parseDouble(this.asText);
        } catch (NumberFormatException e) {
            parsedValue = Double.NaN;
        }
        this.asDouble = parsedValue;
        this.date = new Date();
    }

    /**
     * Creates a message from text using the platform default byte encoding.
     *
     * @param text the text payload
     */
    public Message(String text) {
        this(text.getBytes());
    }

    /**
     * Creates a message from a numeric value.
     *
     * @param value the numeric value to encode as text
     */
    public Message(double value) {
        this(String.valueOf(value));
    }
}
