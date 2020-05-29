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
    private final ChordNode node;

    /**
     * Default Constructor
     * 
     * @param chord node in chord network
     */
    public ChordMaintainer(ChordNode chord) {
		this.node = chord;
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
     * Most of this work in handled by MessageHandler class (@see MessageHandler) after
     * receiving get predecessor.
     */
    private void stabilize() {
        // send message to node's successor asking for its predecessor
        // message handler will take care of the rest
        NodePair<Integer, InetSocketAddress> successor = this.node.getSuccessor();
        this.node.getChannel().sendGetPredecessorMessage(this.node.getAddress(), successor.getValue());
    }

    /**
     * This method is called periodically by every node in the network to make sure
     * its finger table entries are correct. This is how new nodes initialize their
     * finger tables, and it is how existing nodes incorporate new nodes into their
     * finger tables.
     */
    private void fixFingers() {

        // get, update and set finger
        int finger = this.node.getFinger();
        finger = (finger + 1) % this.node.getM();
        node.setFinger(finger);
        // calculate new node ID
        int nodeID = (node.getID() + (int) Math.pow(2, finger)) % (int) Math.pow(2, this.node.getM());
        // find node's successor
        String[] reply = this.node.findSuccessor(nodeID);
        // if successor fails let the handler deal with it
        if (reply == null)
            return;
        // build reply node
        int replyNodeId = Integer.parseInt(reply[2]);
        InetSocketAddress replyNodeInfo = new InetSocketAddress(reply[3], Integer.parseInt(reply[4]));
        NodePair<Integer, InetSocketAddress> replyNode = new NodePair<>(replyNodeId, replyNodeInfo);
        // update entry in finger table with index 'finger'
        node.setFingerTableEntry(finger, replyNode);
    }

    /**
     * Each node runs this method periodically, to clear the node's predecessor pointer
     * if 'n's predecessor has failed. This allows it to accept a new predecessor in
     * 'notify' method.
     */
    private void checkPredecessor() {
        // node may not have a predecessor yet
        if (node.getPredecessor().getKey() == null)
            return;
        // send message to predecessor
        boolean online = node.getChannel().sendPingMessage(this.node.getAddress(),
                this.node.getPredecessor().getValue());
        // if he does not respond set it as failed (null)
        if (!online)
            node.setPredecessor(new NodePair<>(null, null));
    }

    /**
     * Thread to maintain dynamic operations and failure resistance in chord node
     */
    @Override
    public void run() {
        this.stabilize();
        this.fixFingers();
        this.checkPredecessor();
    }
}