public class ChunkReceiver implements Runnable {

    private byte[] message;
    private Peer peer;

    public ChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {

    }
}
