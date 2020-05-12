package messages.replies;

import peer.Peer;
import utils.MyUtils;

public class StoredChunkReceiver implements Runnable {

    private final byte[] message;
    private final Peer peer;

    public StoredChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {
        String message = MyUtils.convertByteArrayToString(this.message);
        String[] args = message.split(" ");
        int senderId = Integer.parseInt(args[2]);
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);
        this.peer.getChunkOccurrences().saveChunkOccurrence(fileId, chunkNumber, senderId);
        System.out.println("\t\tOccurrences updated > " + fileId + "[" + chunkNumber + "]");
        System.out.flush();
    }

}
