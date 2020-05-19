package chord;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
    private final int m;

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
    private ArrayList<NodePair<Integer, InetSocketAddress>> successorList;

    /**
     * Thread Executor used for chord maintainer
     */
    private ScheduledThreadPoolExecutor executor;

    /**
     * Chord channel used for communication
     */
    private ChordChannel channel = null;

    /**
     * Constructor without IP address
     * @param id Chord Node identifier
     * @param m Number of bits of the addressing space
     * @throws UnknownHostException If unable to get localhost
     */
    public ChordNode(Integer id, int m) throws UnknownHostException {
        this(id, m, new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), 30000 + id));
    }

    /**
     * Constructor with IP address
     * @param id Chord Node identifier
     * @param m Number of bits of the addressing space
     * @param address IP address to join the Chord 'network'
     */
    public ChordNode(Integer id, int m, InetSocketAddress address) {

        this.id = id;
        this.m = m;
        this.fingerTable = new FingerTable(m);
        this.address = address;

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
     * @param id Chord Node identifier
     * @param m Number of bits of the addressing space
     * @param address IP address to join the Chord 'network'
     * @param knownAddress 
     */
    public ChordNode(Integer id, int m, InetSocketAddress address, InetSocketAddress knownAddress) {

        this.id = id;
        this.m = m;
        this.fingerTable = new FingerTable(m);
        this.address = address;

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
        this.setPredecessor(null);
        // successor is itself
        ArrayList<NodePair<Integer, InetSocketAddress>> successorList = new ArrayList<>();
        NodePair<Integer, InetSocketAddress> successor = new NodePair<>(this.getId(), this.getAddress());
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
        this.setPredecessor(null);
        // set successor list to empty
        ArrayList<NodePair<Integer, InetSocketAddress>> successorList = new ArrayList<>();
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
    public ArrayList<NodePair<Integer, InetSocketAddress>> getSuccessorList() {
        return successorList;
    }

    /**
     * @param successorList the successorList to set
     */
    public void setSuccessorList(ArrayList<NodePair<Integer, InetSocketAddress>> successorList) {
        this.successorList = successorList;
    }

    /**
     * @return the id
     */
    public Integer getId() {
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
     * @return chord node's sucessor, which happens to be
     * the first element of 'successorList' and also the first
     * element of 'fingerTable'
     */
    public NodePair<Integer, InetSocketAddress> getSuccessor() {
        return this.fingerTable.getFirstNode();
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
     * 
     * @param node new chord's successor
     */
    public void setSuccessor(NodePair<Integer, InetSocketAddress> node) {
        this.fingerTable.setNodePair(0, node);
        this.successorList.set(0, node);
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
     * Gets closest preceding node address
     */
    public InetSocketAddress getClosestPreceding(int id) {
        return this.fingerTable.lookup(this.getId(), id);
    }

    /**
     * Finds the successor node of id
     */
    public String[] findSuccessor(int id) {
        return this.findSuccessor(this.getAddress(), id);
    }

    /**
     * Finds the successor node of id
     */
    public synchronized String[] findSuccessor(InetSocketAddress requestOrigin, int id) {
        int successorId = this.getSuccessorId();

        if (successorId == this.getId()) {
            return this.channel.createSuccessorFoundMessage(id, this.getId(), this.getAddress()).split(" ");
        }
        else if (Utils.inBetween(id, this.getId(), successorId, this.m)) {
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
     * @param node possible new predecessor
     */
    public synchronized void notify(NodePair<Integer, InetSocketAddress> node) {
        NodePair<Integer, InetSocketAddress> predecessor = this.getPredecessor();
        // if predecessor is null then it means that 'checkPredecessor' method
        // has determined that 'chord's predecessor has failed
        if (predecessor == null || Utils.inBetween(node.getKey(), predecessor.getKey(), this.getId(), this.getM()))
            this.setPredecessor(node);
    }

    public ChordChannel getChannel() {
        return channel;
    }
}
