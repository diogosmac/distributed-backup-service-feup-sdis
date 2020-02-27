import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private final static int OK = 0;
    private final static int ERROR = -1;

    private static HashMap<String, String> dnsTable;
    private static AtomicBoolean closeRequested = new AtomicBoolean(false);

    private static DatagramSocket socket;

    private static int openSocket(int port) {

        InetAddress inetAddress;
        try { inetAddress = InetAddress.getLocalHost(); }
        catch (UnknownHostException e) {
            System.out.println("Unknown host");
            return ERROR;
        }

        try { socket = new DatagramSocket(port); }
        catch (SocketException e) {
            System.out.println("Socket exception");
            return ERROR;
        }

        System.out.println("\nServer opened:");
        System.out.println("\tIP Address: " + inetAddress.getHostAddress());
        System.out.println("\tPort: " + port + "\n");

        return OK;

    }

    private static String process(Request request) {

        String[] params = request.getData();

        switch(request.type) {
            case "register":
                if (dnsTable.containsKey(params[0])) { return "ALREADY_REGISTERED"; }
                else {
                    dnsTable.put(params[0], params[1]);
                    return Integer.toString(dnsTable.size());
                }
            case "lookup":
                return dnsTable.getOrDefault(params[0], "NOT_FOUND");
            case "close":
                closeRequested.set(true);
                return "CLOSED_SUCCESSFULLY";
            case "reset":
                dnsTable.clear();
                return "RESET_SUCCESSFULLY";
            default: return "ERROR";
        }

    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Usage: java Server <port number>");
            return;
        }

        dnsTable = new HashMap<String, String>();

        int port = Integer.parseInt(args[0]);
        if (openSocket(port) != OK) {
            System.out.println("An error occurred while creating the server");
            return;
        }

        while (!closeRequested.get()) {

            System.out.println("Waiting for request...");
            byte[] buff = new byte[Request.MAX_SIZE];

            DatagramPacket packet = new DatagramPacket(buff, Request.MAX_SIZE);
            socket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received:\n\t" + received + "\n");

            String[] arguments = received.split(Request.BREAK);
            Request request = Request.fromArgs(arguments);
            String reply = (request != null) ? process(request) : "ERROR";
            byte[] bytesToSend = reply.getBytes();

            DatagramPacket response = new DatagramPacket(
                    bytesToSend, bytesToSend.length,
                    packet.getAddress(), packet.getPort()
            );
            socket.send(response);

        }

    }

}
