package chord;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Chord {

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
    private ScheduledThreadPoolExecutor scheduler;

    public Chord() {

        // start chord maintainer thread
        this.startMaintainer();
    }


    /**
	 * Starts the maintenance routine
	 */
	private void startMaintainer() {
        // create scheduled thread executor and limit to one thread
        this.scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        // perform maintenance every half second after 1.5 seconds after starting
        this.scheduler.scheduleWithFixedDelay(new ChordMaintainer(this), 1500, 500, TimeUnit.MILLISECONDS);
	}
    
}