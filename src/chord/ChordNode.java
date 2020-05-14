package chord;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChordNode {

    /**
	 * The peer's unique identifier
     */
    private Integer id;

    /**
     * Number of bits of the addressing space
     */
    private int m;

    /**
     * The successorList's size, r < m
     */
    private int r;

    /**
     * The peer's address
     */
    private InetSocketAddress address;

    /**
     * The peer's predecessor
     */
    private NodePair<Integer, InetSocketAddress> predecessor;

    /**
     * The peer's finger table. Stores m entries of other peers in the ring
     */
    private FingerTable fingerTable;

    /**
	 * The list of its successors, size r
	 */
    private NodePair<Integer, InetSocketAddress>[] successorList;

    /**
     * Thread Executor used for chord maintainer
     */
    private ScheduledThreadPoolExecutor executor;

    /**
     * Chord channel used for communication
     */
    private ChordChannel channel = null;

    public ChordNode() {
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
     * Gets a ChordNode's InetSocketAddress
     */
	protected InetSocketAddress getAddress() {
	    return address;
    }

    /**
     * Gets a ChordNode's id
     */
    protected int getId() {
	    return this.id;
    }

    /**
     * Gets node's successor id
     */
    protected int getSuccessorId() {
        return this.fingerTable.getFirstNode().getKey();
    }

    /**
     * Gets node's successor address
     */
    protected InetSocketAddress getSuccessorAddress() {
        return this.fingerTable.getFirstNode().getValue();
    }

    /**
     * Gets closest preceding node address
     */
    protected InetSocketAddress getClosestPreceding(int id) {
        return this.fingerTable.lookup(this.getId(), id);
    }

    /**
     * Returns predecessor's id
     */
    protected int getPredecessorId() {
        return this.predecessor.getKey();
    }

    /**
     * Returns predecessor's address
     */
    protected InetSocketAddress getPredecessorAddress() {
        return this.predecessor.getValue();
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
    //TODO: Return type?
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