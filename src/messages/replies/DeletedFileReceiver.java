package messages.replies;

import peer.Peer;
import utils.MyUtils;

public class DeletedFileReceiver implements Runnable {

    private final String fileId;
    private final int sender;
    private final Peer peer;

    public DeletedFileReceiver(byte[] message, Peer peer) {
        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        this.sender = Integer.parseInt(args[2]);
        this.fileId = args[3];
        this.peer = peer;
    }

    @Override
    public void run() {
        if (peer.getProtocolVersion().equals("1.0")) return;
        boolean deletedLastOfFile = peer.getChunkOccurrences().handleDeletedFile(sender, fileId);
        if (deletedLastOfFile) {
            peer.getChunkOccurrences().deleteOccurrences(fileId);
            System.out.println("\tAll occurrences of " + fileId + " have been deleted.");
            peer.concludeDelete(fileId);
        }
    }

}
