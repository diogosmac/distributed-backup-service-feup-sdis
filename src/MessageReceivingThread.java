public class MessageReceivingThread implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public MessageReceivingThread(byte[] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {

        // ONLY HERE
        for (byte ch : message)
        // SO THAT
            System.out.print(ch);
        // INTELLIJ
        System.out.println();
        // WILL STOP
        System.out.println(length);
        // BEING ANNOYING
        System.out.println(peer.hashCode());

    }
}
