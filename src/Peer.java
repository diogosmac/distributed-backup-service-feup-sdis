import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Peer implements PeerActionsInterface {

    public enum Operation {
        BACKUP,
        RESTORE,
        DELETE,
        RECLAIM,
        STATE
    }

    private final String protocolVersion;
    private final int peerId;

    private final Channel multicastControlChannel;
    private final Channel multicastDataBackupChannel;
    private final Channel multicastDataRestoreChannel;

    private final ScheduledThreadPoolExecutor scheduler;

//    private int port;
//    private ServerSocket serverSocket;

    private final OccurrencesStorage chunkOccurrences;
    private final ChunkStorage chunkStorage;

    // key = fileId:chunkNumber
    // value = time of read
    private final ConcurrentHashMap<String, Long> receivedChunks;

    // key = fileId:chunkNumber
    // value = time of read
    private final ConcurrentHashMap<String, Long> putChunkMessagesReclaim;

    private final FileRestorer fileRestorer;

    private final List<Operation> operations;

    public Peer(String protocolVersion, int peerId,
                String MCAddress, String MCPort,
                String MDBAddress, String MDBPort,
                String MDRAddress, String MDRPort) throws IOException {

        this.protocolVersion = protocolVersion;
        this.peerId = peerId;

        this.multicastControlChannel = new Channel(MCAddress, Integer.parseInt(MCPort), this,
                "MC Control Channel is open!");
        this.multicastDataBackupChannel = new Channel(MDBAddress, Integer.parseInt(MDBPort), this,
                "MC Data Backup Channel is open!");
        this.multicastDataRestoreChannel = new Channel(MDRAddress, Integer.parseInt(MDRPort), this,
                "MC Data Restore Channel is open!");

        this.scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(300);

//        this.port = MyUtils.BASE_PORT + this.peerId;
//        this.serverSocket = new ServerSocket(this.port);

        this.chunkOccurrences = new OccurrencesStorage(this);
        this.chunkStorage = new ChunkStorage(this);
        this.receivedChunks = new ConcurrentHashMap<>();
        this.putChunkMessagesReclaim = new ConcurrentHashMap<>();
        this.fileRestorer = new FileRestorer(MyUtils.getRestorePath(this));

        this.operations = new ArrayList<>();
    }

    public void executeThread(Runnable thread) {
        scheduler.execute(thread);
    }

    public void scheduleThread(Runnable thread, int interval, TimeUnit timeUnit) {
        scheduler.schedule(thread, interval, timeUnit);
    }

    public int getPeerId() { return this.peerId; }

    public String getProtocolVersion() { return this.protocolVersion; }

    public boolean isDoingOperation(Operation op) { return this.operations.contains(op); }

    public static void main(String[] args) {

        if (args.length != 9) {
            System.out.println(
                    "Usage: java Peer <protocol_version> <peer_id> <service access point> " +
                            "<mc_address> <mc_port> " +
                            "<mdb_address> <mdb_port> " +
                            "<mdr_address> <mdr_port>");
            return;
        }

        String version = args[0];
        int id = Integer.parseInt(args[1]);
        String accessPoint = args[2];

        try {
            Peer peer = new Peer(version, id, args[3], args[4], args[5], args[6], args[7], args[8]);
            PeerActionsInterface peerInterface = (PeerActionsInterface) UnicastRemoteObject.exportObject(peer, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, peerInterface);
            peer.executeThread(peer.multicastControlChannel);
            peer.executeThread(peer.multicastDataBackupChannel);
            peer.executeThread(peer.multicastDataRestoreChannel);
            System.out.println("\nPeer " + id + " ready. v" + version + " accessPoint: " + accessPoint);

        } catch (Exception e) { System.err.println("Peer exception : " + e.toString()); }
    }

    @Override
    public void backup(String filePath, int replicationDegree) throws Exception {

        this.operations.add(Operation.BACKUP);

        System.out.print("\nBackup > File: " + filePath + ", RD: " + replicationDegree + "\n");
        SavedFile sf = new SavedFile(filePath, replicationDegree); // Stores file bytes and splits it into chunks

        String fileId = sf.getId();
        ArrayList<Chunk> fileChunks = sf.getChunks();
        String fileName = MyUtils.fileNameFromPath(filePath);
        this.chunkOccurrences.addFile(fileId, fileName, replicationDegree);
        for (int currentChunk = 0; currentChunk < fileChunks.size(); currentChunk++) {

            String header = buildPutchunkHeader(fileId, currentChunk, replicationDegree);

            this.chunkOccurrences.addChunkSlot(fileId, currentChunk);

            byte[] headerBytes = MyUtils.convertStringToByteArray(header);
            byte[] chunkBytes = fileChunks.get(currentChunk).getData();
            byte[] putChunkMessage = MyUtils.concatByteArrays(headerBytes, chunkBytes);

            for (int i = 0; i < MyUtils.MAX_TRIES; i++) {

                this.scheduler.execute(new MessageSender(putChunkMessage, this.multicastDataBackupChannel));

                // Starts by waiting one second, and doubles the waiting time with each iteration
                Thread.sleep((long) (1000 * Math.pow(2, i)));

                if (this.chunkOccurrences.getChunkOccurrences(fileId, currentChunk) >= sf.getReplicationDegree())
                    break;

                System.out.println("\t\tDesired number of occurrences: " + sf.getReplicationDegree() + ", " +
                        "Current number of occurrences: " + this.chunkOccurrences.getChunkOccurrences(fileId,
                                                                                                      currentChunk));

                if (i == MyUtils.MAX_TRIES - 1)
                    System.out.println("BACKUP " + filePath + " : " +
                            "Couldn't reach desired replication degree for chunk #" + currentChunk);

            }

        }

        System.out.println(String.join(" ", "BACKUP", filePath, Integer.toString(replicationDegree),
                                        ":", "Operation completed"));

        this.operations.remove(Operation.BACKUP);

    }

    @Override
    public void restore(String filePath) throws Exception {
        this.operations.add(Operation.RESTORE);
        System.out.println("\nRestore > File: " + filePath);

        String fileName = MyUtils.fileNameFromPath(filePath);
        String fileId = MyUtils.encryptFileID(filePath);

        this.fileRestorer.addFile(fileId);
        int currentChunk = 0;
        do {
            this.fileRestorer.addSlot(fileId);

            String restoreMessageStr = buildGetchunkHeader(fileId, currentChunk);

            for (int i = 0; i < MyUtils.MAX_TRIES; i++) {
                this.executeThread(new MessageSender(
                        MyUtils.convertStringToByteArray(restoreMessageStr),
                        this.multicastControlChannel));

                // Starts by waiting one second, and doubles the waiting time with each iteration
                Thread.sleep((long) (1000 * Math.pow(2, i)));
                if (fileRestorer.getChunkData(fileId, currentChunk) != null) {
                    break;
                }

                if (i == MyUtils.MAX_TRIES - 1) {
                    System.out.println("RESTORE " + filePath + " : Operation failed");
                    System.out.println("Couldn't get data from peers for chunk #" + currentChunk);
                    this.operations.remove(Operation.RESTORE);
                    return;
                }

            }

        } while (this.fileRestorer.getChunkData(fileId, currentChunk++).length == MyUtils.CHUNK_SIZE);

        if (this.fileRestorer.restoreFile(fileId, fileName)) {
            System.out.println("\tFile " + fileName + " successfully restored!");
        } else {
            System.out.println("\tFailed to restore file " + fileName);
        }

        System.out.println(String.join(" ", "RESTORE", filePath, ":", "Operation completed"));

        this.operations.remove(Operation.RESTORE);
    }

    @Override
    public void delete(String filePath) throws Exception {
        this.operations.add(Operation.DELETE);
        System.out.println("\nDelete > File: " + filePath);
        SavedFile sf = new SavedFile(filePath);

        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
        String header = buildDeleteHeader(sf.getId());
        byte[] deleteMessage = MyUtils.convertStringToByteArray(header);
        this.scheduler.execute(new MessageSender(deleteMessage, this.multicastControlChannel));

        System.out.println(String.join(" ", "DELETE", filePath, ":", "Operation completed"));
        System.out.flush();
        this.operations.remove(Operation.DELETE);
    }

    @Override
    public void reclaim(int amountOfSpace) throws Exception {
        this.operations.add(Operation.RECLAIM);
        System.out.println("\nReclaim > Amount of Space: " + amountOfSpace);
        int freed = this.chunkStorage.reclaimSpace(amountOfSpace);
        System.out.println("\tFreed " + freed * 0.001 + " KB of disk space");
        this.operations.remove(Operation.RECLAIM);
    }

    @Override
    public String state() throws Exception {
        this.operations.add(Operation.STATE);
        String header = "\nSTATE: Peer " + this.getPeerId() + "\n\n";
        String peerMemoryInfo = this.chunkStorage.getMemoryInfo();
        String backupFilesInfo = this.chunkOccurrences.getOccurrencesInfo();
        String storedChunksInfo = this.chunkStorage.getChunkInfo();
        this.operations.remove(Operation.STATE);
        return header + peerMemoryInfo + backupFilesInfo + storedChunksInfo;
    }

    public Channel getMulticastControlChannel() { return this.multicastControlChannel; }
    public Channel getMulticastDataBackupChannel() { return this.multicastDataBackupChannel; }
    public Channel getMulticastDataRestoreChannel() { return this.multicastDataRestoreChannel; }

    public String buildPutchunkHeader(String fileId, int currentChunk, int replicationDegree) {
        // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
        return String.join(" ", this.protocolVersion, "PUTCHUNK", Integer.toString(this.peerId),
                fileId, Integer.toString(currentChunk), Integer.toString(replicationDegree), MyUtils.CRLF + MyUtils.CRLF);
    }

    public String buildGetchunkHeader(String fileId, int currentChunk) {
        //  <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        return String.join(" ", this.protocolVersion, "GETCHUNK", Integer.toString(this.peerId),
                fileId, Integer.toString(currentChunk), MyUtils.CRLF+MyUtils.CRLF);
    }

    public String buildDeleteHeader(String fileId) {
        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
        return String.join(" ", this.protocolVersion, "DELETE", Integer.toString(this.peerId),
                fileId, MyUtils.CRLF + MyUtils.CRLF);
    }

    public ChunkStorage getChunkStorage() { return this.chunkStorage; }

    public OccurrencesStorage getChunkOccurrences() { return this.chunkOccurrences; }

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

}
