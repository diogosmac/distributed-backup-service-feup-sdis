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

        String messageType = new String(message);
        messageType = messageType.substring(0, messageType.indexOf(" "));

        switch (messageType) {

            // TODO: Implement MessageReceivingThread subclasses for each message type

            case "PUTCHUNK":
                int interval = MyUtils.randomNum(0, 400);
//                peer.scheduleWithScheduler(new ReceivePutChunkThread(message, length, peer), interval, TimeUnit.MILLISECONDS);
                break;

            case "STORED":
//                peer.executeWithScheduler(new ReceiveStoredThread(message, length, peer));
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
