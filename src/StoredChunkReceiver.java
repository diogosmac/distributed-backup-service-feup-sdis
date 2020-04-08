import java.nio.charset.StandardCharsets;

public class StoredChunkReceiver implements Runnable {

    private byte[] message;
    private Peer peer;

    public StoredChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {
        String message = new String(this.message);
        String[] args = message.split(" ");
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);
        this.peer.saveChunkOccurrence(fileId, chunkNumber);
        System.out.println("\t\tOccurrences updated > " + fileId + "[" + chunkNumber + "]");
        System.out.flush();
    }

}
