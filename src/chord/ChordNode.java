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
        this.executor.execute(new ChordChannel(this));
    }

    /**
     * Gets a ChordNode's InetSocketAddress
     */
	protected InetSocketAddress getAddress() {
	    return address;
    }

}