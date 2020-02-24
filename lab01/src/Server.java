import java.io.IOException;
import java.net.*;

public class Server {

    private final static int OK = 0;
    private final static int ERROR = -1;
    private final static int MAX_SIZE = 256;


    private static DatagramSocket socket;

    private static int openSocket(int port) {

        InetAddress inetAddress;
        try { inetAddress = InetAddress.getLocalHost(); }
        catch (UnknownHostException e) {
            System.out.println("Unknown host");
            return ERROR;
        }

//        try { socket = new DatagramSocket(port, inetAddress); }
        try { socket = new DatagramSocket(port); }
        catch (SocketException e) {
            System.out.println("Socket exception");
            return ERROR;
        }

        System.out.println("\nSocket created:");
        System.out.println("\tIP Address: " + inetAddress.getHostAddress());
        System.out.println("\tPort: " + port);
        System.out.println();

        return OK;

    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java Server <port number>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        if (openSocket(port) != OK) {
            System.out.println("An error occurred while creating the server");
            return;
        };

        for (int i = 0; i < 5; i++) {

            byte[] buff = new byte[MAX_SIZE];
            DatagramPacket packet = new DatagramPacket(buff, MAX_SIZE);

            try { socket.receive(packet); }
            catch (IOException e) {
                System.out.println("Exception on the received packet");
                continue;
            }

            int bytesReceived = packet.getLength();
            System.out.println("Received " + bytesReceived + " bytes");

        }

    }

}
