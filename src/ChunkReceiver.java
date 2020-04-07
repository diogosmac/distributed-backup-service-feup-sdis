public class ChunkReceiver implements Runnable {

    private byte[] message;
    private Peer peer;

    public ChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {
        //  <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        String messageStr = new String(message);
        String[] args = messageStr.split(" ");
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if(this.peer.getState() == Peer.State.RESTORE) {
            String bodyStr = args[5].substring(4);
            byte[] body = bodyStr.getBytes();

            this.peer.saveRestoredChunk(chunkNumber, body);
        }
        else {
            this.peer.saveReceivedChunkTime(fileId, chunkNumber);
        }
    }
}
