public class GetChunkReceiver implements Runnable {

    private byte[] message;
    private Peer peer;

    public GetChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {

    }
}
