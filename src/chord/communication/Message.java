package chord.communication;

/**
 * Message
 * <p>
 * Auxiliary class that stores a received message
 */
public class Message {

    private final String[] arguments;

    public Message(String message) {
        this.arguments = message.split(" ");
    }

    /**
     * Gets the separated arguments of the Message
     *
     * @return String array with the arguments
     */
    public String[] getArguments() {
        return arguments;
    }

}
