package utils;

import peer.Peer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MyUtils {

    public final static int CHUNK_SIZE = 15 * 1000;
    public final static long PEER_MAX_MEMORY_USE = CHUNK_SIZE * 50000;

    public final static int MAX_TRIES = 5;

    public final static String DEFAULT_BACKUP_PATH = "/backup/";
    public final static String DEFAULT_RESTORE_PATH = "/restored/";
    public final static String DEFAULT_STATUS_PATH = "/status.sdis";
    public final static String DEFAULT_CHUNK_INFO_PATH = "/chunkInfo.sdis";

    public final static String CHUNK_FILE_EXTENSION = ".chk";

    public static String getPeerPath(Peer peer) {
        return "./peer_" + peer.getPeerId();
    }

    public static String getRestorePath(Peer peer) {
        return getPeerPath(peer) + DEFAULT_RESTORE_PATH;
    }

    public static String getBackupPath(Peer peer) {
        return getPeerPath(peer) + DEFAULT_BACKUP_PATH;
    }

    public static String getStatusPath(Peer peer) {
        return getPeerPath(peer) + DEFAULT_STATUS_PATH;
    }

    public static String getChunkInfoPath(Peer peer) {
        return getPeerPath(peer) + DEFAULT_CHUNK_INFO_PATH;
    }

    public static String sha256(String str) {

        try {

            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] encrypted = messageDigest.digest(str.getBytes());

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

    public static String encryptFileID(String fileName) {

        try {

            Path file = Paths.get(fileName);
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            String lastModified = attributes.lastModifiedTime().toString();
            String owner = Files.getOwner(file).getName();

            return MyUtils.sha256(fileName + "-" + lastModified + "-" + owner);

        } catch (IOException e) {
            return null;
        }

    }

    public static byte[] concatByteArrays(byte[] firstArr, byte[] secondArr) {
        byte[] result = new byte[firstArr.length + secondArr.length];
        System.arraycopy(firstArr, 0, result, 0, firstArr.length);
        System.arraycopy(secondArr, 0, result, firstArr.length, secondArr.length);
        return result;
    }

    public static String fileNameFromPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    public static byte[] trimMessage(byte[] message, int length) {
        byte[] result = new byte[length];
        System.arraycopy(message, 0, result, 0, length);
        return result;
    }

    public static String convertByteArrayToString(byte[] array) {
        // ISO_8859_1 is 1 to 1 conversion
        return new String(array, StandardCharsets.ISO_8859_1);
    }

    public static byte[] convertStringToByteArray(String text) {
        // ISO_8859_1 is 1 to 1 conversion
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

}