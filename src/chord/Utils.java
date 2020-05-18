package chord;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        if (upperBound <= lowerBound) {
            if (target < lowerBound) {
                target += maxNodes;
            }
            upperBound += maxNodes;
        }
        // finally, calculate target intervals
        return lowerBound < target && target < upperBound;
    }

    /**
     * 
     * @param str
     * @return
     */
    public static Integer hash(String str) {
        ByteBuffer wrapped = ByteBuffer.wrap(sha1(str));
		return wrapped.getInt();
    }

    /** */
    private static byte[] sha1(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            return messageDigest.digest(str.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        

    }
}
