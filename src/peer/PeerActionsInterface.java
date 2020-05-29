package peer;

import java.rmi.Remote;

public interface PeerActionsInterface extends Remote {

    void backup(String filePath, int replicationDegree);

    void restore(String filePath);

    void delete(String filePath);

    void reclaim(int amountOfSpace);

    String state();

}
