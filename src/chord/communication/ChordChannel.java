package chord.communication;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import chord.ChordNode;
import chord.NodePair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChordChannel implements Runnable {

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
    protected final ConcurrentLinkedQueue<Message> messageQueue;

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
        this.open(this.parent.getAddress().getPort());
    }

    /**
     * Opens the SSLServerSocket through which the ChordChannel will
     * function
     * @param port Number of the port in which the socket will be opened
     */
    public void open(int port) {
        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "../keys/keystore" + (this.parent.getId() % 3 + 1));
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        // Truststore
        System.setProperty("javax.net.ssl.trustStore", "../keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

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
            System.out.println("QUALQUER COISA DISTINTA");

            try {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                String message = (String) ois.readObject();
                ois.close();
                handleMessage(socket, message);
                socket.close();
            } catch (Exception e) {
                System.out.println("======== PENIS =======");
                e.printStackTrace();
            }

        }

    }

    public void handleMessage(SSLSocket socket, String message) {
        new MessageHandler(socket, message, this, this.parent).start();
    }

    /**
     * Sends a message through the ChordChannel
     * @param address Address to which the message should be sent
     * @param message Message to be sent
     */
    public void sendMessage(InetSocketAddress address, String message) {

            if (address.equals(this.parent.getAddress())) {
                handleMessage(null, message);
                return;
            }

            try {
                SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
                socket.connect(address, timeout);

                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(message);

                socket.close();

            } catch (Exception e) {
                // error in communication, maybe successor stopped working
                // see type of message -> only if FINDSUCCESSOR
                System.out.println("Unreachable " + address.getAddress() + ":" + address.getPort());
                this.parent.removeNode(address);
            }
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
        sb.append(requestOrigin.getHostString()).append(" "); // Origin's IP
        sb.append(requestOrigin.getPort()); // Origin's Port

        return sb.toString();
    }

    /**
     * Delegates the work of finding the successor of id, by sending a message to destination
     * @param requestOrigin Contains the IP and Port of the ChordNode that wants to find the successor of id
     * @param requestedId Id that the origin Node requested
     * @param destination Contains the IP and Port of the ChordNode that will be receiving the request
     */
    public String[] sendFindSuccessorMessage(InetSocketAddress requestOrigin, int requestedId,
                                            InetSocketAddress destination) {
        String message = this.createFindSuccessorMessage(requestOrigin, requestedId);
        this.sendMessage(destination, message);

        if (!this.parent.getAddress().getHostString().equals(requestOrigin.getHostString()))  // This node didn't request the id
            return null; // Delegates work, and returns

        synchronized (this.parent) {
            try {
                this.parent.wait(this.timeout*2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Message currentMessage : this.messageQueue) {
                String [] messageReceived = currentMessage.getArguments();
                if (messageReceived[0].equals("SUCCESSORFOUND") && messageReceived[1].equals(Integer.toString(requestedId))) { // Answer to request made
                    this.messageQueue.remove(currentMessage);
                    return messageReceived;
                }
            }
        }

        return null;
    }

    /**
     * Builds the message with the requested information about the Node with requestedId
     * @param requestedId
     * @param successorId
     * @param successorNodeInfo
     * @return
     */
    public String createSuccessorFoundMessage(int requestedId, int successorId, InetSocketAddress successorNodeInfo) {
        // Message format: SUCCESSORFOUND <requestedId> <successorId> <successorNodeIp> <successorNodePort>
        StringBuilder sb = new StringBuilder();
        sb.append("SUCCESSORFOUND").append(" ");
        sb.append(requestedId).append(" ");
        sb.append(successorId).append(" ");
        sb.append(successorNodeInfo.getHostString()).append(" ");
        sb.append(successorNodeInfo.getPort()).append(" ");
        return sb.toString();
    }

    /**
     * Sends a message with the information requested to the Node that made the request to find the successor
     * @param requestOrigin
     * @param requestedId
     * @param successorNodeInfo
     */
    public void sendSuccessorFound(InetSocketAddress requestOrigin, int requestedId, int successorId, InetSocketAddress successorNodeInfo) {
        String message = this.createSuccessorFoundMessage(requestedId, successorId, successorNodeInfo);
        this.sendMessage(requestOrigin, message);
    }

    /**
     * Builds the joining message, used when a peer joins the chord
     * @return
     */
    public String createJoiningMessage() {
        // Message format: JOINING <newNodeId> <newNodeIp> <newNodePort>
        StringBuilder sb = new StringBuilder();
        sb.append("JOINING").append(" ");
        sb.append(this.parent.getId()).append(" ");
        sb.append(this.parent.getAddress().getHostString()).append(" ");
        sb.append(this.parent.getAddress().getPort());
        return sb.toString();
    }

    /**
     * Creates and sends a joining message to the known node
     * @param knownNode
     */
    public void sendJoiningMessage(InetSocketAddress knownNode) {
        String message = this.createJoiningMessage();
        this.sendMessage(knownNode, message);
    }

    /**
     *
     * @param successor
     * @return
     */
    private String createWelcomeMessage(int successorId, InetSocketAddress successor) {
        // Message format: WELCOME <successorId> <successorIP> <successorPort>
        StringBuilder sb = new StringBuilder();
        sb.append("WELCOME").append(" ");
        sb.append(successorId).append(" ");
        sb.append(successor.getHostString()).append(" ");
        sb.append(successor.getPort());
        return sb.toString();
    }

    /**
     *
     */
    public void sendWelcomeMessage(InetSocketAddress newNode, int successorId, InetSocketAddress successor) {
        String message = this.createWelcomeMessage(successorId, successor);
        this.sendMessage(newNode, message);
    }

    private String createGetPredecessorMessage(InetSocketAddress originInfo) {
        // Message format: GETPREDECESSOR <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("GETPREDECESSOR").append(" ");
        sb.append(originInfo.getHostString()).append(" ");
        sb.append(originInfo.getPort());
        return sb.toString();
    }

    public void sendGetPredecessorMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = this.createGetPredecessorMessage(origin);
        this.sendMessage(destination, message);
    }

    private String createPredecessorMessage(int predecessorId, InetSocketAddress predecessorAddress) {
        // Message format: PREDECESSOR <predecessorId> <predecessorIP> <predecessorPort>
        StringBuilder sb = new StringBuilder();
        sb.append("PREDECESSOR").append(" ");
        sb.append(predecessorId).append(" ");
        sb.append(predecessorAddress.getHostString()).append(" ");
        sb.append(predecessorAddress.getPort());
        return sb.toString();
    }

    public void sendPredecessorMessage(int predecessorId, InetSocketAddress predecessorAddress, InetSocketAddress destination) {
        String message = this.createPredecessorMessage(predecessorId, predecessorAddress);
        this.sendMessage(destination, message);
    }

    public void sendNullPredecessorMessage(InetSocketAddress destination) {
        String message = "PREDECESSOR NULL";
        this.sendMessage(destination, message);
    }

    private String createNotifyMessage(int originId, InetSocketAddress origin) {
        // Message format: NOTIFY <originId> <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("NOTIFY").append(" ");
        sb.append(originId).append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    public void sendNotifyMessage(int originId, InetSocketAddress origin, InetSocketAddress destination) {
        String message = this.createNotifyMessage(originId, origin);
        this.sendMessage(destination, message);
    }

    private String createPingMessage(InetSocketAddress origin) {
        // Message format: PING <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("PING").append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    public boolean sendPingMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createPingMessage(origin);
        this.sendMessage(destination, message);

        synchronized (this.parent) {
            try {
                this.parent.wait(this.timeout*2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Message currentMessage : this.messageQueue) {
                String [] messageReceived = currentMessage.getArguments();
                if (messageReceived[0].equals("PONG")) { // Answer to request made
                    this.messageQueue.remove(currentMessage);
                    return true;
                }
            }

            return false;
        }
    }

    private String createPongMessage(InetSocketAddress origin) {
        // Message format: PONG <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("PONG").append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    public void sendPongMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createPongMessage(origin);
        this.sendMessage(destination, message);
    }

    private String createFindSuccessorListMessage(InetSocketAddress origin) {
        // Message format: FINDSUCCESSORLIST <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("FINDSUCCESSORLIST").append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    public void sendFindSuccessorListMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createFindSuccessorListMessage(origin);
        this.sendMessage(destination, message);
    }

    private String createSuccessorListMessage(InetSocketAddress origin) {
        ArrayList<NodePair<Integer, InetSocketAddress>> successorList = this.parent.getSuccessorList();
        // Message format: SUCCESSORLIST <successorList>
        StringBuilder sb = new StringBuilder();
        sb.append("SUCCESSORLIST").append(" ");
        
        for (NodePair<Integer, InetSocketAddress> successor : successorList) {
            sb.append(successor.getKey()).append(" ");
            sb.append(successor.getValue().getHostString()).append(" ");
            sb.append(successor.getValue().getPort()).append(" ");
        }

        return sb.toString();
    }

    public void sendSuccessorListMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createSuccessorListMessage(origin);
        this.sendMessage(destination, message);
    }

    public synchronized void addMessageQueue(Message message) {
        this.messageQueue.add(message);
    }

}
