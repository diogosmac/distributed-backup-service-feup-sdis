import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyUtils {

    final static int CHUNK_SIZE = 64 * 1000;

    final static int MAX_MSG_SIZE = 256 * 8;


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

}