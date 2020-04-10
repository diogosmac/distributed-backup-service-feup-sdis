public class RemovedReceiver implements Runnable {

    private int senderId;
    private String fileId;
    private int chunkNumber;
    private Peer peer;

    public RemovedReceiver(byte[] message, Peer peer) {
        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        this.senderId = Integer.parseInt(args[2]);
        this.fileId = args[3];
        this.chunkNumber = Integer.parseInt(args[4]);
        this.peer = peer;
    }

    @Override
    public void run() {
        peer.saveChunkDeletion(fileId, chunkNumber, senderId);
        if (!peer.checkChunkReplicationDegree(fileId, chunkNumber)) {
            if (peer.hasChunk(fileId, chunkNumber)){
                int interval = MyUtils.randomNum(0, 400);
                try {

                    Thread.sleep(interval);
                    if (this.peer.noRecentPutChunkMessage(fileId, chunkNumber)) {

                        String header = peer.buildPutchunkHeader(fileId, chunkNumber, peer.getReplicationDegree(fileId));

                        byte[] headerBytes = MyUtils.convertStringToByteArray(header);
                        byte[] chunkBytes = peer.getChunk(fileId, chunkNumber).getData();
                        byte[] putChunkMessage = MyUtils.concatByteArrays(headerBytes, chunkBytes);

                        this.peer.executeThread(new MessageSender(putChunkMessage,
                                this.peer.getMulticastControlChannel()));
                    }

                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

}
