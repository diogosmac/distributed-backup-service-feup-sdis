import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    String lookup() throws RemoteException;
    String register() throws RemoteException;
}
