import java.nio.charset.StandardCharsets;

public class ChunkReceiver implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public ChunkReceiver(byte[] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {
        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        String messageStr = new String(message).substring(0, this.length);
        String[] args = messageStr.split(" ");
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.notRecentlyReceived(fileId, chunkNumber))
            if (this.peer.isDoingOperation(Peer.Operation.RESTORE)) {
                String bodyStr = messageStr.substring(messageStr.indexOf(MyUtils.CRLF + MyUtils.CRLF) + 2);
                byte[] body = bodyStr.getBytes();

                this.peer.saveRestoredChunk(chunkNumber, body);
            }

        this.peer.saveReceivedChunkTime(fileId, chunkNumber);

    }

}
