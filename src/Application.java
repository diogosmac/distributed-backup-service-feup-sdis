import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Application {

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("Usage: <peer ip> <access point>");
            return;
        }

        String peerIp = args[0];

        try {
                Registry registry = LocateRegistry.getRegistry(peerIp);
                PeerActionsInterface peer = (PeerActionsInterface) registry.lookup(args[1]);
                peer.backup();

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
        }

    }
}
