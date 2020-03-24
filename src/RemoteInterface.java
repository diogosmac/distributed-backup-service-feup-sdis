import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    String lookup(String dns_name) throws RemoteException;
    String register(String dns_name, String ip_address) throws RemoteException;
    String reset() throws RemoteException;
}
