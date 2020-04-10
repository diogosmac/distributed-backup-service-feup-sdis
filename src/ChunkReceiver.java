public class ChunkReceiver implements Runnable {

    private final byte[] message;
    private final Peer peer;

    public ChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {
        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        String messageStr = MyUtils.convertByteArrayToString(this.message);
        String[] args = messageStr.split(" ");
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.notRecentlyReceived(fileId, chunkNumber))
            if (this.peer.isDoingOperation(Peer.Operation.RESTORE)) {
                String bodyStr = messageStr.substring(messageStr.indexOf(MyUtils.CRLF + MyUtils.CRLF) + 2);
                byte[] body = MyUtils.convertStringToByteArray(bodyStr);

                this.peer.getFileRestorer().saveRestoredChunk(fileId, chunkNumber, body);
            }

        this.peer.saveReceivedChunkTime(fileId, chunkNumber);
    }

}
