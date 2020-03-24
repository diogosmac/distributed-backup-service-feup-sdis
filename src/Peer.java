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

        if (args.length != 1) {
            System.out.println("Usage: WIP");
            return;
        }
        String remoteObjectName = args[0];

        try {
            Peer obj = new Peer();
            PeerActionsInterface peerInterface = (PeerActionsInterface) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(remoteObjectName, peerInterface);
            System.out.println("Peer ready");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }

    }

    @Override
    public void backup() throws RemoteException {
        System.out.println("[WIP] Backup");
    }

    @Override
    public void restore() throws RemoteException {
        System.out.println("[WIP] Restore");
    }

    @Override
    public void delete() throws RemoteException {
        System.out.println("[WIP] Delete");
    }

    @Override
    public void reclaim() throws RemoteException {
        System.out.println("[WIP] Reclaim");
    }
}