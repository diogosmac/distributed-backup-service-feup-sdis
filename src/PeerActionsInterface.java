import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerActionsInterface extends Remote  {
    void backup() throws RemoteException;
    void restore() throws RemoteException;
    void delete() throws RemoteException;
    void reclaim() throws RemoteException;
}