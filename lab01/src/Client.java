package com.pedro.sdis;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    //args[0] => is the DNS name (or the IP address, in the dotted decimal format) where the server is running
    //args[1] => is the port number where the server is providing service
    //args[2] =>  is the operation to request from the server, either "register" or "lookup"
    //args[>3] => is the list of operands of that operation
                    //<DNS name> <IP address> for register
                    //<DNS name> for lookup

    public static void main(String[] args) throws IOException {
        if(args.length != 4 && args.length != 5 ) {
            System.out.println("Usage: Client <host> <port> <oper> <opnd>*");
            return;
        }
        else {
            System.out.println("Success!");

            InetAddress address = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);

//            InetAddress localHost = InetAddress.getLocalHost();
            DatagramSocket socket = new DatagramSocket();

            byte[] bytesToSend;

            if(args[2].equals("register")) {

                if(args.length != 5) {
                    System.out.println("Missing parameters");
                    return;
                }
                String message = args[2] + args[3] + args[4];
                 bytesToSend = message.getBytes();
            }
            else if(args[2].equals("lookup") ) {
                String message = args[2] + args[3];
                bytesToSend = message.getBytes();
            }
            else {
                System.out.println("Invalid operation");
                return;
            }

            DatagramPacket packet = new DatagramPacket(bytesToSend, bytesToSend.length, address, port);

            socket.send(packet);

            System.out.println("Sent?");
            System.out.println("Porta: " + port);
            System.out.println("Address: " + address);
            System.out.println("Operation: " + args[2]);
        }
    }
}
