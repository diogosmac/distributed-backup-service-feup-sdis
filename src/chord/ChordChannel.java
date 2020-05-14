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

    private InetSocketAddress getAddress(SSLSocket socket) {
        return (socket == null
                ? this.parent.getAddress()
                : (InetSocketAddress) socket.getRemoteSocketAddress());
    }

    /**
     * Handles a message received by the ChordChannel
     * @param socket Socket from which the message was read
     * @param message Message that was received
     */
    protected void handleMessage(SSLSocket socket, String message) {
        // TODO: Handle received message
        String[] args = message.split(" ");
        switch(args[0]) {
            case "FINDSUCCESSOR":
                int id = Integer.parseInt(args[1]);
                InetSocketAddress fsAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                this.parent.findSuccessor(fsAddress, id);
                break;

            case "SUCCESSORFOUND":
                synchronized (this.parent) {
                    InetSocketAddress sfAddress = getAddress(socket);
                    messageQueue.add(new Message(sfAddress, message));
                    this.parent.notify();
                }
                break;

            case "JOINING":
                //Calls find successor as the new node
                InetSocketAddress newNodeInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                int newNodeId = Integer.parseInt(args[1]);
                this.parent.findSuccessor(newNodeInfo, newNodeId);
                break;

        }

    }

    /**
     * Sends a message through the ChordChannel
     * @param address Address to which the message should be sent
     * @param message Message to be sent
     */
    protected void sendMessage(InetSocketAddress address, String message) {

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
     * Creates the findSuccessor message to be later sent
     * @param requestOrigin Contains the IP and Port of the ChordNode that wants to find the successor of id
     * @param requestedId Id that the origin Node requested
     * @return Message to be sent, delegating the findSuccessor work to other node
     */
    private String createFindSuccessorMessage(InetSocketAddress requestOrigin, int requestedId) {
        // Message format: FINDSUCCESSOR <requestedId> <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("FINDSUCCESSOR").append(" "); // Header
        sb.append(requestedId).append(" "); // Id requested
        sb.append(requestOrigin.getAddress().getHostAddress()).append(" "); // Origin's IP
        sb.append(requestOrigin.getPort()); // Origin's Port

        return sb.toString();
    }

    /**
     * Delegates the work of finding the successor of id, by sending a message to destination
     * @param requestOrigin Contains the IP and Port of the ChordNode that wants to find the successor of id
     * @param requestedId Id that the origin Node requested
     * @param destination Contains the IP and Port of the ChordNode that will be receiving the request
     */
    protected String[] sendFindSuccessorMessage(InetSocketAddress requestOrigin, int requestedId,
                                            InetSocketAddress destination) {
        String message = this.createFindSuccessorMessage(requestOrigin, requestedId);
        this.sendMessage(destination, message);

        if (!this.parent.getAddress().getHostName().equals(requestOrigin.getAddress().getHostName()))  // This node didn't request the id
            return null; // Delegates work, and returns
        else
        {
            synchronized (this.parent) {
                try {
                    this.parent.wait(this.timeout*2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (Message currentMessage : this.messageQueue) {
                    String [] messageReceived = currentMessage.getArguments();
                    if (messageReceived.length == 4 && messageReceived[1].equals(Integer.toString(requestedId))) // Answer to request made
                        return messageReceived;

                    return currentMessage.arguments;
                }

            }

        }

        return null;
    }

    /**
     * Builds the message with the requested information about the Node with requestedId
     * @param requestedId
     * @param requestedNodeInfo
     * @return
     */
    private String createReturnMessage(int requestedId, InetSocketAddress requestedNodeInfo) {
        // Message format: SUCCESSORFOUND <requestedId> <requestedNodeIp> <requestedNodePort>
        StringBuilder sb = new StringBuilder();
        sb.append("SUCCESSORFOUND").append(" ");
        sb.append(requestedId).append(" ");
        sb.append(requestedNodeInfo.getAddress().getHostName()).append(" ");
        sb.append(requestedNodeInfo.getPort()).append(" ");
        return sb.toString();
    }

    /**
     * Sends a message with the information requested to the Node that made the request to find the successor
     * @param requestOrigin
     * @param requestedId
     * @param requestedNodeInfo
     */
    protected void returnFindSuccessor(InetSocketAddress requestOrigin, int requestedId, InetSocketAddress requestedNodeInfo) {
        String message = this.createReturnMessage(requestedId, requestedNodeInfo);
        this.sendMessage(requestOrigin, message);
    }

    /**
     * Builds the joining message, used when a peer joins the chord
     * @return
     */
    private String createJoiningMessage() {
        // Message format: JOINING <newNodeId> <newNodeIp> <newNodePort>
        StringBuilder sb = new StringBuilder();
        sb.append("JOINING").append(" ");
        sb.append(this.parent.getId()).append(" ");
        sb.append(this.parent.getAddress().getHostName()).append(" ");
        sb.append(this.parent.getAddress().getPort());
        return sb.toString();
    }

    /**
     * Creates and sends a joining message to the known node
     * @param knownNode
     */
    protected void sendJoiningMessage(InetSocketAddress knownNode) {
        String message = this.createJoiningMessage();
        this.sendMessage(knownNode, message);
    }

}
