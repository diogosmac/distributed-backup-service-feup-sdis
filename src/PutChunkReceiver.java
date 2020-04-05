public class PutChunkReceiver implements Runnable {

    private byte [] message;
    private int length;
    private Peer peer;

    public PutChunkReceiver(byte [] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    public Chunk buildChunk() {
        String message = new String(this.message);
        String[] args = message.split(" ");
//        String version = args[0];
        int sender_id = Integer.parseInt(args[1]);
        if(this.peer.getPeerID() != sender_id) {
            String file_id = args[2];
            int chunk_number = Integer.parseInt(args[3]);
            int rep_degree = Integer.parseInt(args[4]);
            // Excludes both <CRLF>
            String body = args[5].substring(4);
            byte [] body_bytes = body.getBytes();
            return new Chunk(file_id, chunk_number, body_bytes, body_bytes.length, rep_degree);
        }
        else
            return null;
    }

    public String buildStoredMessage(String file_id, int chunk_number) {

        // <Version> STORED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        String message = String.join(" ", peer.getProtocolVersion(), "STORED",
                Integer.toString(peer.getPeerID()), file_id, Integer.toString(chunk_number),
                MyUtils.CRLF+MyUtils.CRLF);

        return message;
    }

    @Override
    public void run() {
        Chunk received_chunk = buildChunk();
        if(received_chunk != null) {
            this.peer.storeChunk(received_chunk);
            String stored_message = buildStoredMessage(received_chunk.getFileID(), received_chunk.getNum());
            peer.executeThread(new MessageSender(stored_message.getBytes(), peer.getMulticastControlChannel()));
        }
    }
}
