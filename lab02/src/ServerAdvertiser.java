import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ServerAdvertiser implements Runnable
{
    private static MulticastSocket multicast;
    private final static int OK = 0;
    private final static int ERROR = -1;

    private InetAddress server_advertiser_addr;
    private int server_advertiser_port;
    private InetAddress server_service_addr;
    private int server_service_port;
    private ScheduledExecutorService service;

    private static int openMulticast() {

        try {
            multicast = new MulticastSocket();
            multicast.setTimeToLive(1);
        }
        catch (IOException e) {
            System.out.println("Multicast socket exception");
            return ERROR;
        }

        return OK;
    }

    public void shutdown() {
        if (this.service != null)
            this.service.shutdown();

        multicast.close();
    }

    ServerAdvertiser(InetAddress mcast_addr, int mcast_port, InetAddress srvc_addr, int srvc_port) {
        server_advertiser_addr = mcast_addr;
        server_advertiser_port = mcast_port;
        server_service_addr = srvc_addr;
        server_service_port = srvc_port;

        if (openMulticast() != OK) {
            System.out.println("An error occurred while opening the advertising socket");
            return;
        }

        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
    }

    byte[] buildAd() {
        String ad = this.server_advertiser_addr.toString() + " "
                +   this.server_advertiser_port + ":"
                +   this.server_service_addr + " "
                +   this.server_service_port;

        return ad.getBytes();
    }

    @Override
    public void run() {
        byte[] bytesToSend = this.buildAd();

        DatagramPacket multicast_info = new DatagramPacket(
                bytesToSend, bytesToSend.length, server_advertiser_addr, server_advertiser_port);

        try { multicast.send(multicast_info); }
        catch (IOException e) {
            System.out.println("Couldn't send advertisement packet");
        }

        System.out.println("[Multicast] Info: " + new String(this.buildAd()));
    }
}