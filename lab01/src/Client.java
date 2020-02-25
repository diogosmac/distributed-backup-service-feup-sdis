import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
            System.out.println("Usage: Client <host> <port> <oper> <opnd>*");
            return;
        }

        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        DatagramSocket socket = new DatagramSocket();

        Request request = Request.fromArgs(
                Arrays.copyOfRange(args, 2, args.length));
        if (request == null) return;

        byte[] bytesToSend;
        bytesToSend = request.toString().getBytes();

        DatagramPacket packet = new DatagramPacket(bytesToSend, bytesToSend.length, address, port);

        System.out.println("\nSending request to:\t" + address.getHostAddress() + ":" + port);
        System.out.println("Operation:\t\t" + request.toString() + "\n");
        socket.send(packet);

        byte[] buff = new byte[Request.MAX_SIZE];
        DatagramPacket response = new DatagramPacket(buff, Request.MAX_SIZE);

        socket.receive(response);

        String received = new String(response.getData(), 0, response.getLength());
        System.out.println("Client:\t" + request.toString() + " : " + received + "\n");

    }

}
