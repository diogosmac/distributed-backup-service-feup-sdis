package chord;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private int finger;

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

    public ChordNode(Integer id, int m) throws UnknownHostException {
        this(id, m, new InetSocketAddress(InetAddress.getLocalHost(), 2));
    }

    public ChordNode(Integer id, int m, InetSocketAddress address) {

        this.id = id;
        this.m = m;
        this.fingerTable = new FingerTable(m);
        this.address = address;

        // creates the ChordNode's scheduled thread executor
        this.createExecutor();
        // start chord maintainer thread
        this.startMaintainer();
        // start chord communication channel thread
        this.startChannel();
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
        // perform maintenance every half second after 1.5 seconds after starting
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
    protected int getSuccessorId() {
        return this.getSuccessor().getKey();
    }

    /**
     * Gets node's successor address
     */
    protected InetSocketAddress getSuccessorAddress() {
        return this.getSuccessor().getValue();
    }

    /**
     * Set 'node' as node's new successor
     * 
     * @param node new chord's successor
     */
    public void setSuccessor(NodePair<Integer, InetSocketAddress> node) {
        this.fingerTable.setNodePair(0, node);
    }

    /**
     * @return chord node's successor's predecessor
     */
    public NodePair<Integer, InetSocketAddress> getSuccessorsPredecessor() {
        NodePair<Integer, InetSocketAddress> successor = this.fingerTable.getFirstNode();

        // TODO
        // send request to successor.getValue() (InetSocketAddress) for its predecessor
        // build pair and return it

        return null;
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
    protected InetSocketAddress getClosestPreceding(int id) {
        return this.fingerTable.lookup(this.getId(), id);
    }

    /**
     * Finds the successor node of id
     */
    protected void findSuccessor(int id) {
        this.findSuccessor(this.getAddress(), id);
    }

    /**
     * Finds the successor node of id
     */
    protected String[] findSuccessor(InetSocketAddress requestOrigin, int id) {
        //TODO: Check predecessor?

        int successorId = this.fingerTable.getFirstNode().getKey();
        if (this.fingerTable.inBetween(id, this.getId(), successorId)) {
            this.channel.returnFindSuccessor(requestOrigin, id, this.getSuccessorAddress());
            return null;
        }
        else {
            InetSocketAddress closestPrecedingNode = this.getClosestPreceding(id);
            return this.channel.sendFindSuccessorMessage(requestOrigin, id, closestPrecedingNode);
        }
    }

}
