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
            System.out.println("Error while reading from TCP socket: " + e.toString());
            return false;
        }

        return true;
    }

    public void closeSocket() {
        try {
            this.socket.close();
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.toString());
        }
    }

    public byte[] readChunk() {
        byte[] chunk = new byte[MyUtils.CHUNK_SIZE + 1000];
        try {
            InputStream iS = this.socket.getInputStream();
            int nRead = iS.read(chunk, 0, MyUtils.CHUNK_SIZE + 1000);

            String chunkStr = MyUtils.convertByteArrayToString(MyUtils.trimMessage(chunk, nRead));
            int spaceIndex = chunkStr.indexOf(" ");
            int chunkSize = Integer.parseInt(chunkStr.substring(0, spaceIndex));
            String dataStr = chunkStr.substring(spaceIndex + 1);
            byte[] data = MyUtils.convertStringToByteArray(dataStr);

            if(chunkSize == data.length)  // No lost data
                return data;
            else
                return null;

        } catch (IOException e) {
            System.out.println("Error while reading from TCP socket: " + e.toString());
            return null;
        }
    }

    @Override
    public void run() {
        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>   version = 1.0
        // <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><TCP Address> <Port>   version = 2.0
        String messageStr = MyUtils.convertByteArrayToString(this.message);
        String[] args = messageStr.split(" ");
        String receivedProtocolVersion = args[0];
        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.notRecentlyReceived(fileId, chunkNumber))
            if (this.peer.isDoingOperation(Peer.Operation.RESTORE)) {
                String bodyStr = messageStr.substring(messageStr.indexOf(MyUtils.CRLF + MyUtils.CRLF) + 2);
                byte[] body = new byte[64000];

                if (this.peer.getProtocolVersion().equals("2.0") && receivedProtocolVersion.equals("2.0")) {
                    String address = bodyStr.substring(0, bodyStr.indexOf(" "));
                    if (this.buildSocket(address, Integer.parseInt(args[6]))) {
                        if ((body = this.readChunk()) == null) {
                            return;
                        }
                    }
                    else
                        return;

                    this.closeSocket();
                }
                else
                    body = MyUtils.convertStringToByteArray(bodyStr);

                this.peer.getFileRestorer().saveRestoredChunk(fileId, chunkNumber, body);
            }

        this.peer.saveReceivedChunkTime(fileId, chunkNumber);
    }

}
