public class HelloWorldReceiver implements Runnable {

    private final int sender;
    private final Peer peer;

    public HelloWorldReceiver(byte[] message, Peer peer) {
        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        this.sender = Integer.parseInt(args[2]);
        this.peer = peer;
    }

    @Override
    public void run() {
        if (peer.getProtocolVersion().equals("1.0")) return;
        for (String fileToDelete : peer.getScheduledDeletes()) {
            if (peer.getChunkOccurrences().peerHasFile(sender, fileToDelete)) {
                String deleteMessage = peer.buildDeleteHeader(fileToDelete);
                byte[] message = MyUtils.convertStringToByteArray(deleteMessage);
                peer.executeThread(new MessageSender(message, peer.getMulticastControlChannel()));
            }
        }
    }

}
