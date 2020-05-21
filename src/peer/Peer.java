package peer;

import chord.ChordNode;
import storage.*;
import utils.MyUtils;

import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Peer implements PeerActionsInterface {

    private final int peerId;
    private final ChordNode node;

    private final ChunkStorage chunkStorage;
    private final Occurrences fileOccurrences;

    // key    :     fileId:chunkNumber
    // value  :     time of read
//    private final ConcurrentHashMap<String, Long> receivedChunks;

    // key    :     fileId:chunkNumber
    // value  :     time of read
//    private final ConcurrentHashMap<String, Long> putChunkMessagesReclaim;

    private final FileRestorer fileRestorer;

    // elements are the IDs of the files to be deleted
//    private final List<String> scheduledDeletes;

    public Peer(ChordNode node) {

        this.peerId = node.getID();
        this.node = node;
        this.node.setPeer(this);

        this.chunkStorage = new ChunkStorage(this);
        this.fileOccurrences = new Occurrences(this);
//        this.receivedChunks = new ConcurrentHashMap<>();
//        this.putChunkMessagesReclaim = new ConcurrentHashMap<>();
        this.fileRestorer = new FileRestorer(MyUtils.getRestorePath(this));

        this.initRMI();

//        this.scheduledDeletes = new ArrayList<>();
//        this.loadScheduledDeletes(this.scheduledDeletes);
    }

    public int getPeerId() { return this.peerId; }

    public ChordNode getNode() { return this.node; }

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
        this.node.initiateReclaim(amountOfSpace);
    }

    @Override
    public String state() {
        String header = "\nSTATE: Node " + this.node.getID() + "\n\n";
        String peerMemoryInfo = this.chunkStorage.getMemoryInfo();
        String backupFilesInfo = this.fileOccurrences.getOccurrencesInfo();
        String storedChunksInfo = this.chunkStorage.getChunkInfo();
        return header + peerMemoryInfo + backupFilesInfo + storedChunksInfo;
    }

    public ChunkStorage getChunkStorage() { return this.chunkStorage; }

    public Occurrences getFileOccurrences() { return this.fileOccurrences; }

    public FileRestorer getFileRestorer() { return this.fileRestorer; }

//    public void saveReceivedChunkTime(String fileId, int chunkNumber) {
//        String key = String.join(":", fileId, Integer.toString(chunkNumber));
//        this.receivedChunks.put(key, System.currentTimeMillis());
//    }
//
//    public boolean notRecentlyReceived(String fileId, int chunkNumber) {
//        String key = String.join(":", fileId, Integer.toString(chunkNumber));
//        if (this.receivedChunks.containsKey(key)) {
//            long value = this.receivedChunks.get(key);
//            return (System.currentTimeMillis() - value) >= 400;
//        }
//
//        return true;
//    }
//
//    public void logPutChunkMessage(String fileId, int chunkNumber) {
//        String key = String.join(":", fileId, Integer.toString(chunkNumber));
//        this.putChunkMessagesReclaim.put(key, System.currentTimeMillis());
//    }
//
//    public boolean noRecentPutChunkMessage(String fileId, int chunkNumber) {
//        String key = String.join(":", fileId, Integer.toString(chunkNumber));
//        if (this.putChunkMessagesReclaim.containsKey(key)) {
//            long value = this.putChunkMessagesReclaim.get(key);
//            return (System.currentTimeMillis() - value) >= 400;
//        }
//        return true;
//    }
//
//    public List<String> getScheduledDeletes() { return this.scheduledDeletes; }
//
//    public void scheduleDelete(String fileId) {
//        if (!this.scheduledDeletes.contains(fileId)) {
//            this.scheduledDeletes.add(fileId);
//            this.saveScheduledDeletes();
//        }
//    }
//
//    public void concludeDelete(String fileId) {
//        if (this.scheduledDeletes.contains(fileId)) {
//            this.scheduledDeletes.remove(fileId);
//            this.saveScheduledDeletes();
//        }
//    }
//
//    private void saveScheduledDeletes() {
//        String dirPath = MyUtils.getPeerPath(this);
//        File file = new File(String.join("/", dirPath, MyUtils.DEFAULT_DELETE_BACKLOG_PATH));
//        if (file.getParentFile().mkdirs())
//            System.out.println("\tCreated " + dirPath + " directory.");
//
//        try {
//            PrintWriter writer = new PrintWriter(file);
//            StringBuilder output = new StringBuilder();
//            for (String fileId : this.scheduledDeletes) {
//                output.append(fileId).append("\n");
//            }
//            writer.print(output.toString());
//            writer.close();
//        } catch (Exception e) {
//            System.out.println("Exception while writing scheduled deletes to file: " + e.toString()); }
//    }
//
//    private void loadScheduledDeletes(List<String> scheduledDeletes) {
//        String dirPath = MyUtils.getPeerPath(this);
//        File file = new File(String.join("/", dirPath, MyUtils.DEFAULT_DELETE_BACKLOG_PATH));
//        if (file.exists()) {
//            try {
//                BufferedReader br = new BufferedReader(new FileReader(file));
//                String fileId;
//                while ((fileId = br.readLine()) != null) {
//                    scheduledDeletes.add(fileId);
//                }
//                br.close();
//            } catch (Exception e) {
//                System.out.println("Exception while reading from file: " + e.toString());
//            }
//        }
//    }

    public void initRMI() {
        try {
            PeerActionsInterface peerInterface = (PeerActionsInterface) UnicastRemoteObject.exportObject(this, 0);

            int id = this.getPeerId();
            String accessPoint = "ap"+id;

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, peerInterface);
            System.out.println("\n[RMI] Node " + id + " ready. AccessPoint: " + accessPoint);

        } catch (Exception e) { System.err.println("Error initializing RMI: " + e.toString()); }
    }

}
