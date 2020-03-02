import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

public class Client {

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Usage: Client <mcast_addr> <mcast_port> <oper> <opnd> * ");
            return;
        }

        //Multicast Address
        InetAddress mcast_addr = InetAddress.getByName(args[0]);

        //Multicast Port
        int mcast_port = Integer.parseInt(args[1]);

        //Socket
        MulticastSocket mcast_socket  = new MulticastSocket(mcast_port);
        mcast_socket.joinGroup(mcast_addr);

        System.out.println("Juntei-me ao grupo!");

        //Read from socket
        byte[] service_info_buff = new byte[Request.MAX_SIZE];
        DatagramPacket mcast_response = new DatagramPacket(service_info_buff, Request.MAX_SIZE);
        mcast_socket.receive(mcast_response);

        //Handles responseDatagramPacket
        String mcasted_info = new String(mcast_response.getData(), 0, mcast_response.getLength());
        System.out.println("Multicasted Info:\t" + mcasted_info + "\n");

        //Handles multicasted info

        // info_split[0] => "<multicast_addr> <multicast_port>"
        // info_split[1] => "<service_addr> <service_port>"
        String [] info_split = mcasted_info.split(":");

        // service_info[0] => service_addr
        // service_info[1] => service_port
        String [] service_info = info_split[1].split(" ");

        //substring used to remove '/' from address
        InetAddress service_addr = InetAddress.getByName(service_info[0].substring(1));
        int service_port = Integer.parseInt(service_info[1]);


        //Handles request creation and sends to server service
        DatagramSocket unicast_socket = new DatagramSocket();

        Request request = Request.fromArgs(
                Arrays.copyOfRange(args, 2, args.length));
        if (request == null) return;

        byte[] bytesToSend;
        bytesToSend = request.toString().getBytes();

        DatagramPacket packet = new DatagramPacket(bytesToSend, bytesToSend.length, service_addr, service_port);

        System.out.println("\nSending request to:\t" + service_addr.getHostAddress() + ":" + service_port);
        System.out.println("Operation:\t\t" + request.toString() + "\n");
        unicast_socket.send(packet);

        byte[] buff = new byte[Request.MAX_SIZE];
        DatagramPacket response = new DatagramPacket(buff, Request.MAX_SIZE);

        unicast_socket.receive(response);

        String received = new String(response.getData(), 0, response.getLength());
        System.out.println("Client:\t" + request.toString() + " : " + received + "\n");
    }

}
