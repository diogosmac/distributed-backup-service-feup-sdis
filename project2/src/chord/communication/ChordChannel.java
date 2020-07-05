package chord.communication;

import chord.ChordNode;
import chord.NodePair;
import chord.Utils;
import utils.MyUtils;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

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
     *
     * @param parent ChordNode that this channel is linked to
     */
    public ChordChannel(ChordNode parent) {
        this.parent = parent;
        messageQueue = new ConcurrentLinkedQueue<>();
        this.open(this.parent.getAddress().getPort());
    }

    /**
     * Opens the SSLServerSocket through which the ChordChannel will function
     *
     * @param port Number of the port in which the socket will be opened
     */
    public void open(int port) {
        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "../keys/keystore" + (this.parent.getID() % 3 + 1));
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
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {

        SSLSocket socket = null;
        while (true) {
            try {
                socket = (SSLSocket) serverSocket.accept();
                InputStream is = socket.getInputStream();

                DataInputStream dis = new DataInputStream(is);
                int length = dis.readInt();

                byte[] messageBytes = new byte[length];
                dis.readFully(messageBytes, 0, messageBytes.length);
                String message = MyUtils.convertByteArrayToString(messageBytes);

                handleMessage(message);

                dis.close();
                is.close();
            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
                System.err.println("Node disconnected while reading from socket");
            }
        }
    }

    /**
     * @param message The message that was received
     */
    public void handleMessage(String message) {
        new MessageHandler(message, this, this.parent).start();
    }

    /**
     * Sends a message through the ChordChannel
     *
     * @param address Address to which the message should be sent
     * @param message Message to be sent
     */
    public void sendMessage(InetSocketAddress address, String message) {

        if (address.equals(this.parent.getAddress())) {
            handleMessage(message);
            return;
        }

        try {

            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
            socket.connect(address, timeout);
            OutputStream os = socket.getOutputStream();

            DataOutputStream dos = new DataOutputStream(os);
            byte[] messageBytes = MyUtils.convertStringToByteArray(message);
            dos.writeInt(messageBytes.length);
            dos.write(messageBytes, 0, messageBytes.length);
            dos.flush();
        } catch (Exception e) {
            // error in communication, maybe successor stopped working
            // see type of message -> only if FINDSUCCESSOR
            System.out.println("Unreachable " + address.getAddress() + ":" + address.getPort());
            this.parent.removeNode(address);
        }
    }

    /**
     * Creates the findSuccessor message to be later sent
     *
     * @param requestOrigin Contains the IP and Port of the ChordNode that wants to find the successor of id
     * @param requestedId   Id that the origin Node requested
     * @return Message to be sent, delegating the findSuccessor work to other node
     */
    private String createFindSuccessorMessage(InetSocketAddress requestOrigin, int requestedId) {
        // Message format: FINDSUCCESSOR <requestedId> <originIP> <originPort>

        return "FINDSUCCESSOR" + " " +                 // Header
                requestedId + " " +                     // Id requested
                requestOrigin.getHostString() + " " +   // Origin's IP
                requestOrigin.getPort();
    }

    /**
     * Delegates the work of finding the successor of id, by sending a message to destination
     *
     * @param requestOrigin Contains the IP and Port of the ChordNode that wants to find the successor of id
     * @param requestedId   Id that the origin Node requested
     * @param destination   Contains the IP and Port of the ChordNode that will be receiving the request
     */
    public String[] sendFindSuccessorMessage(InetSocketAddress requestOrigin, int requestedId,
                                             InetSocketAddress destination) {
        String message = this.createFindSuccessorMessage(requestOrigin, requestedId);
        this.sendMessage(destination, message);

        if (!this.parent.getAddress().getHostString().equals(requestOrigin.getHostString()))  // This node didn't request the id
            return null; // Delegates work, and returns

        synchronized (this.parent) {
            try {
                this.parent.wait(this.timeout * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Message currentMessage : this.messageQueue) {
                String[] messageReceived = currentMessage.getArguments();
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
     *
     * @param requestedId       id for which the successor was requested
     * @param successorId       id of the successor
     * @param successorNodeInfo address of the successor
     * @return Built message
     */
    public String createSuccessorFoundMessage(int requestedId, int successorId, InetSocketAddress successorNodeInfo) {
        // Message format: SUCCESSORFOUND <requestedId> <successorId> <successorNodeIp> <successorNodePort>
        return "SUCCESSORFOUND" + " " +
                requestedId + " " +
                successorId + " " +
                successorNodeInfo.getHostString() + " " +
                successorNodeInfo.getPort() + " ";
    }

    /**
     * Sends a message with the information requested to the Node that made the request to find the successor
     *
     * @param requestOrigin     address of the node which sent the request
     * @param requestedId       id for which the successor was requested
     * @param successorNodeInfo address of the successor
     */
    public void sendSuccessorFound(InetSocketAddress requestOrigin, int requestedId, int successorId, InetSocketAddress successorNodeInfo) {
        String message = this.createSuccessorFoundMessage(requestedId, successorId, successorNodeInfo);
        this.sendMessage(requestOrigin, message);
    }

    /**
     * Builds the joining message, used when a peer joins the chord
     *
     * @return the joining message
     */
    public String createJoiningMessage() {
        // Message format: JOINING <newNodeId> <newNodeIp> <newNodePort>
        return "JOINING" + " " +
                this.parent.getID() + " " +
                this.parent.getAddress().getHostString() + " " +
                this.parent.getAddress().getPort();
    }

    /**
     * Creates and sends a joining message to the known node
     *
     * @param knownNode address of a known node that is part of the chord network
     */
    public void sendJoiningMessage(InetSocketAddress knownNode) {
        String message = this.createJoiningMessage();
        this.sendMessage(knownNode, message);
    }

    /**
     * Builds the welcome message, used to acknowledge a new peer joining the chord
     *
     * @param successorId      id of the successor
     * @param successorAddress address of the successor
     * @return the welcome message
     */
    private String createWelcomeMessage(int successorId, InetSocketAddress successorAddress) {
        // Message format: WELCOME <successorId> <successorIP> <successorPort>
        return "WELCOME" + " " +
                successorId + " " +
                successorAddress.getHostString() + " " +
                successorAddress.getPort();
    }

    /**
     * Creates and sends a welcome message, to acknowledge a new node that joins the chord
     *
     * @param newNode          address of the node that joined the chord
     * @param successorId      id of the successor
     * @param successorAddress address of the successor
     */
    public void sendWelcomeMessage(InetSocketAddress newNode, int successorId, InetSocketAddress successorAddress) {
        String message = this.createWelcomeMessage(successorId, successorAddress);
        this.sendMessage(newNode, message);
    }

    /**
     * Builds a message that requests the predecessor of a node
     *
     * @param originInfo address of the message's sender
     * @return the getpredecessor message
     */
    private String createGetPredecessorMessage(InetSocketAddress originInfo) {
        // Message format: GETPREDECESSOR <originIP> <originPort>
        return "GETPREDECESSOR" + " " +
                originInfo.getHostString() + " " +
                originInfo.getPort();
    }

    /**
     * Sends a message that requests the predecessor of the sender node
     *
     * @param origin      address of the sender node
     * @param destination destination address for the message
     */
    public void sendGetPredecessorMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = this.createGetPredecessorMessage(origin);
        this.sendMessage(destination, message);
    }

    /**
     * Builds a message that replies to a getpredecessor request
     *
     * @param predecessorId      id of the predecessor
     * @param predecessorAddress address of the predecessor
     * @return the predecessor message
     */
    private String createPredecessorMessage(int predecessorId, InetSocketAddress predecessorAddress) {
        // Message format: PREDECESSOR <predecessorId> <predecessorIP> <predecessorPort>
        return "PREDECESSOR" + " " +
                predecessorId + " " +
                predecessorAddress.getHostString() + " " +
                predecessorAddress.getPort();
    }

    /**
     * Sends a message to reply (positively) to a getpredecessor request
     *
     * @param predecessorId      id of the predecessor
     * @param predecessorAddress address of the predecessor
     * @param destination        address to which the message must be sent
     */
    public void sendPredecessorMessage(int predecessorId, InetSocketAddress predecessorAddress, InetSocketAddress destination) {
        String message = this.createPredecessorMessage(predecessorId, predecessorAddress);
        this.sendMessage(destination, message);
    }

    /**
     * Sends a message to reply (negatively) to a getpredecessor request
     *
     * @param destination address to which the message must be sent
     */
    public void sendNullPredecessorMessage(InetSocketAddress destination) {
        String message = "PREDECESSOR NULL";
        this.sendMessage(destination, message);
    }

    /**
     * Creates a notify message
     *
     * @param originId id of the sender
     * @param origin   address of the sender
     * @return the notify message
     */
    private String createNotifyMessage(int originId, InetSocketAddress origin) {
        // Message format: NOTIFY <originId> <originIP> <originPort>
        return "NOTIFY" + " " +
                originId + " " +
                origin.getHostString() + " " +
                origin.getPort();
    }

    /**
     * Sends a notify message, which informs a node that the sender is its predecessor
     *
     * @param originId    id of the sender
     * @param origin      address of the sender
     * @param destination address to which the message must be sent
     */
    public void sendNotifyMessage(int originId, InetSocketAddress origin, InetSocketAddress destination) {
        String message = this.createNotifyMessage(originId, origin);
        this.sendMessage(destination, message);
    }

    /**
     * Creates a ping message
     *
     * @param origin address of the sender
     * @return the ping message
     */
    private String createPingMessage(InetSocketAddress origin) {
        // Message format: PING <originIP> <originPort>
        return "PING" + " " +
                origin.getHostString() + " " +
                origin.getPort();
    }

    /**
     * Sends a ping message, which is part of the mechanism to ensure that all known nodes are active
     *
     * @param origin      address of the sender
     * @param destination address of the receiver
     * @return the ping message
     */
    public boolean sendPingMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createPingMessage(origin);
        this.sendMessage(destination, message);

        synchronized (this.parent) {
            try {
                this.parent.wait(this.timeout * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Message currentMessage : this.messageQueue) {
                String[] messageReceived = currentMessage.getArguments();
                if (messageReceived[0].equals("PONG")) { // Answer to request made
                    this.messageQueue.remove(currentMessage);
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Creates a pong message
     *
     * @param origin address of the sender
     * @return the pong message
     */
    private String createPongMessage(InetSocketAddress origin) {
        // Message format: PONG <originIP> <originPort>
        return "PONG" + " " +
                origin.getHostString() + " " +
                origin.getPort();
    }

    /**
     * Sends a pong message, which is part of the mechanism to ensure that all known nodes are active
     *
     * @param origin      address of the sender
     * @param destination address of the receiver
     * @return the pong message
     */
    public void sendPongMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createPongMessage(origin);
        this.sendMessage(destination, message);
    }

    /**
     * Creates a findsuccessorlist message
     *
     * @param origin address of the sender
     * @return the findsuccessorlist message
     */
    private String createFindSuccessorListMessage(InetSocketAddress origin) {
        // Message format: FINDSUCCESSORLIST <originIP> <originPort>
        return "FINDSUCCESSORLIST" + " " +
                origin.getHostString() + " " +
                origin.getPort();
    }

    /**
     * Sends a findsuccessorlist message, which is part of the mechanism used to
     * maintain the finger tables updated with fault tolerance
     *
     * @param origin      the address of the sender
     * @param destination the address of the receiver
     */
    public void sendFindSuccessorListMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createFindSuccessorListMessage(origin);
        this.sendMessage(destination, message);
    }

    /**
     * Creates a successorlist message
     *
     * @return the successorlist message
     */
    private String createSuccessorListMessage() {
        CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> successorList = this.parent.getSuccessorList();
        // Message format: SUCCESSORLIST <successorList>
        StringBuilder sb = new StringBuilder();
        sb.append("SUCCESSORLIST").append(" ");

        for (NodePair<Integer, InetSocketAddress> successor : successorList) {
            if (successor.getKey() == null)
                continue;
            sb.append(successor.getKey()).append(" ");
            sb.append(successor.getValue().getHostString()).append(" ");
            sb.append(successor.getValue().getPort()).append(" ");
        }

        return sb.toString();
    }

    /**
     * Sends a successorlist message, which is part of the mechanism used to
     * maintain the finger tables updated with fault tolerance
     *
     * @param destination the address of the receiver
     */
    public void sendSuccessorListMessage(InetSocketAddress destination) {
        String message = createSuccessorListMessage();
        this.sendMessage(destination, message);
    }

    /**
     * Adds a message to the message queue
     *
     * @param message message to be added
     */
    public synchronized void addMessageQueue(Message message) {
        this.messageQueue.add(message);
    }

    public void sendPutchunkMessage(String fileID, int chunkNumber, int replicationDegree, String hash, byte[] data,
                                    InetSocketAddress initiator, InetSocketAddress firstSuccessor,
                                    InetSocketAddress destination) {
        String message = createPutchunkMessage(fileID, chunkNumber, replicationDegree, hash, initiator, firstSuccessor, data);
        this.sendMessage(destination, message);
    }

    private String createPutchunkMessage(String fileID, int chunkNumber, int replicationDegree, String hash,
                                         InetSocketAddress initiator, InetSocketAddress firstSuccessor, byte[] data) {
        return "PUTCHUNK" + " " +
                fileID + " " +
                chunkNumber + " " +
                replicationDegree + " " +
                initiator.getHostString() + " " +
                initiator.getPort() + " " +
                firstSuccessor.getHostString() + " " +
                firstSuccessor.getPort() + " " +
                hash + " " +
                MyUtils.convertByteArrayToString(data);
    }

    private String createUpdateFileReplicationDegreeMessage(String fileID, int chunkNumber, int realRD) {
        // Message format: UPDATERD <fileID> <chunkNumber> <realRD>
        return "UPDATERD" + " " +
                fileID + " " +
                chunkNumber + " " +
                realRD;
    }

    protected void sendUpdateFileReplicationDegree(String fileID, int chunkNumber, int missingReplicas, InetSocketAddress destination) {
        String message = createUpdateFileReplicationDegreeMessage(fileID, chunkNumber, missingReplicas);
        this.sendMessage(destination, message);
    }

    private String createDeleteMessage(InetSocketAddress origin, String fileID) {
        // Message format: DELETE <originIP> <originPort> <fileID>
        return "DELETE" + " " +
                origin.getHostString() + " " +
                origin.getPort() + " " +
                fileID;
    }

    public void sendDeleteMessage(String fileID, InetSocketAddress destination) {
        String message = this.createDeleteMessage(this.parent.getAddress(), fileID);
        this.sendMessage(destination, message);
    }

    private String createEnsureRDMessage(InetSocketAddress initiator, InetSocketAddress firstSuccessor, String fileID, int chunkNumber) {
        // Message format: ENSURERD <initiatorIP> <initiatorPort> <firstSuccessorIP> <firstSuccessorPort> <hash> <fileID> <chunkNumber>
        String hash = MyUtils.sha256(firstSuccessor.getHostString() + fileID + chunkNumber + System.currentTimeMillis());
        return "ENSURERD" + " " +
                initiator.getHostString() + " " +
                initiator.getPort() + " " +
                firstSuccessor.getHostString() + " " +
                firstSuccessor.getPort() + " " +
                hash + " " +
                fileID + " " +
                chunkNumber;
    }

    public void sendEnsureRDMessage(InetSocketAddress initiator, InetSocketAddress firstSuccessor, String fileID, int chunkNumber, InetSocketAddress destination) {
        String message = this.createEnsureRDMessage(initiator, firstSuccessor, fileID, chunkNumber);
        this.sendMessage(destination, message);
    }

    private String createSaveChunkMessage(String fileID, int chunkNumber,
                                          InetSocketAddress initiator, InetSocketAddress firstSuccessor, byte[] data) {
        // Message format: SAVECHUNK <fileID> <chunkNumber> <hash> <initiatorIP> <initiatorPort> <firstSuccessorIP> <firstSuccessorPort> <data>
        Integer hash = Utils.hash("Savechunk" + fileID + chunkNumber + System.currentTimeMillis());
        return "SAVECHUNK" + " " +
                fileID + " " +
                chunkNumber + " " +
                hash + " " +
                initiator.getHostString() + " " +
                initiator.getPort() + " " +
                firstSuccessor.getHostString() + " " +
                firstSuccessor.getPort() + " " +
                MyUtils.convertByteArrayToString(data);
    }

    public void sendSaveChunkMessage(String fileID, int chunkNumber, InetSocketAddress initiator, InetSocketAddress firstSuccessor,
                                     byte[] data, InetSocketAddress destination) {
        String message = this.createSaveChunkMessage(fileID, chunkNumber, initiator, firstSuccessor, data);
        this.sendMessage(destination, message);
    }

    private String createGetChunkMessage(InetSocketAddress initiator, InetSocketAddress firstSuccessor, String fileID, int chunkNumber) {
        // Message format: GETCHUNK <initiatorIP> <initiatorPort> <firstSuccessorIP> <firstSuccessorPort> <fileID> <chunkNumber> <hash>
        Integer hash = Utils.hash("Getchunk" + fileID + chunkNumber + System.currentTimeMillis());
        return "GETCHUNK" + " " +
                initiator.getHostString() + " " +
                initiator.getPort() + " " +
                firstSuccessor.getHostString() + " " +
                firstSuccessor.getPort() + " " +
                fileID + " " +
                chunkNumber + " " +
                hash;
    }

    public void sendGetChunkMessage(InetSocketAddress initiator, InetSocketAddress firstSuccessor, String fileID, int chunkNumber, InetSocketAddress destination) {
        String message = this.createGetChunkMessage(initiator, firstSuccessor, fileID, chunkNumber);
        this.sendMessage(destination, message);
    }

    private String createChunkMessage(String fileID, int chunkNumber, byte[] data) {
        // Message format: CHUNK <fileID> <chunkNumber> <data>
        return "CHUNK" + " " +
                fileID + " " +
                chunkNumber + " " +
                MyUtils.convertByteArrayToString(data);
    }

    public void sendChunkMessage(String fileID, int chunkNumber, byte[] data, InetSocketAddress destination) {
        String message = this.createChunkMessage(fileID, chunkNumber, data);
        this.sendMessage(destination, message);
    }

    private String createChunkSavedMessage(String fileID, int chunkNumber) {
        // Message format: CHUNKSAVED <fileID> <chunkNumber>
        return "CHUNKSAVED" + " " +
                fileID + " " +
                chunkNumber;
    }

    public void sendChunkSavedMessage(String fileID, int chunkNumber, InetSocketAddress initiatorAddress) {
        String message = this.createChunkSavedMessage(fileID, chunkNumber);
        this.sendMessage(initiatorAddress, message);
    }
}
