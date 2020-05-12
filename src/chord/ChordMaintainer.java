package chord;

import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * Chord Maintainer
 * 
 * In order to ensure that lookups execute correctly as the set of participating
 * nodes changes, Chord must ensure that each node's successor is up to date. It
 * does this using a "stabilization" protocol that each node runs periodically
 * in the background and whichi updates Chord's finger tables and successor
 * pointers.
 * 
 */
public class ChordMaintainer implements Runnable {

    /**
     * Node in Chord network
     */
    private ChordNode chord;

    /**
     * Default Constructor
     * 
     * @param chord node in chord network
     */
    public ChordMaintainer(ChordNode chord) {
		this.chord = chord;
    }
    
    /**
     * When the first chord node enters the ring we need to create a new Chord ring
     */
    private void create() {
        // no predecessor
        chord.setPredecessor(null);
        // successor is itself
        ArrayList<NodePair<Integer, InetSocketAddress>> successorList = new ArrayList<>();
        successorList.add(new NodePair<Integer,InetSocketAddress>(chord.getId(), chord.getAddress()));
        chord.setSuccessorList(successorList);
    }

    /**
     * Join a Chord ring containing any known node 'node'. This method asks 'node'
     * to find the immediate successor of 'chord'. By itself this method does not
     * make the rest of the network aware of 'chord'
     * 
     * @param node any know node in the chord network
     */
    private void join(NodePair<Integer, InetSocketAddress> node) {
        // no predecessor
        chord.setPredecessor(null);
        // successor is found by 'node'
        ArrayList<NodePair<Integer, InetSocketAddress>> successorList = new ArrayList<>();
        //successorList.add(node.findSuccessor(this.chord));
        chord.setSuccessorList(successorList);
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
        // get 'chord's sucessor's predecessor
        NodePair<Integer, InetSocketAddress> successor = chord.getSuccessor();
        NodePair<Integer, InetSocketAddress> successorsPredecessor = chord.getSuccessorsPredecessor();
        // check if sucessor's predecessor ID is between 'chord' and 'chords's successor
        // if so, then successorsPredecessor is our new successor
        if (Utils.inBetween(successorsPredecessor.getKey(), chord.getId(), successor.getKey(), chord.getM()))
            chord.setSuccessor(successorsPredecessor);
        // notify 'chord's successor of 'chord's existance 
        // TODO 
    }

    /**
     * This method notifies node 'n's successor of 'n's existence, giving the
     * successor the chance to change its predecessor to 'n'. The successor only
     * does this if it knows of no closer predecessor than 'n'.
     * @param node 
     */
    private void notify(NodePair<Integer, InetSocketAddress> node) {
        
    }

    /**
     * This method is called periodically by every node in the network to make sure
     * its finger table entries are correct. This is how new nodes initialize their
     * finger tables, and it is how existing nodes incorporate new nodes into their
     * finger tables.
     */
    private void fixFingers() {

    }

    /**
     * Each node runs this method periodically, to clear the node's predecessor pointer
     * if 'n's predecessor has failed. This allows it to accept a new predecessor in
     * 'notify' method.
     */
    private void checkPredecessor() {

    }

    /**
     * Thread 
     */
    @Override
    public void run() {
        // TODO Auto-generated method stub

    }
}