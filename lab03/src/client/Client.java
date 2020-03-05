package client;

import server.ServerInterface;
import request.Request;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface stub = (ServerInterface) registry.lookup("Hello");
            String response = stub.lookup();
            System.out.println("response: " + response);
        } catch (Exception e) {
            System.err.println("client.Client exception: " + e.toString());
            return;
        }

    }

}
