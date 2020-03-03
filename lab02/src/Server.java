import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private final static int OK = 0;
    private final static int ERROR = -1;

    private static HashMap<String, String> dnsTable;
    private static AtomicBoolean closeRequested = new AtomicBoolean(false);

    private static DatagramSocket unicast;
    private static String service_addr;

    private static int openUnicast(int port) {

        InetAddress inetAddress;
        try { inetAddress = InetAddress.getLocalHost(); }
        catch (UnknownHostException e) {
            System.out.println("Unknown unicast host");
            return ERROR;
        }

        service_addr = inetAddress.getHostAddress();

        try { unicast = new DatagramSocket(port); }
        catch (SocketException e) {
            System.out.println("Unicast socket exception");
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

        if (args.length != 3) {
            System.out.println("Usage: java Server <srvc_port> <mcast_addr> <mcast_port>");
            return;
        }

        dnsTable = new HashMap<String, String>();

        int srvc_port = Integer.parseInt(args[0]);
        if (openUnicast(srvc_port) != OK) {
            System.out.println("An error occurred while creating the server");
            return;
        }

        //Multicast
        InetAddress mcast_addr = InetAddress.getByName(args[1]);
        int mcast_port = Integer.parseInt(args[2]);
        InetAddress service_addr_inet = InetAddress.getByName(service_addr);
        ServerAdvertiser sa = new ServerAdvertiser(mcast_addr, mcast_port, service_addr_inet, srvc_port);
        //=========

        while (!closeRequested.get()) {

            System.out.println("Waiting for request...\n");
            byte[] buff = new byte[Request.MAX_SIZE];

            DatagramPacket packet = new DatagramPacket(buff, Request.MAX_SIZE);
            unicast.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("\nReceived:\n\t" + received + "\n");

            String[] arguments = received.split(Request.BREAK);
            Request request = Request.fromArgs(arguments);
            String reply = (request != null) ? process(request) : "ERROR";
            byte[] bytesToSend = reply.getBytes();

            DatagramPacket response = new DatagramPacket(
                    bytesToSend, bytesToSend.length,
                    packet.getAddress(), packet.getPort()
            );
            unicast.send(response);

        }

        //Shuts down Advertiser
        sa.shutdown();
    }

}
