import java.util.concurrent.TimeUnit;

public class MessageReceiver implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public MessageReceiver(byte[] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {

        System.out.println("N Bytes read: " + length);

        String messageType = new String(message);
        messageType = messageType.substring(0, messageType.indexOf(" "));

        switch (messageType) {

            // TODO: Implement MessageReceivingThread subclasses for each message type

            case "PUTCHUNK":
                int interval = MyUtils.randomNum(0, 400);
                peer.scheduleThread(new PutChunkReceiver(message, length, peer), interval, TimeUnit.MILLISECONDS);
                break;

            case "STORED":
                peer.executeThread(new StoredChunkReceiver(message, length, peer));
                break;


            case "GETCHUNK":
//                peer.executeWithScheduler(new ReceiveGetChunkThread(message, peer));
                break;

            case "CHUNK":
//                peer.executeWithScheduler(new ReceiveChunkThread(message, length, peer));
                break;


            case "DELETE":
//                peer.executeWithScheduler(new ReceiveDeleteThread(message, peer));
                break;


            case "REMOVED":
//                peer.executeWithScheduler(new ReceiveRemovedThread(message, peer));
                break;


            default:
                break;

        }

    }

}
