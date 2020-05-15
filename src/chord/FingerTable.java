package chord;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.stream.Collectors;


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

    @Override
    public String toString() {
        return this.table.isEmpty()
            ? "empty"
            : this.table.stream()
                .map( n -> n.toString() )
                .collect( Collectors.joining("\n"));

    }
}
