import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private final static int OK = 0;
    private final static int ERROR = -1;

    private static HashMap<String, String> dnsTable;
    private static AtomicBoolean closeRequested = new AtomicBoolean(false);

    private static MulticastSocket multicast;
    private static DatagramSocket unicast;

    private static int openUnicast(int port) {

        InetAddress inetAddress;
        try { inetAddress = InetAddress.getLocalHost(); }
        catch (UnknownHostException e) {
            System.out.println("Unknown unicast host");
            return ERROR;
        }

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

    private static int openMulticast(String mcast_ip, int mcast_port) {

        InetAddress mcast_addr;
        try { mcast_addr = InetAddress.getByName(mcast_ip); }
        catch (UnknownHostException e) {
            System.out.println("Unknown host");
            return ERROR;
        }

        SocketAddress sock_addr = new InetSocketAddress(mcast_addr, mcast_port);

        try {
            multicast = new MulticastSocket(sock_addr);
            multicast.setTimeToLive(1);
        }
        catch (IOException e) {
            System.out.println("Multicast socket exception");
            return ERROR;
        }

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

        String mcast_addr = args[1];
        int mcast_port = Integer.parseInt(args[2]);
        if (openMulticast(mcast_addr, mcast_port) != OK) {
            System.out.println("An error occurred while opening the advertising socket");
            return;
        }

        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        Runnable advertise = () -> {
            String toSend = unicast.getInetAddress().getHostAddress() + ' ' + unicast.getPort();
            byte[] bytesToSend = toSend.getBytes();

            DatagramPacket multicast_info = new DatagramPacket(
                    bytesToSend, bytesToSend.length);

            try { multicast.send(multicast_info); }
            catch (IOException e) {
                System.out.println("Couldn't send advertisement packet");
            }
        };
        ses.scheduleAtFixedRate(advertise, 1, 1, TimeUnit.SECONDS);

//        class ScheduleAdvertisements implements Runnable {
//
//            String unic_addr;
//            int unic_port;
//
//            ScheduleAdvertisements(String addr, int port) {
//                this.unic_addr = addr;
//                this.unic_port = port;
//            }
//
//            public void alert() {
//                    String toSend = unic_addr + ' ' + unic_port;
//                    byte[] bytesToSend = toSend.getBytes();
//
//                    DatagramPacket multicast_info = new DatagramPacket(
//                            bytesToSend, bytesToSend.length);
//
//                    try { multicast.send(multicast_info); }
//                    catch (IOException e) {
//                        System.out.println("Couldn't send advertisement packet");
//                    }
//
//                    this.run();
//            }
//
//            public void run() {
//                ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
//                executorService.schedule((Runnable) this::alert, 1, TimeUnit.SECONDS);
//            }
//
//        }
//
//        Thread advertisementThread = new Thread(new ScheduleAdvertisements(
//                unicast.getInetAddress().getHostAddress(), unicast.getPort()
//        ));
//        advertisementThread.start();

        while (!closeRequested.get()) {

            System.out.println("Waiting for request...");
            byte[] buff = new byte[Request.MAX_SIZE];

            DatagramPacket packet = new DatagramPacket(buff, Request.MAX_SIZE);
            unicast.receive(packet);
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
            unicast.send(response);

        }

    }

}
