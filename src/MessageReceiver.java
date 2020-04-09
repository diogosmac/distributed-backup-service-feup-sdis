public class MessageReceiver implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public MessageReceiver(byte[] message, int length, Peer peer) {
        this.message = MyUtils.trimMessage(message, length);
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {

        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        int senderId = Integer.parseInt(args[2]);

        String messageType = args[1];

        switch (messageType) {

            // TODO: Implement MessageReceiver subclasses for each message type

            case "PUTCHUNK":
                if (senderId == peer.getPeerID()) break;
                System.out.println("\tPUTCHUNK Message received | Type: " + args[1] + ", " +
                                                                 "Sender: " + args[2] + ", " +
                                                                 "Chunk #" + args[4] + ", " +
                                                                 "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new PutChunkReceiver(message, peer));
                break;

            case "STORED":
                if (senderId == peer.getPeerID()) break;
                if (peer.isDoingOperation(Peer.Operation.BACKUP)) {
                    System.out.println("\tSTORED Message received   | Type: " + args[1] + ", " +
                                                                     "Sender: " + args[2] + ", " +
                                                                     "Chunk #" + args[4] + ", " +
                                                                     "Number bytes: " + length);
                    System.out.flush();
                    peer.executeThread(new StoredChunkReceiver(message, peer));
                }
                break;


            case "GETCHUNK":
                if (senderId == peer.getPeerID()) break;
                System.out.println("\tGETCHUNK Message received | Type: " + args[1] + ", " +
                                                                 "Sender: " + args[2] + ", " +
                                                                 "Chunk #" + args[4] + ", " +
                                                                 "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new GetChunkReceiver(message, peer));
                break;

            case "CHUNK":
                if (senderId == peer.getPeerID()) break;
                System.out.println("\tCHUNK Message received    | Type: " + args[1] + ", " +
                                                                 "Sender: " + args[2] + ", " +
                                                                 "Chunk #" + args[4] + ", " +
                                                                 "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new ChunkReceiver(message, peer));
                break;


            case "DELETE":
                System.out.println("\tDELETE Message received   | Type: " + args[1] + ", " +
                                                                 "Sender: " + args[2] + ", " +
                                                                 "File ID: " + args[3] + ", " +
                                                                 "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new DeleteReceiver(message, peer));
                break;


            case "REMOVED":
                System.out.println("\tREMOVED Message received  | Type: " + args[1] + ", " +
                                                                 "Sender: " + args[2] + ", " +
                                                                 "File ID: " + args[3] + ", " +
                                                                 "Chunk #" + args[4]);
                System.out.flush();
                peer.executeThread(new RemovedReceiver(message, peer));
                break;

            default:
                System.out.println("\tUnknown Message received  | " + messageStr);
                System.out.flush();
                break;

        }

    }

}
