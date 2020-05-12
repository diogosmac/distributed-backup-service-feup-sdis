package chord;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChordChannel implements Runnable {

    /**
     * Auxiliary class that stores a received message
     */
    private static class Message {

        private final InetSocketAddress address;
        private final String[] arguments;

        private Message(InetSocketAddress address, String message) {
            this.address = address;
            this.arguments = message.split(" ");
        }

        /**
         * Returns the address the Message was received from
         * @return (self explanatory)
         */
        private InetSocketAddress getAddress() {
            return address;
        }

        /**
         * Gets the separated arguments of the Message
         * @return String array with the arguments
         */
        private String[] getArguments() {
            return arguments;
        }

        /**
         * Gets the entire Message in one String
         * @return (self explanatory)
         */
        private String getMessage() {
            return String.join(" ", arguments);
        }

    }

    /**
     * ChordNode object this channel is linked to
     */
    private final ChordNode parent;

    /**
     * Socket through which the channel receives messages
     */
    private SSLServerSocket serverSocket;

    /**
     * Queue where the received messages are stored
     */
    private final ConcurrentLinkedQueue<Message> messageQueue;

    /**
     * Timeout for socket operations
     */
    final protected int timeout = 2000;

    /**
     * Constructor for the ChordChannel
     * @param parent ChordNode that this channel is linked to
     */
    public ChordChannel(ChordNode parent) {
        this.parent = parent;
        messageQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Opens the SSLServerSocket through which the ChordChannel will
     * function
     * @param port Number of the port in which the socket will be opened
     */
    protected void open(int port) {
        try {
            serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(port);
        } catch (IOException e) {
            System.out.println("Error creating SSLServerSocket in port " + port);
            e.printStackTrace();
        }
    }

    /**
     * ChordChannel thread execution
     */
    @Override
    public void run() {

        while (true) {

            try {

                SSLSocket socket = (SSLSocket) serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                String message = (String) ois.readObject();
                handleMessage(socket, message);

                socket.close();

            } catch (Exception e) { e.printStackTrace(); }

        }

    }

    /**
     * Handles a message received by the ChordChannel
     * @param socket Socket from which the message was read
     * @param message Message that was received
     */
    protected void handleMessage(SSLSocket socket, String message) {
        // TODO: Handle received message
        InetSocketAddress address = (
                socket == null
                        ? this.parent.getAddress()
                        : (InetSocketAddress) socket.getRemoteSocketAddress());
        messageQueue.add(new Message(address, message));
    }

    /**
     * Sends a message through the ChordChannel
     * @param address Address to which the message should be sent
     * @param message Message to be sent
     */
    protected void sendMessage(InetSocketAddress address, String message) {
        // TODO: Properly send message
        if (address.equals(this.parent.getAddress())) {
            handleMessage(null, message);
            return;
        }

        try {

            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
            socket.connect(address, timeout);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);

            socket.close();

        } catch (IOException e) { e.printStackTrace(); }

    }

    /**
     * Starts the thread in charge of running the ChordChannel
     */
    protected void start() {
        this.parent.execute(this);
    }

}
