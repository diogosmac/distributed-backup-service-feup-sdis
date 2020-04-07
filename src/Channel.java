import java.io.IOException;
import java.net.*;

public class Channel implements Runnable {

    private int PORT;
    protected InetAddress ADDRESS;
    Peer peer;
    private String welcomeMessage;

    public Channel(String inetAddress, int port, Peer peer, String welcomeMessage) {

        try {

            this.ADDRESS = InetAddress.getByName(inetAddress);
            this.PORT = port;
            System.out.println(this.ADDRESS + ":" + this.PORT);
            this.peer = peer;
            this.welcomeMessage = welcomeMessage;

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public void sendMessage(byte[] message) {

        try (DatagramSocket socket = new DatagramSocket()) {

            DatagramPacket messagePacket = new DatagramPacket(message, message.length, ADDRESS, PORT);
            socket.send(messagePacket);
            String msg = new String(message);
            String[] msgParts = msg.split(" ");
            System.out.println("\tMessage sent              | Type: " + msgParts[1] + ", " +
                                                             "Sender: " + msgParts[2] + ", " +
                                                             "Number bytes: " + message.length);
            System.out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {

        try {

            MulticastSocket socket = new MulticastSocket(PORT);
            socket.joinGroup(ADDRESS);
            System.out.println(this.welcomeMessage);

            while (true) {

                byte[] buffer = new byte[MyUtils.CHUNK_SIZE];

                DatagramPacket messagePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(messagePacket);

                peer.executeThread(new MessageReceiver(buffer, peer));

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
