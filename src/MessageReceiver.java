import java.util.concurrent.TimeUnit;

public class MessageReceiver implements Runnable {

    private byte[] message;
    private Peer peer;

    public MessageReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {
        String messageStr = new String(message);
        String[] args = messageStr.split(" ");
        int senderId = Integer.parseInt(args[2]);

        String messageType = args[1];

        switch (messageType) {

            // TODO: Implement MessageReceiver subclasses for each message type

            case "PUTCHUNK":
                if (senderId == peer.getPeerID()) return;
                System.out.println("\tPUTCHUNK Message received | Type: " + args[1] + ", " +
                        "Sender: " + args[2] + ", " +
                        "Chunk #" + args[4] + ", " +
                        "Number bytes: " + this.message.length);
                System.out.flush();
                int interval = MyUtils.randomNum(0, 400);
                peer.scheduleThread(new PutChunkReceiver(message, peer), interval, TimeUnit.MILLISECONDS);
                break;

            case "STORED":
                if (senderId == peer.getPeerID()) return;
                if (peer.isDoingOperation(Peer.Operation.BACKUP)) {
                    System.out.println("\tSTORED Message received   | Type: " + args[1] + ", " +
                            "Sender: " + args[2] + ", " +
                            "Chunk #" + args[4] + ", " +
                            "Number bytes: " + this.message.length);
                    System.out.flush();
                    peer.executeThread(new StoredChunkReceiver(message, peer));
                }
                break;


            case "GETCHUNK":
                if (senderId == peer.getPeerID()) return;
                System.out.println("\tGETCHUNK Message received | Type: " + args[1] + ", " +
                        "Sender: " + args[2] + ", " +
                        "Chunk #" + args[4] + ", " +
                        "Number bytes: " + this.message.length);
                System.out.flush();
                peer.executeThread(new GetChunkReceiver(message, peer));
                break;

            case "CHUNK":
                if (senderId == peer.getPeerID()) return;
                System.out.println("\tCHUNK Message received    | Type: " + args[1] + ", " +
                        "Sender: " + args[2] + ", " +
                        "Chunk #" + args[4] + ", " +
                        "Number bytes: " + this.message.length);
                System.out.flush();
                peer.executeThread(new ChunkReceiver(message, peer));
                break;


            case "DELETE":
                System.out.println("\tDELETE Message received   | Type: " + args[1] + ", " +
                        "Sender: " + args[2] + ", " +
                        "File ID: " + args[3] + ", " +
                        "Number bytes: " + this.message.length);
                System.out.flush();
                peer.executeThread(new DeleteReceiver(message, peer));
                break;


            default:
                System.out.println("\tUnknown Message received  | " + messageStr);
                System.out.flush();
                break;

        }

    }

}
