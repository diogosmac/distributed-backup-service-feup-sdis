import java.util.concurrent.TimeUnit;

public class PutChunkReceiver implements Runnable {

    private final byte[] message;
    private final Peer peer;

    public PutChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    public Chunk buildChunk() {

        String message = MyUtils.convertByteArrayToString(this.message);
        String[] args = message.split(" ");
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.isDoingOperation(Peer.Operation.RECLAIM)) {
            this.peer.logPutChunkMessage(fileId, chunkNumber);
        }

        int repDegree = Integer.parseInt(args[5]);
        byte[] bodyBytes = null;
        int numBytes = 0;
        if (!this.peer.getChunkStorage().hasChunk(fileId, chunkNumber)) {
            String body = message.substring(message.indexOf(MyUtils.CRLF + MyUtils.CRLF) + 2); // Skips both <CRLF>
            bodyBytes = MyUtils.convertStringToByteArray(body);
            numBytes = bodyBytes.length;
        }

        return new Chunk(fileId, chunkNumber, bodyBytes, numBytes, repDegree);
    }

    public String buildStoredMessage(String fileId, int chunkNumber) {
        // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        return String.join(" ", peer.getProtocolVersion(), "STORED",
                Integer.toString(peer.getPeerId()), fileId, Integer.toString(chunkNumber),
                MyUtils.CRLF + MyUtils.CRLF);
    }

    @Override
    public void run() {
        Chunk receivedChunk = buildChunk();
        if (receivedChunk.getData() != null) {
            this.peer.getChunkOccurrences().addFile(receivedChunk.getFileID(), receivedChunk.getReplicationDegree());
            int status = this.peer.getChunkStorage().addChunk(receivedChunk);
            switch (status) {
                case 0:
                    this.peer.getChunkOccurrences().saveChunkOccurrence(
                            receivedChunk.getFileID(), receivedChunk.getNum(), this.peer.getPeerId());
                    break;
                case 1:
                    System.out.println("Not enough space to store received chunk");
                    return;
                case 2:
                    System.out.println("Exception occurred while storing the received chunk");
                    return;
                default:
                    break;
            }
        }

        String storedMessage = buildStoredMessage(receivedChunk.getFileID(), receivedChunk.getNum());
        int interval = MyUtils.randomNum(0, 400);
        peer.scheduleThread(new MessageSender(
                MyUtils.convertStringToByteArray(storedMessage),
                peer.getMulticastControlChannel()), interval, TimeUnit.MILLISECONDS);
    }

}
