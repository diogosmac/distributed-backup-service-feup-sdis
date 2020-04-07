import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class MyUtils {

    public final static int BASE_PORT = 1904;

    public final static int CHUNK_SIZE = 64 * 1000;

    public final static char CR = '\r';
    public final static char LF = '\n';
    public final static String CRLF = new StringBuilder(CR).append(LF).toString();

    public final static int CHUNK_SEND_MAX_TRIES = 5;

    public final static String DEFAULT_RESTORE_PATH = "./restored_files/";

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

    public static byte[] convertToByteArray(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] concatByteArrays(byte[] firstArr, byte[] secondArr) {
        byte[] result = new byte[firstArr.length + secondArr.length];
        System.arraycopy(firstArr, 0, result, 0, firstArr.length);
        System.arraycopy(secondArr, 0, result, firstArr.length, secondArr.length);
        return result;
    }


    public static int randomNum(int lowest, int highest) {

        Random r = new Random();
        return r.nextInt(highest + 1 - lowest) + lowest;

    }

}