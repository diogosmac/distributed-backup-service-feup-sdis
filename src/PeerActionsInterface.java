import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerActionsInterface extends Remote  {
    void backup(String filePath, int replicationDegree) throws RemoteException;
    void restore(String filePath) throws RemoteException;
    void delete(String filePath) throws RemoteException;
    void reclaim(int amountOfSpace) throws RemoteException;
    void state() throws RemoteException;
}