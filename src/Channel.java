import java.io.IOException;
import java.net.*;

public class Channel implements Runnable {

    private int PORT;
    protected InetAddress ADDRESS;
    Peer peer;

    public Channel(String inetAddress, int port, Peer peer) {

        try {

            this.ADDRESS = InetAddress.getByName(inetAddress);
            this.PORT = port;
            this.peer = peer;

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public void sendMessage(byte[] message) {

        try (DatagramSocket socket = new DatagramSocket()) {

            DatagramPacket messagePacket = new DatagramPacket(message, message.length, ADDRESS, PORT);
            socket.send(messagePacket);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {

        try {

            MulticastSocket socket = new MulticastSocket(PORT);
            socket.joinGroup(ADDRESS);

            while (true) {

                byte[] buffer = new byte[MyUtils.CHUNK_SIZE];

                DatagramPacket messagePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(messagePacket);

                int length = 0;
                for (int i = buffer.length - 1; i > 0; i--) {
                    if (buffer[i] != 0) {
                        length = i + 1;
                        break;
                    }
                }

                peer.executeWithScheduler(new MessageReceivingThread(buffer, length, peer));

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
