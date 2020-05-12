package chord;

public class Utils {
    
    /**
     * This method checks if an integer number is between in interval
     * 
     * @param target wanted file's ID
     * @param lowerBound
     * @param upperBound
     * @return true if 'target' is between 'lowerBound' and 'upperBound'
     */
    public static boolean inBetween(Integer target, Integer lowerBound, Integer upperBound, int m) {
        // calculate max nodes in the chord
        int maxNodes = (int) Math.pow(2, m);
        // if upper bound is smaller than lower bound, then we have made a complete
        // loop in the chord's ring
        if (upperBound < lowerBound) {
            upperBound += maxNodes;
            target += maxNodes;
        }
        // finally, calculate target intervals
        return lowerBound < target && target < upperBound;
    }
}
