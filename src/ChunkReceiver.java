import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ChunkReceiver implements Runnable {

    private final byte[] message;
    private final Peer peer;
    private Socket socket;

    public ChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    public boolean buildSocket(String hostName, int port) {
        try {
            this.socket = new Socket(hostName, port);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public byte[] readChunk() {
        byte[] chunk = new byte[MyUtils.CHUNK_SIZE];
        try {
            InputStream iS = this.socket.getInputStream();
            iS.read(chunk, 0, MyUtils.CHUNK_SIZE);
            return chunk;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void run() {
        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>   version = 1.0
        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><TCP Address> <Port>   version = 2.0
        String messageStr = MyUtils.convertByteArrayToString(this.message);
        String[] args = messageStr.split(" ");
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.notRecentlyReceived(fileId, chunkNumber))
            if (this.peer.isDoingOperation(Peer.Operation.RESTORE)) {
                String bodyStr = messageStr.substring(messageStr.indexOf(MyUtils.CRLF + MyUtils.CRLF) + 2);
                byte[] body = new byte[64000];

                if (this.peer.getProtocolVersion().equals("1.0")) {
                    body = MyUtils.convertStringToByteArray(bodyStr);
                } else if (this.peer.getProtocolVersion().equals("2.0")) {
                    if (this.buildSocket(bodyStr, Integer.parseInt(args[6]))) {
                        if ((body = this.readChunk()) == null) {
                            System.out.println("Error while reading from TCP socket");
                            return;
                        }
                    }
                    else {
                        System.out.println("Error while reading from TCP socket");
                        return;
                    }
                }
                this.peer.getFileRestorer().saveRestoredChunk(fileId, chunkNumber, body);
            }

        this.peer.saveReceivedChunkTime(fileId, chunkNumber);
    }

}
