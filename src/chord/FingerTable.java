package chord;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class FingerTable {

    /**
     * Equal to 'm'
     */
    private final int MAX_SIZE;

    private ArrayList<NodePair<Integer, InetSocketAddress>> table;

    public FingerTable(int size) {
        this.table = new ArrayList<>();
        this.MAX_SIZE = size;
    }

    public InetSocketAddress lookup(Integer peerHash, Integer fileHash) {
        // lookup in finger table for peer/node closest to fileHash
        for (int finger = MAX_SIZE - 1; finger >= 0; finger--) {
            NodePair<Integer, InetSocketAddress> possibleNode = this.table.get(finger);
            System.out.println(possibleNode.getKey());
            if (!inBetween(fileHash, peerHash, possibleNode.getKey()))
                return possibleNode.getValue();
        }
        // if not found then we move to the next node/peer
        return getFirstNode().getValue();
    }

    public NodePair<Integer, InetSocketAddress> getFirstNode() {
        return this.table.get(0);
    }

    private boolean inBetween(int target, int lowerBound, int upperBound) {

        int maxHashes = (int) Math.pow(2, this.MAX_SIZE);

        if (upperBound < lowerBound) {
            upperBound += maxHashes;
            target += maxHashes;
        }

        return lowerBound < target && target < upperBound;
    }
    
    public void addNodePair(int index, NodePair<Integer, InetSocketAddress> element) {
        this.table.add(index, element);
    }
    
}