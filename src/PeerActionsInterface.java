import java.rmi.Remote;

public interface PeerActionsInterface extends Remote {

    void backup(String filePath, int replicationDegree) throws Exception;

    void restore(String filePath) throws Exception;

    void delete(String filePath) throws Exception;

    void reclaim(int amountOfSpace) throws Exception;

    String state() throws Exception;

}
