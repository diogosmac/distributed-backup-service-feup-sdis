public class DeleteReceiver implements Runnable {

    private final String fileId;
    private final Peer peer;

    public DeleteReceiver(byte[] message, Peer peer) {
        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        this.fileId = args[3];
        this.peer = peer;
    }

    @Override
    public void run() {
        peer.getChunkOccurrences().deleteOccurrences(fileId);
        peer.getChunkStorage().deleteFile(fileId);
    }

}
