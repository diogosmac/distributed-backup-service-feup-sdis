import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerActionsInterface extends Remote {

    void backup(String filePath, int replicationDegree) throws Exception;

    void restore(String filePath) throws Exception;

    void delete(String filePath) throws Exception;

    void reclaim(int amountOfSpace) throws Exception;

    void state() throws Exception;

}
