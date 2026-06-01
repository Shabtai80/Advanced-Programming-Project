package graph;

import java.util.Arrays;
import java.util.Date;

public class Message {
    public final byte[] data;
    public final String asText;
    public final double asDouble;
    public final Date date;

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

    public Message(String text) {
        this(text.getBytes());
    }

    public Message(double value) {
        this(String.valueOf(value));
    }
}
