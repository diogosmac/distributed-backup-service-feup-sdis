package chord;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class FingerTable {

    /**
     * Equal to 'm'
     */
    private final int MAX_SIZE;

    ArrayList<NodePair<Integer, Integer>> table;

    public FingerTable(int size) {
        this.table = new ArrayList<>();
        this.MAX_SIZE = size;
    }

    public Integer lookup(Integer peerHash, Integer fileHash) {
        // lookup in finger table for peer/node closest to fileHash
        for (int finger = MAX_SIZE - 1; finger >= 0; finger--) {
            NodePair<Integer, Integer> possibleNode = this.table.get(finger);
            if (!inBetween(fileHash, peerHash, possibleNode.getKey()))
                return possibleNode.getValue();
        }
        // if not found then we move to the next node/peer
        return getFirstNode().getValue();
    }

    public NodePair<Integer, Integer> getFirstNode() {
        return this.table.get(0);
    }

    private boolean inBetween(int target, int lowerBound, int upperBound) {

        int maxHashes = (int) Math.pow(2, this.MAX_SIZE);

        if (upperBound < lowerBound)
            upperBound += maxHashes;

        return lowerBound < maxHashes && maxHashes < upperBound;
	}
    
}