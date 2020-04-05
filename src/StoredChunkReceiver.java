public class StoredChunkReceiver implements Runnable {

    private byte [] message;
    private int length;
    private Peer peer;

    public StoredChunkReceiver(byte [] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {
        String message = new String(this.message);
        String [] args = message.split(" ");
//        String version = args[0];
        int sender_id = Integer.parseInt(args[2]);

        if(sender_id != this.peer.getPeerID()) {
            String file_id = args[3];
            int chunk_number = Integer.parseInt(args[4]);
            this.peer.saveChunkOccurrence(file_id, chunk_number);
        }
    }
}
