import java.io.File;

public class DeleteReceiver implements Runnable {

    private final String fileId;
    private final Peer peer;


    public DeleteReceiver(byte[] message, Peer peer) {
        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        this.fileId = args[3];
        this.peer = peer;
    }

    public boolean DeleteBacklog () {
        File DeleteBacklog = new File(MyUtils.getPeerPath(this.peer) + MyUtils.DEFAULT_DELETE_BACKLOG_PATH);
        if (DeleteBacklog.exists()) {
            if (!DeleteBacklog.delete())
                return false;
        }

        return true;
    }

    @Override
    public void run() {
        if(!DeleteBacklog())
            System.out.println("Error removing delete backlog!");

        peer.getChunkOccurrences().deleteOccurrences(fileId);
        peer.getChunkStorage().deleteFile(fileId);
    }

}
