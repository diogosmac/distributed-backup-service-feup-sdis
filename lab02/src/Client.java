import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

public class Client {

    // args[0] => is the DNS name (or the IP address, in the dotted decimal format) where the server is running
    // args[1] => is the port number where the server is providing service
    // args[2] =>  is the operation to request from the server, either "register" or "lookup"
    // args[>3] => is the list of operands of that operation
    // <DNS name> <IP address> for register
    // <DNS name> for lookup

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

        System.out.println("Readd");
        //Handles responseDatagramPacket
        String service_info = new String(mcast_response.getData(), 0, mcast_response.getLength());
        System.out.println("Service Info:\t" + service_info + "\n");


//        DatagramSocket unicast_socket = new DatagramSocket();
//
//        Request request = Request.fromArgs(
//                Arrays.copyOfRange(args, 2, args.length));
//        if (request == null) return;
//
//        byte[] bytesToSend;
//        bytesToSend = request.toString().getBytes();
//
//        DatagramPacket packet = new DatagramPacket(bytesToSend, bytesToSend.length, address, port);
//
//        System.out.println("\nSending request to:\t" + address.getHostAddress() + ":" + port);
//        System.out.println("Operation:\t\t" + request.toString() + "\n");
//        unicast_socket.send(packet);
//
//        byte[] buff = new byte[Request.MAX_SIZE];
//        DatagramPacket response = new DatagramPacket(buff, Request.MAX_SIZE);
//
//        unicast_socket.receive(response);
//
//        String received = new String(response.getData(), 0, response.getLength());
//        System.out.println("Client:\t" + request.toString() + " : " + received + "\n");

    }

}
