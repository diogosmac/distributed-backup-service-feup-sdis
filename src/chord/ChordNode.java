package chord;

import peer.Peer;
import storage.Chunk;
import storage.SavedFile;
import utils.MyUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import chord.communication.ChordChannel;

/**
 * Chord Node
 * 
 * Class used to represent a node in the chord's network
 */
public class ChordNode {

    /**
	 * The node's unique identifier
     */
    private final Integer id;

    /**
     * Number of bits of the addressing space
     */
    private final int m = Utils.m;

    /**
     * The successorList's size, r < m
     */
    private int r;

    /**
     * The node's address
     */
    private final InetSocketAddress address;

    /**
     * The node's predecessor
     */
    private NodePair<Integer, InetSocketAddress> predecessor;

    /**
     * The node's finger table. Stores m entries of other nodes in the ring
     */
    private final FingerTable fingerTable;

    /**
     * The finger (index) of the finger table entry to fix
     */
    private int finger = -1;

    /**
	 * The list of its successors, size r
	 */
    private CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> successorList;

    /**
     * Thread Executor used for chord maintainer
     */
    private ScheduledThreadPoolExecutor executor;

    /**
     * Chord channel used for communication
     */
    private ChordChannel channel = null;

    /**
     * Peer supported by this chord node
     */
    private Peer peer;

    private List<String> protocolPropagationWall;

    public static void main(String[] args) {

        int port;
        ChordNode node = null;

        // First node is joining the network
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
            try {
                node = new ChordNode(port);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        // Other node is joining
        } else if (args.length == 3) {
                port = Integer.parseInt(args[0]);
                InetSocketAddress thisAddress = new InetSocketAddress("94.61.206.209", port);
                InetSocketAddress knownAddress = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
                node = new ChordNode(thisAddress, knownAddress);
        // You dumbass
        } else {
            System.out.println("Usage: java ChordNode <node-id> [ <connection-address> <connection-port> ]");
            return;
        }

        System.out.println("Started chord node");
        System.out.println("\tID: " + node.getID());
        System.out.println("\tAddress: " + node.getAddress());

        Timer timer = new Timer(); 
        ChordNodePrinter printer = new ChordNodePrinter(node);
        timer.schedule(printer, 1000, 5000);
    }
    
    public ChordNode(int port) throws UnknownHostException {
        this(new InetSocketAddress("94.61.206.209", port));
    }

    public ChordNode(InetSocketAddress address) {
        this.id = Utils.hash(address.getHostString() + ":" + address.getPort());
        this.r = (int) Math.ceil(this.m / 3.0);
        this.fingerTable = new FingerTable(m);
        this.address = address;
        this.peer = new Peer(this);
        this.protocolPropagationWall = Collections.synchronizedList(new ArrayList<>());

        // create the chord ring
        this.create();

        // creates the ChordNode's scheduled thread executor
        this.createExecutor();

        // start chord communication channel thread
        this.startChannel();

        // start chord maintainer thread
        this.startMaintainer();
    }

    /**
     * Constructor with IP address
     * @param address IP address of this Chord node
     * @param knownAddress IP address of a node on the Chord 'network' to be joined
     */
    public ChordNode(InetSocketAddress address, InetSocketAddress knownAddress) {

        this.id = Utils.hash(address.getHostString() + ":" + address.getPort());
        this.r = (int) Math.ceil(this.m / 3.0);
        this.fingerTable = new FingerTable(m);
        this.address = address;
        this.peer = new Peer(this);
        this.protocolPropagationWall = Collections.synchronizedList(new ArrayList<>());

        // creates the ChordNode's scheduled thread executor
        this.createExecutor();

        // start chord communication channel thread
        this.startChannel();

        // start chord maintainer thread
        this.startMaintainer();

        // get known address node's identifier
        this.join(knownAddress);
    }

    /**
     * When the first chord node enters the ring we need to create a new Chord ring
     */
    private void create() {
        // no predecessor
        this.setPredecessor(new NodePair<>(null, null));
        // successor is itself
        CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> successorList = new CopyOnWriteArrayList<>();
        NodePair<Integer, InetSocketAddress> successor = new NodePair<>(this.getID(), this.getAddress());
        successorList.add(successor);
        this.setSuccessorList(successorList);
        this.fingerTable.setNodePair(0, successor);
    }

    /**
     * Join a Chord ring containing any known node 'node'. This method asks 'node'
     * to find the immediate successor of the newly joined node. By itself this method
     * does not make the rest of the network aware of 'chord'
     * 
     * @param node any know node in the chord network
     */
    protected void join(InetSocketAddress node) {
        // no predecessor
        this.setPredecessor(new NodePair<>(null, null));
        // set successor list to empty
        CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> successorList = new CopyOnWriteArrayList<>();
        successorList.add(new NodePair<>(null, null));
        this.setSuccessorList(successorList);
        // successor is found by 'node'
        // send message no 'node' se he can find our successor
        this.channel.sendJoiningMessage(node);
    }

