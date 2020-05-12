package messages.replies;

import messages.MessageSender;
import peer.Peer;
import utils.MyUtils;

public class RemovedReceiver implements Runnable {

    private final int senderId;
    private final String fileId;
    private final int chunkNumber;
    private final Peer peer;

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
        peer.getChunkOccurrences().saveChunkDeletion(fileId, chunkNumber, senderId);
        if (!peer.getChunkOccurrences().checkChunkReplicationDegree(fileId, chunkNumber)) {
            if (peer.getChunkStorage().hasChunk(fileId, chunkNumber)){
                int interval = MyUtils.randomNum(0, 400);
                try {

                    Thread.sleep(interval);
                    if (this.peer.noRecentPutChunkMessage(fileId, chunkNumber)) {

                        String header = peer.buildPutchunkHeader(
                                fileId, chunkNumber, peer.getChunkOccurrences().getReplicationDegree(fileId));

                        byte[] headerBytes = MyUtils.convertStringToByteArray(header);
                        byte[] chunkBytes = peer.getChunkStorage().getChunk(fileId, chunkNumber).getData();
                        byte[] putChunkMessage = MyUtils.concatByteArrays(headerBytes, chunkBytes);

                        this.peer.executeThread(new MessageSender(putChunkMessage,
                                this.peer.getMulticastControlChannel()));
                    }

                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

}
