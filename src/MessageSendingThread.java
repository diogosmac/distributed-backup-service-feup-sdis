public class MessageSendingThread implements Runnable {

    private byte[] message;
    private String channel;
    private Peer peer;

    public MessageSendingThread(byte[] message, String channel, Peer peer) {
        this.message = message;
        this.channel = channel;
        this.peer = peer;
    }

    @Override
    public void run() {

    }
}