    /**
     * Create scheduled thread executor and limit to two threads
     */
    private void createExecutor() {
        this.executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    }

    /**
	 * Starts the maintenance routine
	 */
	private void startMaintainer() {
        // perform maintenance every half second 1.5 seconds after starting
        this.executor.scheduleWithFixedDelay(new ChordMaintainer(this), 1500, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * Starts the Chord communication channel
     */
    private void startChannel() {
        this.channel = new ChordChannel(this);
        this.executor.execute(this.channel);
    }

    /**
     * @return the fingerTable
     */
    public FingerTable getFingerTable() {
        return fingerTable;
    }
    
    /**
     * @return the predecessor
     */
    public NodePair<Integer, InetSocketAddress> getPredecessor() {
        return predecessor;
    }

    /**
     * @param predecessor the predecessor to set
     */
    public void setPredecessor(NodePair<Integer, InetSocketAddress> predecessor) {
        this.predecessor = predecessor;
    }

    /**
     * @return the successorList
     */
    public CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> getSuccessorList() {
        return successorList;
    }

    /**
     * @param successorList the successorList to set
     */
    public void setSuccessorList(CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> successorList) {
        this.successorList = successorList;
    }

    /**
     * @return the id
     */
    public Integer getID() {
        return id;
    }

    /**
     * @return the address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * @return the m
     */
    public int getM() {
        return m;
    }

    /**
     * @return the finger
     */
    public int getFinger() {
        return finger;
    }
    
    /**
     * @param finger the finger to set
     */
    public void setFinger(int finger) {
        this.finger = finger;
    }

    /**
     * @return chord node's successor, which happens to be
     * the first element of 'successorList' and also the first
     * element of 'fingerTable'. In case of failure and node's successor is
     * 'null', we get the next sucessor. In the rare case that there are none
     * we return the current node.
     */
    public NodePair<Integer, InetSocketAddress> getSuccessor() {
        for (NodePair<Integer, InetSocketAddress> successor : this.successorList) {
            if (successor.getKey() != null)
                return successor;
        }

        return new NodePair<Integer, InetSocketAddress>(this.getID(), this.getAddress());
    }

    /**
     * Gets node's successor id
     */
    public int getSuccessorId() {
        return this.getSuccessor().getKey();
    }

    /**
     * Gets node's successor address
     */
    public InetSocketAddress getSuccessorAddress() {
        return this.getSuccessor().getValue();
    }

    /**
     * Set 'node' as node's new successor
     * @param node new chord's successor
     */
    public void setSuccessor(NodePair<Integer, InetSocketAddress> node) {
        this.fingerTable.setNodePair(0, node);
        this.successorList.add(0, node);

        if (this.successorList.size() > this.r)
            this.successorList.remove(this.r);
    }

    /**
     * Set 'node' as node's new successor
     * 
     * @param node new chord's successor
     */
    public void addSuccessor(NodePair<Integer, InetSocketAddress> node) {
        if (this.successorList.size() < r)
            this.successorList.add(node);
    }

    /**
     * Given a node identifier, retrieve the closest preceding node, i.e. the
     * node with the greatest identifier that is lower than 'id'. This is achieved
     * by an enhancement of the original method, i.e. cross referencing the finger
     * table with the successor list.
     */
    public void getSuccessorsPredecessor() {
        NodePair<Integer, InetSocketAddress> successor = this.fingerTable.getFirstNode();
        // send request to successor.getValue() (InetSocketAddress) for its predecessor
        // build pair and return it
        this.channel.sendGetPredecessorMessage(this.getAddress(), successor.getValue());
    }

    /**
     * Set entry in finger table given finger (index) and entry (node)
     * 
     * @param finger the finger table index
     * @param node the information to store on the finger table
     */
    public void setFingerTableEntry(int finger, NodePair<Integer, InetSocketAddress> node) {
        this.fingerTable.setNodePair(finger, node);
    }

    /**
     * Remove all nodes from the Finger Table and Successor List with IP
     * address equal to 'address'. This method is called when node at
     * 'address' has failed.
     * 
     * @param address Address on failed node to be removed from Finger Table
     * and Successor List
     */
    public void removeNode(InetSocketAddress address) {
        NodePair<Integer, InetSocketAddress> pair = new NodePair<>(this.id, this.address);
        
        this.fingerTable.removeNode(address, pair);
        
        CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> toRemove = new CopyOnWriteArrayList<>();

        for (NodePair<Integer, InetSocketAddress> entry : this.successorList) {
            if (entry.getValue() != null && entry.getValue().equals(address)) {
                toRemove.add(entry);
            }
        }

        this.successorList.removeAll(toRemove);
    }

    
    /**
     * @return the peer associated to this node
     */
    public Peer getPeer() {
        return peer;
    }

    /**
     * Set peer that will be used to process and store this node's files
     * @param peer the peer to be used
     */
    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    /**
     * Gets closest preceding node address
     */
    public synchronized InetSocketAddress getClosestPreceding(Integer id) {
        NodePair<Integer, InetSocketAddress> lookup = this.fingerTable.lookup(this.getID(), id);

        Integer key = lookup.getKey();

        for (int i = this.successorList.size() - 1; i >= 0; i--) {
            NodePair<Integer, InetSocketAddress> succ = this.successorList.get(i);

            Integer succKey = succ.getKey();
            
            if (Utils.inBetween(succKey, key, id, this.m) && id != succKey)
                return succ.getValue();
        }

        return lookup.getValue();
    }

    /**
     * Finds the successor node of id
     *
     * @param id Identifier of known node
     * @return Chord's reply
     */
    public String[] findSuccessor(int id) {
        return this.findSuccessor(this.getAddress(), id);
    }

    /**
     * Finds the successor node of id
     * 
     * @param requestOrigin Original requesting node's address
     * @param id Identifier of known node
     * @return Chord's reply
     */
    public synchronized String[] findSuccessor(InetSocketAddress requestOrigin, int id) {
        int successorId = this.getSuccessorId();

        if (successorId == this.getID()) {
            return this.channel.createSuccessorFoundMessage(id, this.getID(), this.getAddress()).split(" ");
        }
        else if (Utils.inBetween(id, this.getID(), successorId, this.m)) {
            if (!requestOrigin.equals(this.getAddress())) {
                this.channel.sendSuccessorFound(requestOrigin, id, this.getSuccessorId(), this.getSuccessorAddress());
                return null;
            }
            else {
                return this.channel.createSuccessorFoundMessage(id, successorId, this.getSuccessor().getValue()).split(" ");
            }
        }
        else {
            InetSocketAddress closestPrecedingNode = this.getClosestPreceding(id);
            return this.channel.sendFindSuccessorMessage(requestOrigin, id, closestPrecedingNode);
        }

    }

    /**
     * This method notifies node 'n's successor of 'n's existence, giving the
     * successor the chance to change its predecessor to 'n'. The successor only
     * does this if it knows of no closer predecessor than 'n'.
     * 
     * @param node possible new predecessor
     */
    public synchronized void notify(NodePair<Integer, InetSocketAddress> node) {
        NodePair<Integer, InetSocketAddress> predecessor = this.getPredecessor();
        // if predecessor is null then it means that 'checkPredecessor' method
        // has determined that 'chord's predecessor has failed
        if (predecessor.getKey() == null || Utils.inBetween(node.getKey(), predecessor.getKey(), this.getID(), this.getM()))
            this.setPredecessor(node);
    }

    /**
     * 
     * @return node's communication channel
     */
    public ChordChannel getChannel() {
        return channel;
    }

    public void initiateChunkBackup(String fileID, int chunkNumber, int replicationDegree, String hash, InetSocketAddress succAddress, byte[] data) {
        this.channel.sendPutchunkMessage(fileID, chunkNumber, replicationDegree, hash, data,
                this.getAddress(), succAddress, succAddress);
        this.peer.getFileOccurrences().addChunkSlot(fileID, chunkNumber);
    }

    public void initiateBackup(String filePath, int replicationDegree) {

        System.out.print("\nBACKUP PROTOCOL\n" +
                "\t> File: " + filePath + "\n" +
                "\t> RD:   " + replicationDegree + "\n");

        // Stores file bytes and splits it into chunks
        SavedFile sf = new SavedFile(filePath, replicationDegree);

        ArrayList<Chunk> fileChunks = sf.getChunks();
        String fileID = sf.getId();

        this.peer.getFileOccurrences().addFile(fileID, filePath, replicationDegree);

        for (Chunk chunk : fileChunks) {
            // ChunkID => id on chord
            Integer chunkID = Utils.hash(fileID + ":" + chunk.getNum());
            String[] succ = this.findSuccessorMaxTries(chunkID);

            if (succ == null) {
                System.out.println("Error finding successor of chunk " + MyUtils.MAX_TRIES + " times in a row. Ending");
                return;
            }

            // Message format: SUCCESSORFOUND <requestedId> <successorId> <successorNodeIp> <successorNodePort>
            InetSocketAddress succAddress = new InetSocketAddress(succ[3], Integer.parseInt(succ[4]));
            String hash = MyUtils.sha256(this.getAddress() + "-" + succAddress + "-" + System.currentTimeMillis());
            this.initiateChunkBackup(fileID, chunk.getNum(), replicationDegree, hash, succAddress, chunk.getData());
        }

    }

    public int storeChunk(String fileID, int chunkNumber, byte[] data, int size, InetSocketAddress initiator) {
        return this.peer.getChunkStorage().addChunk(new Chunk(fileID, chunkNumber, data, size), initiator);
    }

    public boolean isChunkStored(String fileID, int chunkNumber) {
        return this.peer.getChunkStorage().hasChunk(fileID, chunkNumber);
    }

    public Chunk getStoredChunk(String fileID, int chunkNumber) {
        return this.peer.getChunkStorage().getChunk(fileID, chunkNumber);
    }

    public void initiateDelete(String filePath) {
        SavedFile sf = new SavedFile(filePath);
        String fileID = sf.getId();

        if (fileID == null) {
            System.out.println("File does not exist.");
            return;
        }

        deleteFile(fileID);
        this.channel.sendDeleteMessage(this.getAddress(), fileID, this.getSuccessorAddress());
    }

    public void deleteFile(String fileID) {
        if (!this.peer.getChunkStorage().deleteFile(fileID)) {
            System.out.println("Error deleting file with ID " + fileID);
            return;
        }
        System.out.println("File with id=" + fileID + " was removed!");
        this.peer.getFileOccurrences().deleteFile(fileID);
    }

    public void placeWall(String hash) {
        this.protocolPropagationWall.add(hash);
    }

    public boolean hitWall(String hash) {
        if (this.protocolPropagationWall.contains(hash)) {
            this.protocolPropagationWall.remove(hash);
            return true;
        }
        this.protocolPropagationWall.add(hash);
        return false;
    }

    public void initiateReclaim(int space) {
        this.peer.getChunkStorage().reclaimSpace(space);
    }

    public void sendMessage(InetSocketAddress destination, String message) {
        this.channel.sendMessage(destination, message);
    }

    public void initiateRestore(String filePath) {
        SavedFile sf = new SavedFile(filePath, 0);
        int numberChunks = sf.getChunks().size();

        String fileName = MyUtils.fileNameFromPath(filePath);
        String fileID = sf.getId();
        this.getPeer().getFileRestorer().addFile(fileID, numberChunks);

        for (int currentChunk = 0; currentChunk < numberChunks; currentChunk++) {
            Integer chunkID = Utils.hash(fileID + ":" + currentChunk);

            String[] reply = this.findSuccessorMaxTries(chunkID);

            if (reply == null) {
                System.out.println("Error finding successor of chunk " + MyUtils.MAX_TRIES + " times in a row. Ending");
                return;
            }

            // Message format: SUCCESSORFOUND <requestedId> <successorId> <successorNodeIp> <successorNodePort>
            InetSocketAddress succAddress = new InetSocketAddress(reply[3], Integer.parseInt(reply[4]));

            this.channel.sendGetChunkMessage(this.getAddress(), succAddress, fileID, currentChunk, succAddress);
        }

        try {
            // If the file isnt ready to be restored (still missing chunks), waits (max 8 seconds) for it to finish
            this.getPeer().getFileRestorer().getFileRestorationStatus(fileID).await(8, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Restoration was interrupted by another Thread. Restoration failed");
            e.printStackTrace();
        }

        // Tries to restore
        if (this.getPeer().getFileRestorer().restoreFile(fileID, fileName))
            System.out.println("File successfully restored");
        else
            System.out.println("Error restoring file");
    }

    String[] findSuccessorMaxTries(Integer chunkID) {
        String[] reply;
        int nTries = 0;
        do {
            reply = this.findSuccessor(chunkID);
            if (reply == null)
                nTries++;
        } while (nTries < MyUtils.MAX_TRIES);

        if (nTries == MyUtils.MAX_TRIES) {
            System.out.println("Error getting successor, please try again later");
            return null;
        }

        return reply;
    }
}

/**
 * Chord Node Printer
 * 
 * Helper class to print chord node's information
 * about periodically
 */
class ChordNodePrinter extends TimerTask {

    private ChordNode node;

    public ChordNodePrinter(ChordNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        System.out.println("\nFINGER TABLE");
        System.out.println(node.getFingerTable());
        //
        System.out.println("\nSUCCESSOR LIST");
        CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> successorList = node.getSuccessorList();
        for (NodePair<Integer, InetSocketAddress> successor : successorList)
            System.out.println(successor);
        //
        System.out.println("\nPREDECESSOR");
        System.out.println(node.getPredecessor());
    }
}
