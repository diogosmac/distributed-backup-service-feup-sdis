import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

class Peer implements PeerActionsInterface {

    public Peer() {
        //Channel setup
    }

    public static void main(String[] args) throws IOException {

        if (args.length != 3) {
            System.out.println("Usage: <protocol version> <peer id> <service access point>");
            return;
        }

        String version = args[0];
        int id = Integer.parseInt(args[1]);
        String accessPoint = args[2];

        try {
            Peer obj = new Peer();
            PeerActionsInterface peerInterface = (PeerActionsInterface) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, peerInterface);
            System.out.println("Peer "+id+" ready. v" + version + " accessPoint: " +  accessPoint);

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }

    }

    @Override
    public void backup(String filePath, int replicationDegree) throws RemoteException {
        System.out.println("[WIP] Backup");
    }

    @Override
    public void restore(String filePath) throws RemoteException {
        System.out.println("[WIP] Restore");
    }

    @Override
    public void delete(String filePath) throws RemoteException {
        System.out.println("[WIP] Delete");
    }

    @Override
    public void reclaim(int amountOfSpace) throws RemoteException {
        System.out.println("[WIP] Reclaim");
    }

    @Override
    public void state() throws RemoteException {
        System.out.println("[WIP] Reclaim");
    }
}