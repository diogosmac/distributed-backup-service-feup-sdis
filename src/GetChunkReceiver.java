import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class GetChunkReceiver implements Runnable {

    private final byte[] message;
    private final Peer peer;
    private ServerSocket tcpSocket;
    private int serverPort;

    public GetChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
    }

    public String buildChunkHeader(String fileId, int chunkNumber) {
        //  <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        return String.join(" ", this.peer.getProtocolVersion(), "CHUNK",
                Integer.toString(this.peer.getPeerId()), fileId, Integer.toString(chunkNumber),
                MyUtils.CRLF + MyUtils.CRLF);
    }

    public byte[] getConnectInfo() {

        try {
            String addr = InetAddress.getLocalHost().getHostAddress();
            String info = addr + " " + (MyUtils.BASE_PORT+this.peer.getPeerId());
            return MyUtils.convertStringToByteArray(info);
        } catch (UnknownHostException e) {
            System.out.println("Failed get connection info");
            return null;
        }
    }

    public boolean openTcpSocket(int port) {
        try {
            this.tcpSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            System.out.println("Error opening TCP socket:" + e.toString());
            return false;
        }
    }

    public void closeTcpSocket() {
        try {
            this.tcpSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing TCP socket: " + e.toString());
        }
    }

    private void sendChunk(byte[] chunk, int size) {
        Socket socket;
        try {
            socket = this.tcpSocket.accept();
            OutputStream oS = socket.getOutputStream();

            String headerStr = size + " ";
            byte[] header = MyUtils.convertStringToByteArray(headerStr);
            byte[] chunkData = MyUtils.trimMessage(chunk, size);

            oS.write(MyUtils.concatByteArrays(header, chunkData));
            oS.close();
            socket.close();
        }
        catch (IOException e) {
            System.out.println("Accept failed: " + this.serverPort);
        }
    }

    @Override
    public void run() {
        //  <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        String receivedMessage = MyUtils.convertByteArrayToString(this.message);
        String[] args = receivedMessage.split(" ");

        String fileId = args[3];
        int chunkNumber = Integer.parseInt(args[4]);

        if (this.peer.getChunkStorage().hasChunk(fileId, chunkNumber)) {

            Chunk wantedChunk = this.peer.getChunkStorage().getChunk(fileId, chunkNumber);
            String headerStr = buildChunkHeader(wantedChunk.getFileID(), wantedChunk.getNum());

            byte[] header = MyUtils.convertStringToByteArray(headerStr);

            int msToWait = MyUtils.randomNum(0, 400);

            try {
                Thread.sleep(msToWait);
            } catch (Exception e) { System.out.println("I can't sleep yet, there are monsters nearby"); }

            if (this.peer.notRecentlyReceived(fileId, chunkNumber))
                if (this.peer.getProtocolVersion().equals("1.0")) {
                    byte[] chunkMessage = MyUtils.concatByteArrays(header, wantedChunk.getData());
                    this.peer.executeThread(new MessageSender(chunkMessage, this.peer.getMulticastDataRestoreChannel()));
                }
                else if (this.peer.getProtocolVersion().equals("2.0")) {
                    if (this.peer.notRecentlyReceived(fileId, chunkNumber)) {
                        byte[] socketInfo;
                        if ((socketInfo = getConnectInfo()) == null)
                            return;

                        byte[] chunkMessage = MyUtils.concatByteArrays(header, socketInfo);
                        if (this.openTcpSocket(MyUtils.BASE_PORT + this.peer.getPeerId())) {
                            this.peer.executeThread(new MessageSender(chunkMessage, this.peer.getMulticastDataRestoreChannel()));
                            System.out.println("Wanted chunk size (1): " + wantedChunk.getSize());
                            this.sendChunk(wantedChunk.getData(), wantedChunk.getSize());
                            System.out.println("Chunk #" + chunkNumber + " Sent!");
                            this.closeTcpSocket();
                        }
                    }
                }
        }
    }

}
