public class GetChunkReceiver implements Runnable {

    private byte[] message;
    private int length;
    private Peer peer;

    public GetChunkReceiver(byte[] message, int length, Peer peer) {
        this.message = message;
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {
        //  <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        String receivedMessage = new String(this.message);
        String[] args = receivedMessage.split(" ");

        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.hasChunk(fileId, chunkNumber)) {
            Chunk wantedChunk = this.peer.getChunk(fileId, chunkNumber);

            //  <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
            String headerStr = String.join(" ", this.peer.getProtocolVersion(), "CHUNK",
                    Integer.toString(this.peer.getPeerID()), wantedChunk.getFileID(),
                    Integer.toString(wantedChunk.getNum()), MyUtils.CRLF + MyUtils.CRLF);


            byte[] header = headerStr.getBytes();
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
