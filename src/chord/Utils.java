package chord;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Chord Util Methods
 * 
 * This Class contains useful methods to be used during regular
 * chord maintaining operations
 */
public class Utils {

    public static final int m = 16;
    
    /**
     * This method checks if an integer number is between in interval
     * 
     * @param target wanted file's ID
     * @param lowerBound interval's lower bound
     * @param upperBound interval's upper bound (inclusive)
     * @return true if 'target' is between 'lowerBound' and 'upperBound', this last on is inclusive
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
        return lowerBound < target && target <= upperBound;
    }

    /**
     * @param str String to be hashed
     * @return Hashed integer value of the string
     */
    public static Integer hash(String str) {
        byte[] encrypted = sha1(str);
        if (encrypted == null) {
            System.out.println("Error occurred when encrypting string!");
            return null;
        }
        ByteBuffer wrapped = ByteBuffer.wrap(encrypted);
		return Math.floorMod(wrapped.getInt(), (int) Math.pow(2, m));
    }

    /**
     * 
     * @param str String to hash
     * @return byte array of hashed string using SHA1 algorithm
     */
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
