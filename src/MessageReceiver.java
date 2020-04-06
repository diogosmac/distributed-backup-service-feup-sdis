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
        String messageStr = new String(message);
        String[] args = messageStr.split(" ");
        String messageType = args[1];

        switch (messageType) {

            // TODO: Implement MessageReceiver subclasses for each message type

            case "PUTCHUNK":
                System.out.println("PUTCHUNK Message received | Type: " + args[1] + ", " +
                                                               "Sender: " + args[2] + ", " +
                                                               "Chunk #" + args[4]);
                System.out.flush();
                int interval = MyUtils.randomNum(0, 400);
                peer.scheduleThread(new PutChunkReceiver(message, length, peer), interval, TimeUnit.MILLISECONDS);
                break;

            case "STORED":
                System.out.println("STORED Message received");
                System.out.flush();
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
                System.out.println("Received unrecognized message: " + messageStr);
                System.out.flush();
                break;

        }

    }

}
