import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class MyUtils {

    final static int BASE_PORT = 1904;

    final static int CHUNK_SIZE = 64 * 1000;

    private final static int VERSION = 1;
    private final static int SUBVERSION = 0;
    static String getVersion() {
        return VERSION + "." + SUBVERSION;
    }


    public static String sha256(String str) {

        try {

            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] encrypted = messageDigest.digest(str.getBytes(StandardCharsets.UTF_8));

            StringBuilder stringBuilder = new StringBuilder();
            for (byte crypt : encrypted) {
                String hexByte = Integer.toHexString(0xff & crypt);
                if (hexByte.length() == 1) stringBuilder.append('0');
                stringBuilder.append(hexByte);
            }
            return stringBuilder.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }


    static int randomNum(int lowest, int highest) {

        Random r = new Random();
        return r.nextInt(highest + 1 - lowest) + lowest;

    }

}