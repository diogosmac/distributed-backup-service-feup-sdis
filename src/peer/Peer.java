package peer;

import chord.ChordNode;
import storage.*;
import utils.MyUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Peer implements PeerActionsInterface {

    private final int peerId;
    private final ChordNode node;

    private final ChunkStorage chunkStorage;
    private final Occurrences fileOccurrences;

    // key    :     fileId:chunkNumber
    // value  :     time of read
    private final ConcurrentHashMap<String, Long> receivedChunks;

    // key    :     fileId:chunkNumber
    // value  :     time of read
    private final ConcurrentHashMap<String, Long> putChunkMessagesReclaim;

    private final FileRestorer fileRestorer;

    // elements are the IDs of the files to be deleted
    private final List<String> scheduledDeletes;

    public Peer(ChordNode node) throws IOException {

        this.peerId = node.getID();
        this.node = node;
        this.node.setPeer(this);

        this.chunkStorage = new ChunkStorage(this);
        this.fileOccurrences = new Occurrences(this);
        this.receivedChunks = new ConcurrentHashMap<>();
        this.putChunkMessagesReclaim = new ConcurrentHashMap<>();
        this.fileRestorer = new FileRestorer(MyUtils.getRestorePath(this));

        this.scheduledDeletes = new ArrayList<>();
        this.loadScheduledDeletes(this.scheduledDeletes);

    }

    public int getPeerId() { return this.peerId; }

    public ChordNode getNode() { return this.node; }

    @Override
    public void backup(String filePath, int replicationDegree) throws Exception {

        this.node.initiateBackup(filePath, replicationDegree);

//
//        this.operations.add(Operation.BACKUP);
//
//        System.out.print("\nBackup > File: " + filePath + ", RD: " + replicationDegree + "\n");
//        SavedFile sf = new SavedFile(filePath, replicationDegree); // Stores file bytes and splits it into chunks
//
//        String fileId = sf.getId();
//
////        if (getProtocolVersion().equals("2.0"))
////            this.scheduledDeletes.remove(fileId);
//
//        ArrayList<Chunk> fileChunks = sf.getChunks();
//        String fileName = MyUtils.fileNameFromPath(filePath);
//        this.chunkOccurrences.addFile(fileId, fileName, replicationDegree);
//        for (int currentChunk = 0; currentChunk < fileChunks.size(); currentChunk++) {
//
//            String header = buildPutchunkHeader(fileId, currentChunk, replicationDegree);
//
//            this.chunkOccurrences.addChunkSlot(fileId, currentChunk);
//
//            byte[] headerBytes = MyUtils.convertStringToByteArray(header);
//            byte[] chunkBytes = fileChunks.get(currentChunk).getData();
//            byte[] putChunkMessage = MyUtils.concatByteArrays(headerBytes, chunkBytes);
//
//            for (int i = 0; i < MyUtils.MAX_TRIES; i++) {
//
//                this.scheduler.execute(new MessageSender(putChunkMessage, this.multicastDataBackupChannel));
//
//                // Starts by waiting one second, and doubles the waiting time with each iteration
//                Thread.sleep((long) (1000 * Math.pow(2, i)));
//
//                if (this.chunkOccurrences.getChunkOccurrences(fileId, currentChunk) >= sf.getReplicationDegree())
//                    break;
//
//                System.out.println("\t\tDesired number of occurrences: " + sf.getReplicationDegree() + ", " +
//                        "Current number of occurrences: " + this.chunkOccurrences.getChunkOccurrences(fileId,
//                                                                                                      currentChunk));
//
//                if (i == MyUtils.MAX_TRIES - 1)
//                    System.out.println("BACKUP " + filePath + " : " +
//                            "Couldn't reach desired replication degree for chunk #" + currentChunk);
//
//            }
//
//        }
//
//        System.out.println(String.join(" ", "BACKUP", filePath, Integer.toString(replicationDegree),
//                                        ":", "Operation completed"));
//
//        this.operations.remove(Operation.BACKUP);
//
    }

    @Override
    public void restore(String filePath) throws Exception {
//        this.operations.add(Operation.RESTORE);
//        System.out.println("\nRestore > File: " + filePath);
//
//        String fileName = MyUtils.fileNameFromPath(filePath);
//        String fileId = MyUtils.encryptFileID(filePath);
//
//        this.fileRestorer.addFile(fileId);
//        int currentChunk = 0;
//        do {
//            this.fileRestorer.addSlot(fileId);
//
//            String restoreMessageStr = buildGetchunkHeader(fileId, currentChunk);
//
//            for (int i = 0; i < MyUtils.MAX_TRIES; i++) {
//                this.executeThread(new MessageSender(
//                        MyUtils.convertStringToByteArray(restoreMessageStr),
//                        this.multicastControlChannel));
//
//                // Starts by waiting one second, and doubles the waiting time with each iteration
//                Thread.sleep((long) (1000 * Math.pow(2, i)));
//                if (fileRestorer.getChunkData(fileId, currentChunk) != null) {
//                    break;
//                }
//
//                if (i == MyUtils.MAX_TRIES - 1) {
//                    System.out.println("RESTORE " + filePath + " : Operation failed");
//                    System.out.println("Couldn't get data from peers for chunk #" + currentChunk);
//                    this.operations.remove(Operation.RESTORE);
//                    return;
//                }
//            }
//
//        } while (this.fileRestorer.getChunkData(fileId, currentChunk++).length == MyUtils.CHUNK_SIZE);
//
//        if (this.fileRestorer.restoreFile(fileId, fileName)) {
//            System.out.println("\tFile " + fileName + " successfully restored!");
//        } else {
//            System.out.println("\tFailed to restore file " + fileName);
//        }
//
//        System.out.println(String.join(" ", "RESTORE", filePath, ":", "Operation completed"));
//
//        this.operations.remove(Operation.RESTORE);
    }

    @Override
    public void delete(String filePath) throws Exception {

        this.node.initiateDelete(filePath);

//        this.operations.add(Operation.DELETE);
//        System.out.println("\nDelete > File: " + filePath);
//        SavedFile sf = new SavedFile(filePath);
//
//        String fileId = sf.getId();
//
//        if (this.chunkOccurrences.hasFile(fileId))
//            this.scheduleDelete(fileId);
//
//        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
//        String header = buildDeleteHeader(fileId);
//        byte[] deleteMessage = MyUtils.convertStringToByteArray(header);
//        this.scheduler.execute(new MessageSender(deleteMessage, this.multicastControlChannel));
//
//        System.out.println(String.join(" ", "DELETE", filePath, ":", "Operation completed"));
//        System.out.flush();
//        this.operations.remove(Operation.DELETE);
    }

    @Override
    public void reclaim(int amountOfSpace) throws Exception {
//        this.operations.add(Operation.RECLAIM);
//        System.out.println("\nReclaim > Amount of Space: " + amountOfSpace);
//        int freed = this.chunkStorage.reclaimSpace(amountOfSpace);
//        System.out.println("\tFreed " + freed * 0.001 + " KB of disk space");
//        this.operations.remove(Operation.RECLAIM);
    }

    @Override
    public String state() throws Exception {
        String header = "\nSTATE: Node " + this.node.getID() + "\n\n";
        String peerMemoryInfo = this.chunkStorage.getMemoryInfo();
        String backupFilesInfo = this.fileOccurrences.getOccurrencesInfo();
        String storedChunksInfo = this.chunkStorage.getChunkInfo();
        return header + peerMemoryInfo + backupFilesInfo + storedChunksInfo;
    }

//    public String buildHelloWorldHeader() {
//        // <Version> HELLOWORLD <SenderId> <CRLF><CRLF>
//        return String.join(" ", this.protocolVersion, "HELLOWORLD", Integer.toString(this.peerId),
//                MyUtils.CRLF + MyUtils.CRLF);
//    }
//
//    public String buildPutchunkHeader(String fileId, int currentChunk, int replicationDegree) {
//        // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
//        return String.join(" ", this.protocolVersion, "PUTCHUNK", Integer.toString(this.peerId),
//                fileId, Integer.toString(currentChunk), Integer.toString(replicationDegree), MyUtils.CRLF + MyUtils.CRLF);
//    }
//
//    public String buildGetchunkHeader(String fileId, int currentChunk) {
//        //  <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
//        return String.join(" ", this.protocolVersion, "GETCHUNK", Integer.toString(this.peerId),
//                fileId, Integer.toString(currentChunk), MyUtils.CRLF+ MyUtils.CRLF);
//    }
//
//    public String buildDeleteHeader(String fileId) {
//        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
//        return String.join(" ", this.protocolVersion, "DELETE", Integer.toString(this.peerId),
//                fileId, MyUtils.CRLF + MyUtils.CRLF);
//    }

    public ChunkStorage getChunkStorage() { return this.chunkStorage; }

    public Occurrences getFileOccurrences() { return this.fileOccurrences; }

    public FileRestorer getFileRestorer() { return this.fileRestorer; }

    public void saveReceivedChunkTime(String fileId, int chunkNumber) {
        String key = String.join(":", fileId, Integer.toString(chunkNumber));
        this.receivedChunks.put(key, System.currentTimeMillis());
    }

    public boolean notRecentlyReceived(String fileId, int chunkNumber) {
        String key = String.join(":", fileId, Integer.toString(chunkNumber));
        if (this.receivedChunks.containsKey(key)) {
            long value = this.receivedChunks.get(key);
            return (System.currentTimeMillis() - value) >= 400;
        }

        return true;
    }

    public void logPutChunkMessage(String fileId, int chunkNumber) {
        String key = String.join(":", fileId, Integer.toString(chunkNumber));
        this.putChunkMessagesReclaim.put(key, System.currentTimeMillis());
    }

    public boolean noRecentPutChunkMessage(String fileId, int chunkNumber) {
        String key = String.join(":", fileId, Integer.toString(chunkNumber));
        if (this.putChunkMessagesReclaim.containsKey(key)) {
            long value = this.putChunkMessagesReclaim.get(key);
            return (System.currentTimeMillis() - value) >= 400;
        }
        return true;
    }

    public List<String> getScheduledDeletes() { return this.scheduledDeletes; }

    public void scheduleDelete(String fileId) {
        if (!this.scheduledDeletes.contains(fileId)) {
            this.scheduledDeletes.add(fileId);
            this.saveScheduledDeletes();
        }
    }

    public void concludeDelete(String fileId) {
        if (this.scheduledDeletes.contains(fileId)) {
            this.scheduledDeletes.remove(fileId);
            this.saveScheduledDeletes();
        }
    }

    private void saveScheduledDeletes() {
        String dirPath = MyUtils.getPeerPath(this);
        File file = new File(String.join("/", dirPath, MyUtils.DEFAULT_DELETE_BACKLOG_PATH));
        if (file.getParentFile().mkdirs())
            System.out.println("\tCreated " + dirPath + " directory.");

        try {
            PrintWriter writer = new PrintWriter(file);
            StringBuilder output = new StringBuilder();
            for (String fileId : this.scheduledDeletes) {
                output.append(fileId).append("\n");
            }
            writer.print(output.toString());
            writer.close();
        } catch (Exception e) {
            System.out.println("Exception while writing scheduled deletes to file: " + e.toString()); }
    }

    private void loadScheduledDeletes(List<String> scheduledDeletes) {
        String dirPath = MyUtils.getPeerPath(this);
        File file = new File(String.join("/", dirPath, MyUtils.DEFAULT_DELETE_BACKLOG_PATH));
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String fileId;
                while ((fileId = br.readLine()) != null) {
                    scheduledDeletes.add(fileId);
                }
                br.close();
            } catch (Exception e) {
                System.out.println("Exception while reading from file: " + e.toString());
            }
        }
    }


}
