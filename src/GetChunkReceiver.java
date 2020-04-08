public class GetChunkReceiver implements Runnable {

    private byte[] message;
    private Peer peer;

    public GetChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    @Override
    public void run() {
        //  <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        String receivedMessage = MyUtils.convertByteArrayToString(this.message);
        String[] args = receivedMessage.split(" ");

        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.hasChunk(fileId, chunkNumber)) {
            Chunk wantedChunk = this.peer.getChunk(fileId, chunkNumber);

            //  <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
            String headerStr = String.join(" ", this.peer.getProtocolVersion(), "CHUNK",
                    Integer.toString(this.peer.getPeerID()), wantedChunk.getFileID(),
                    Integer.toString(wantedChunk.getNum()), MyUtils.CRLF + MyUtils.CRLF);


            byte[] header = MyUtils.convertStringToByteArray(headerStr);
            byte[] chunkMessage = MyUtils.concatByteArrays(header, wantedChunk.getData());

            int msToWait = MyUtils.randomNum(0, 400);

            try {
                Thread.sleep(msToWait);
            } catch (Exception e) {
                System.out.println("I can't sleep yet, there are monsters nearby");
            }

            if (this.peer.notRecentlyReceived(fileId, chunkNumber))
                this.peer.executeThread(new MessageSender(chunkMessage, this.peer.getMulticastDataRestoreChannel()));
        }
    }
}
