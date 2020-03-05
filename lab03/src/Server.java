import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements ServerInterface {

    private final static int OK = 0;
    private final static int ERROR = -1;

    private static HashMap<String, String> dnsTable;
    private static AtomicBoolean closeRequested = new AtomicBoolean(false);

    public Server() {}

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
            System.out.println("Usage: java Server <remote_object_name>");
            return;
        }

        dnsTable = new HashMap<String, String>();

        try {
            Server obj = new Server();
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("Hello", stub);
            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }

    }

    @Override
    public String lookup() throws RemoteException {
        return "ayy";
    }

    @Override
    public String register() throws RemoteException {
        return null;
    }
}
