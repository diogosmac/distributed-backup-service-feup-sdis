package chord;

import java.net.InetSocketAddress;
import java.util.ArrayList;
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
    private ArrayList<NodePair<Integer, InetSocketAddress>> successorList;

    /**
     * Thread Executor used for chord maintainer
     */
    private ScheduledThreadPoolExecutor scheduler;

    public ChordNode() {

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
     * @return chord node's sucessor, which happens to be
     * the first element of 'successorList' and also the first
     * element of 'fingerTable'
     */
    public NodePair<Integer, InetSocketAddress> getSuccessor() {
        return this.fingerTable.getFirstNode();
    }

    /**
     * Set 'node' as node's new successor
     * 
     * @param node new chord's successor
     */
    public void setSuccessor(NodePair<Integer, InetSocketAddress> node) {
        this.fingerTable.addNodePair(0, node);
    }

    /**
     * @return chord node's sucessor's predecessor
     */
    public NodePair<Integer, InetSocketAddress> getSuccessorsPredecessor() {
        NodePair<Integer, InetSocketAddress> successor = this.fingerTable.getFirstNode();

        // TODO
        // send request to successor.getValue() (InetSocketAddress) for its predecessor
        // build pair and return it

        return null;
    }
    
}
