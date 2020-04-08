import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class PutChunkReceiver implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public PutChunkReceiver(byte[] message, int length, Peer peer) {
        this.message = new byte[length];
        System.arraycopy(message, 0, this.message, 0, length);
        this.length = length;
        this.peer = peer;
    }

    public Chunk buildChunk() {
        String message = new String(this.message, StandardCharsets.ISO_8859_1);
//        System.out.println(" ==> Full message length: " + message.length());
        String[] args = message.split(" ");
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);
        int repDegree = Integer.parseInt(args[5]);
        byte[] bodyBytes = null;
        int numBytes = 0;
        if (!this.peer.hasChunk(fileId, chunkNumber)) {
            String body = message.substring(message.indexOf(MyUtils.CRLF + MyUtils.CRLF) + 2); // Skips both <CRLF>
//            System.out.println(" ==> Body length: " + body.length());
            bodyBytes = body.getBytes(StandardCharsets.ISO_8859_1);
//            System.out.println(" ==> Body bytes length: " + bodyBytes.length);
            numBytes = bodyBytes.length;
        }

        return new Chunk(fileId, chunkNumber, bodyBytes, numBytes, repDegree);
    }

    public String buildStoredMessage(String fileId, int chunkNumber) {
        // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        return String.join(" ", peer.getProtocolVersion(), "STORED",
                Integer.toString(peer.getPeerID()), fileId, Integer.toString(chunkNumber),
                MyUtils.CRLF + MyUtils.CRLF);
    }

    @Override
    public void run() {
        Chunk receivedChunk = buildChunk();
        if (receivedChunk.getData() != null) {
            this.peer.storeChunk(receivedChunk);

            String storedMessage = buildStoredMessage(receivedChunk.getFileID(), receivedChunk.getNum());
            int interval = MyUtils.randomNum(0, 400);
            peer.scheduleThread(new MessageSender(
                    storedMessage.getBytes(),
                    peer.getMulticastControlChannel()), interval, TimeUnit.MILLISECONDS);
        }
    }

}
