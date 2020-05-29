package peer;

import chord.ChordNode;
import storage.ChunkStorage;
import storage.FileRestorer;
import storage.Occurrences;
import utils.MyUtils;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements PeerActionsInterface {

    private final int peerId;
    private final ChordNode node;

    private final ChunkStorage chunkStorage;
    private final Occurrences fileOccurrences;

    private final FileRestorer fileRestorer;

    public Peer(ChordNode node) {

        this.peerId = node.getID();
        this.node = node;
        this.node.setPeer(this);

        this.chunkStorage = new ChunkStorage(this);
        this.fileOccurrences = new Occurrences(this);
        this.fileRestorer = new FileRestorer(MyUtils.getRestorePath(this));

        this.initRMI();

    }

    public int getPeerId() {
        return this.peerId;
    }

    public ChordNode getNode() {
        return this.node;
    }

    @Override
    public void backup(String filePath, int replicationDegree) {
        this.node.initiateBackup(filePath, replicationDegree);
    }

    @Override
    public void restore(String filePath) {
        this.node.initiateRestore(filePath);
    }

    @Override
    public void delete(String filePath) {
        this.node.initiateDelete(filePath);
    }

    @Override
    public void reclaim(int amountOfSpace) {
        int freed = this.node.initiateReclaim(amountOfSpace);
        System.out.println("Freed " + freed * 0.001 + " kB of space");
    }

    @Override
    public String state() {
        String header = "\nSTATE: Node " + this.node.getID() + "\n\n";
        String peerMemoryInfo = this.chunkStorage.getMemoryInfo();
        String backupFilesInfo = this.fileOccurrences.getOccurrencesInfo();
        String storedChunksInfo = this.chunkStorage.getChunkInfo();
        return header + peerMemoryInfo + backupFilesInfo + storedChunksInfo;
    }

    public synchronized ChunkStorage getChunkStorage() {
        return this.chunkStorage;
    }

    public synchronized Occurrences getFileOccurrences() {
        return this.fileOccurrences;
    }

    public synchronized FileRestorer getFileRestorer() {
        return this.fileRestorer;
    }

    public void initRMI() {
        try {
            PeerActionsInterface peerInterface = (PeerActionsInterface) UnicastRemoteObject.exportObject(this, 0);

            int id = this.getPeerId();
            String accessPoint = "ap" + id;

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, peerInterface);
            System.out.println("\n[RMI] Node " + id + " ready. AccessPoint: " + accessPoint);

        } catch (Exception e) {
            System.err.println("Error initializing RMI: " + e.toString());
        }
    }

}
