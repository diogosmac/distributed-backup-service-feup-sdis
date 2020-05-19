package chord.communication;

import java.net.InetSocketAddress;

/**
 * Auxiliary class that stores a received message
 */
public class Message {

    private final InetSocketAddress address;
    private final String[] arguments;

    public Message(InetSocketAddress address, String message) {
        this.address = address;
        this.arguments = message.split(" ");
    }

    /**
     * Returns the address the Message was received from
     * @return (self explanatory)
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Gets the separated arguments of the Message
     * @return String array with the arguments
     */
    public String[] getArguments() {
        return arguments;
    }

    /**
     * Gets the entire Message in one String
     * @return (self explanatory)
     */
    public String getMessage() {
        return String.join(" ", arguments);
    }

}
