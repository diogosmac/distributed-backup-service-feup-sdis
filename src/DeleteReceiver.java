public class DeleteReceiver implements Runnable {

    private final String fileId;
    private final Peer peer;

    public DeleteReceiver(byte[] message, Peer peer) {
        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        this.fileId = args[3];
        this.peer = peer;
    }

    public String buildDeletedFileMessage(String fileId) {
        // <Version> DELETEDFILE <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        return String.join(" ", peer.getProtocolVersion(), "DELETEDFILE",
                Integer.toString(peer.getPeerId()), fileId,
                MyUtils.CRLF + MyUtils.CRLF);
    }

    @Override
    public void run() {

        if (peer.getProtocolVersion().equals("1.0"))
            peer.getChunkOccurrences().deleteOccurrences(fileId);

        peer.getChunkStorage().deleteFile(fileId);

        if (peer.getProtocolVersion().equals("2.0")) {
            String deletedFileMessage = buildDeletedFileMessage(fileId);
            byte[] message = MyUtils.convertStringToByteArray(deletedFileMessage);
            peer.executeThread(new MessageSender(message, peer.getMulticastControlChannel()));
        }

    }

}
