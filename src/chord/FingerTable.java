package chord;

import java.net.InetSocketAddress;
import java.util.ArrayList;

/**
 * Finger Table
 * To avoid linear search, Chord implements a faster search method by requiring
 * each node to keep a finger table containing up to 'm' entries, recall that
 * 'm' is the number of bits in the hash key. The 'i'th entry of node 'n' will
 * contain successor(('n' + 2^('i' − 1)) mod 2^'m'). The first entry of finger
 * table is actually the node's immediate successor (and therefore an extra successor
 * field is not needed). Every time a node wants to look up a key 'k', it will pass the
 * query to the closest successor or predecessor (depending on the finger table) of 'k'
 * in its finger table (the "largest" one on the circle whose ID is smaller than 'k'),
 * until a node finds out the key is stored in its immediate successor.
 * 
 * With such a finger table, the number of nodes that must be contacted to find a successor
 * in an 'N'-node network is O(log⁡ 'N'). 
 */
public class FingerTable {

    /**
     * Equal to 'm', i.e. the number of bits in the hash key
     */
    private final int MAX_SIZE;

    /**
     * List of NodePairs containing nodes' hashed ID as keys, and
     * Socket Address (IP and Port) as values
     */
    private ArrayList<NodePair<Integer, InetSocketAddress>> table;

    /**
     * Constructor
     * @param size maximum size of finger table
     */
    public FingerTable(int size) {
        this.table = new ArrayList<>();
        this.MAX_SIZE = size;
    }

    /**
     * 
     * @param finger Index in the finger table
     * @param node NodePair containing node's hashed ID as keys, and
     * Socket Address (IP and Port) as value
     */
    public void setNodePair(int finger, NodePair<Integer, InetSocketAddress> node) {
        this.table.add(finger, node);
    }
    
    /**
     * 
     * @return the first network node 
     */
    public NodePair<Integer, InetSocketAddress> getFirstNode() {
        return this.table.get(0);
    }

    /**
     * Lookups the "largest" node on the circle whose ID is smaller
     * than 'fileID'('k')
     * 
     * @param nodeID current node's ID
     * @param fileID wanted file's ID
     * @return InetSocketAddress of the "largest" node on the circle
     * whose ID is smaller than 'fileID'('k')
     */
    public InetSocketAddress lookup(Integer nodeID, Integer fileID) {
        // lookup in finger table for peer/node closest to fileID
        for (int finger = MAX_SIZE - 1; finger >= 0; finger--) {
            NodePair<Integer, InetSocketAddress> possibleNode = this.table.get(finger);
            System.out.println(possibleNode.getKey());
            if (!Utils.inBetween(fileID, nodeID, possibleNode.getKey(), MAX_SIZE))
                return possibleNode.getValue();
        }
        // if not found then we move to the next node/peer
        return getFirstNode().getValue();
    }
}
