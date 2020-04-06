public class StoredChunkReceiver implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public StoredChunkReceiver(byte[] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {
        String message = new String(this.message);
        String[] args = message.split(" ");
//        String version = args[0];
        int senderId = Integer.parseInt(args[2]);
        // Checks if the message read wasnt send by himself
        System.out.flush();
        if(senderId != this.peer.getPeerID()) {
            String fileId = args[3];
            int chunkNumber = Integer.parseInt(args[4]);
            this.peer.saveChunkOccurrence(fileId, chunkNumber);
            System.out.println("Occurrences updated");
            System.out.flush();
        } else {
            System.out.println("\tSkipping: My own message");
            System.out.flush();
        }

    }

}
