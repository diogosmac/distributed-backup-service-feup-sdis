import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class GetChunkReceiver implements Runnable {

    private final byte[] message;
    private final Peer peer;
    private ServerSocket tcpSocket;
    private int serverPort;

    public GetChunkReceiver(byte[] message, Peer peer) {
        this.message = message;
        this.peer = peer;
        if (this.peer.getProtocolVersion().equals("2.0"))
            this.serverPort = MyUtils.BASE_PORT + this.peer.getPeerId();
    }

    public String buildChunkHeader(String fileId, int chunkNumber) {
        //  <Version> CHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
        return String.join(" ", this.peer.getProtocolVersion(), "CHUNK",
                Integer.toString(this.peer.getPeerId()), fileId, Integer.toString(chunkNumber),
                MyUtils.CRLF + MyUtils.CRLF);
    }

    public byte [] getConnectInfo() {

        try {
            String addr = InetAddress.getLocalHost().getHostAddress();
            String info = addr + " " + (MyUtils.BASE_PORT+this.peer.getPeerId());
            return MyUtils.convertStringToByteArray(info);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean openTcpSocket() {
        try {
            this.tcpSocket = new ServerSocket(this.serverPort);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendChunk(byte[] chunk) {
        Socket socket;
        try {
            socket = this.tcpSocket.accept();
            OutputStream oS = socket.getOutputStream();
            oS.write(chunk);
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
                    byte[] socketInfo;
                    if ((socketInfo = getConnectInfo()) == null) {
                        System.out.println("Failed to open and send chunk via TCP");
                        return;
                    }
                    byte[] chunkMessage = MyUtils.concatByteArrays(header, socketInfo);
                    if (this.openTcpSocket()) {
                        this.peer.executeThread(new MessageSender(chunkMessage, this.peer.getMulticastDataRestoreChannel()));
                        this.sendChunk(wantedChunk.getData());
                    } else {
                        System.out.println("Failed to open and send chunk via TCP");
                    }
                }
        }
    }

}
