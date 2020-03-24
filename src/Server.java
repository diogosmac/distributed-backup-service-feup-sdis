import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Server implements ServerInterface {

    private static HashMap<String, String> dnsTable;

    public Server() {}

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Usage: java Server <remote_object_name>");
            return;
        }
        String remoteObjectName = args[0];

        try {

            Server obj = new Server();
            ServerInterface serverInterface = (ServerInterface) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(remoteObjectName, serverInterface);
            dnsTable = new HashMap<>();
            System.out.println("Server ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }

    }

    @Override
    public String lookup(String dns_name) throws RemoteException {

        String ret = dnsTable.getOrDefault(dns_name, "NOT_FOUND");

        System.out.println("Lookup " + dns_name + " :: " + ret);
        return ret;

    }

    @Override
    public String register(String dns_name, String ip_address) throws RemoteException {

        String ret;

        if (dnsTable.containsKey(dns_name)) { ret = "ALREADY_REGISTERED"; }
        else {
            dnsTable.put(dns_name, ip_address);
            ret = Integer.toString(dnsTable.size());
        }

        System.out.println("Register " + dns_name + " " + ip_address + " :: " + ret);
        return ret;

    }

    @Override
    public String reset() throws RemoteException {

        dnsTable.clear();
        String ret = "RESET_SUCCESSFULLY";

        System.out.println("Reset :: " + ret);
        return ret;

    }

}
