package chord.communication;

import utils.MyUtils;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import chord.ChordNode;
import chord.NodePair;
import chord.Utils;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.Buffer;
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
     * Opens the SSLServerSocket through which the ChordChannel will
     * function
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
    @Override
    public void run() {

        boolean ended = false;
        SSLSocket socket = null;
        while (!ended) {
            try {
                socket = (SSLSocket) serverSocket.accept();
                InputStream is = socket.getInputStream();

                DataInputStream dis = new DataInputStream(is);
                int length = dis.readInt();

                byte[] messageBytes = new byte[length];
                dis.readFully(messageBytes, 0, messageBytes.length);
                String message = MyUtils.convertByteArrayToString(messageBytes);

                handleMessage(socket, message);

                dis.close();
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Node disconnected while reading from socket");
                ended = true;
            }
        }

        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param socket
     * @param message
     */
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
     * @param requestOrigin Contains the IP and Port of the ChordNode that wants to find the successor of id
     * @param requestedId Id that the origin Node requested
     * @return Message to be sent, delegating the findSuccessor work to other node
     */
    private String createFindSuccessorMessage(InetSocketAddress requestOrigin, int requestedId) {
        // Message format: FINDSUCCESSOR <requestedId> <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("FINDSUCCESSOR").append(" ");                 // Header
        sb.append(requestedId).append(" ");                     // Id requested
        sb.append(requestOrigin.getHostString()).append(" ");   // Origin's IP
        sb.append(requestOrigin.getPort());                     // Origin's Port

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
     * @return Built message
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
        sb.append(this.parent.getID()).append(" ");
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
     * @param newNode
     * @param successorId
     * @param successor
     */
    public void sendWelcomeMessage(InetSocketAddress newNode, int successorId, InetSocketAddress successor) {
        String message = this.createWelcomeMessage(successorId, successor);
        this.sendMessage(newNode, message);
    }

    /**
     * 
     * @param originInfo
     * @return
     */
    private String createGetPredecessorMessage(InetSocketAddress originInfo) {
        // Message format: GETPREDECESSOR <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("GETPREDECESSOR").append(" ");
        sb.append(originInfo.getHostString()).append(" ");
        sb.append(originInfo.getPort());
        return sb.toString();
    }

    /**
     * 
     * @param origin
     * @param destination
     */
    public void sendGetPredecessorMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = this.createGetPredecessorMessage(origin);
        this.sendMessage(destination, message);
    }

    /**
     * 
     * @param predecessorId
     * @param predecessorAddress
     * @return
     */
    private String createPredecessorMessage(int predecessorId, InetSocketAddress predecessorAddress) {
        // Message format: PREDECESSOR <predecessorId> <predecessorIP> <predecessorPort>
        StringBuilder sb = new StringBuilder();
        sb.append("PREDECESSOR").append(" ");
        sb.append(predecessorId).append(" ");
        sb.append(predecessorAddress.getHostString()).append(" ");
        sb.append(predecessorAddress.getPort());
        return sb.toString();
    }

    /**
     * 
     * @param predecessorId
     * @param predecessorAddress
     * @param destination
     */
    public void sendPredecessorMessage(int predecessorId, InetSocketAddress predecessorAddress, InetSocketAddress destination) {
        String message = this.createPredecessorMessage(predecessorId, predecessorAddress);
        this.sendMessage(destination, message);
    }

    /**
     * 
     * @param destination
     */
    public void sendNullPredecessorMessage(InetSocketAddress destination) {
        String message = "PREDECESSOR NULL";
        this.sendMessage(destination, message);
    }

    /**
     * 
     * @param originId
     * @param origin
     * @return
     */
    private String createNotifyMessage(int originId, InetSocketAddress origin) {
        // Message format: NOTIFY <originId> <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("NOTIFY").append(" ");
        sb.append(originId).append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    /**
     * 
     * @param originId
     * @param origin
     * @param destination
     */
    public void sendNotifyMessage(int originId, InetSocketAddress origin, InetSocketAddress destination) {
        String message = this.createNotifyMessage(originId, origin);
        this.sendMessage(destination, message);
    }

    /**
     * 
     * @param origin
     * @return
     */
    private String createPingMessage(InetSocketAddress origin) {
        // Message format: PING <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("PING").append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    /**
     * 
     * @param origin
     * @param destination
     * @return
     */
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

    /**
     * 
     * @param origin
     * @return
     */
    private String createPongMessage(InetSocketAddress origin) {
        // Message format: PONG <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("PONG").append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    /**
     * 
     * @param origin
     * @param destination
     */
    public void sendPongMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createPongMessage(origin);
        this.sendMessage(destination, message);
    }

    /**
     * 
     * @param origin
     * @return
     */
    private String createFindSuccessorListMessage(InetSocketAddress origin) {
        // Message format: FINDSUCCESSORLIST <originIP> <originPort>
        StringBuilder sb = new StringBuilder();
        sb.append("FINDSUCCESSORLIST").append(" ");
        sb.append(origin.getHostString()).append(" ");
        sb.append(origin.getPort());
        return sb.toString();
    }

    /**
     * 
     * @param origin
     * @param destination
     */
    public void sendFindSuccessorListMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createFindSuccessorListMessage(origin);
        this.sendMessage(destination, message);
    }

    /**
     * 
     * @param origin
     * @return
     */
    private String createSuccessorListMessage(InetSocketAddress origin) {
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
     * 
     * @param origin
     * @param destination
     */
    public void sendSuccessorListMessage(InetSocketAddress origin, InetSocketAddress destination) {
        String message = createSuccessorListMessage(origin);
        this.sendMessage(destination, message);
    }

    /**
     * 
     * @param message
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

    public void sendDeleteMessage(InetSocketAddress origin, String fileID, InetSocketAddress destination) {
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
