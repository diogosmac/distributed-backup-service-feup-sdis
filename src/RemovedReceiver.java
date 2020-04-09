public class RemovedReceiver implements Runnable {

    private String fileId;
    private int chunkNumber;
    private Peer peer;

    public RemovedReceiver(byte[] message, Peer peer) {
        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        this.fileId = args[3];
        this.chunkNumber = Integer.parseInt(args[4]);
        this.peer = peer;
    }

    @Override
    public void run() {
        peer.saveChunkDeletion(fileId, chunkNumber);
        if (!peer.checkChunkReplicationDegree(fileId, chunkNumber)) {
            int interval = MyUtils.randomNum(0, 400);
            try {
                Thread.sleep(interval);
                peer.backup(peer.getFileName(fileId), peer.getReplicationDegree(fileId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
