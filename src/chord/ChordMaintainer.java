package chord;

import java.net.InetSocketAddress;

/**
 * Chord Maintainer
 * 
 * In order to ensure that lookups execute correctly as the set of participating
 * nodes changes, Chord must ensure that each node's successor is up to date. It
 * does this using a "stabilization" protocol that each node runs periodically
 * in the background and which updates Chord's finger tables and successor
 * pointers.
 * 
 */
public class ChordMaintainer implements Runnable {

    /**
     * Node in Chord network
     */
    private final ChordNode chord;

    /**
     * Default Constructor
     * 
     * @param chord node in chord network
     */
    public ChordMaintainer(ChordNode chord) {
		this.chord = chord;
    }

    /**
     * This method is called periodically by every node in the network to learn about
     * newly joined nodes. Each time node 'n' runs 'stabilize', it asks its successor
     * for the successor's predecessor 'p', and decides whether 'p' should be 'n's
     * successor instead. This would be the case if node 'p' recently joined the
     * system.
     * In addition, this method notifies node 'n's successor of 'n's existence, giving
     * the successor the chance to change its predecessor to 'n'. The successor only
     * does this if it knows of no closer predecessor than 'n'.
     */
    private void stabilize() {
        // send message to node's successor asking for its predecessor
        // message handler will take care of the rest
        NodePair<Integer, InetSocketAddress> successor = this.chord.getSuccessor();
        this.chord.getChannel().sendGetPredecessorMessage(this.chord.getAddress(), successor.getValue());
    }

    /**
     * This method is called periodically by every node in the network to make sure
     * its finger table entries are correct. This is how new nodes initialize their
     * finger tables, and it is how existing nodes incorporate new nodes into their
     * finger tables.
     */
    private void fixFingers() {
        // get, update and set finger
        int finger = this.chord.getFinger();
        finger = (finger + 1) % this.chord.getM();
        chord.setFinger(finger);
        // calculate new node ID
        Integer nodeID = (chord.getId() + (int) Math.pow(2, finger)) % (int) Math.pow(2, this.chord.getM());
        // find node's successor
        String[] reply = this.chord.findSuccessor(nodeID);
        if (reply == null) {
            System.out.println("=========== FODA-SE ===========");
            return;
        }
        // build reply node
        int replyNodeId = Integer.parseInt(reply[2]);
        InetSocketAddress replyNodeInfo = new InetSocketAddress(reply[3], Integer.parseInt(reply[4]));
        NodePair<Integer, InetSocketAddress> node = new NodePair<>(replyNodeId, replyNodeInfo);
        // update entry in finger table with index 'finger'
        chord.setFingerTableEntry(finger, node);
    }

    /**
     * Each node runs this method periodically, to clear the node's predecessor pointer
     * if 'n's predecessor has failed. This allows it to accept a new predecessor in
     * 'notify' method.
     */
    private void checkPredecessor() {
        // node may not have a predecessor yet
        if (chord.getPredecessor() == null)
            return;
        // send message to predecessor
        boolean online = chord.getChannel().sendPingMessage(this.chord.getAddress(),
                this.chord.getPredecessor().getValue());
        // if he does not respond set it as failed (null)
        if (!online)
            chord.setPredecessor(null);
    }

    /**
     * Thread to maintain dynamic operations and failure resistance in chord node
     * 
     */
    @Override
    public void run() {
        System.out.println(" ---- STABLE");
        this.stabilize();
        System.out.println(" ---- FIX FINGERS");
        this.fixFingers();
        System.out.println(" ---- CHECK PREDECESSOR");
        this.checkPredecessor();
    }
}