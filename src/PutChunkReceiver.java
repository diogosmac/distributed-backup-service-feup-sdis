public class PutChunkReceiver implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public PutChunkReceiver(byte[] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    public Chunk buildChunk() {
        String message = new String(this.message);
        String[] args = message.split(" ");
//        String version = args[0];
        int senderId = Integer.parseInt(args[2]);
        if (this.peer.getPeerID() != senderId) {
            String fileId = args[2];
            int chunkNumber = Integer.parseInt(args[4]);
            int repDegree = Integer.parseInt(args[5]);

            // Excludes both <CRLF>
            String body = args[6].substring(4);
            byte[] bodyBytes = body.getBytes();
            return new Chunk(fileId, chunkNumber, bodyBytes, bodyBytes.length, repDegree);
        } else
            return null;
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
        if (receivedChunk != null) {
            this.peer.storeChunk(receivedChunk);
            String storedMessage = buildStoredMessage(receivedChunk.getFileID(), receivedChunk.getNum());
            peer.executeThread(new MessageSender(storedMessage.getBytes(), peer.getMulticastControlChannel()));
            System.out.println("Message Sender called");
        }
        else
            System.out.println("\t I just read my own message!");
    }
}
