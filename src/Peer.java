import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Peer implements PeerActionsInterface {

    public enum State {
        IDLE,
        BACKUP,
        RESTORE,
        DELETE,
        RECLAIM,
        STATE
    }

    private String protocolVersion;
    private int peerID;

    private Channel multicastControlChannel;
    private Channel multicastDataBackupChannel;
    private Channel multicastDataRestoreChannel;

    private ScheduledThreadPoolExecutor scheduler;

    private int port;
    private ServerSocket serverSocket;

    private OccurrencesStorage chunkOccurrences;
    private ChunkStorage chunkStorage;

    // key = fileId:chunkNumber
    // value = time of read
    private ConcurrentHashMap<String, Long> receivedChunks;

    private FileRestorer fileRestorer;

    private State state;

    public Peer(String protocolVersion, int peerID,
                String MCAddress, String MCPort,
                String MDBAddress, String MDBPort,
                String MDRAddress, String MDRPort) throws IOException {

        this.protocolVersion = protocolVersion;
        this.peerID = peerID;

        this.multicastControlChannel = new Channel(MCAddress, Integer.parseInt(MCPort), this,
                "MC Control Channel is open!");
        this.multicastDataBackupChannel = new Channel(MDBAddress, Integer.parseInt(MDBPort), this,
                "MC Data Backup Channel is open!");
        this.multicastDataRestoreChannel = new Channel(MDRAddress, Integer.parseInt(MDRPort), this,
                "MC Data Restore Channel is open!");

        this.scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(300);

        this.port = MyUtils.BASE_PORT + this.peerID;
        this.serverSocket = new ServerSocket(this.port);

        this.chunkOccurrences = new OccurrencesStorage();
        this.chunkStorage = new ChunkStorage();
        this.receivedChunks = new ConcurrentHashMap<>();

        this.state = State.IDLE;
    }

    public void executeThread(Runnable thread) {
        scheduler.execute(thread);
    }

    public void scheduleThread(Runnable thread, int interval, TimeUnit timeUnit) {
        scheduler.schedule(thread, interval, timeUnit);
    }

    public static void main(String[] args) throws IOException {

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
            System.out.println("\nPeer " + id + " ready. v" + version + " accessPoint: " + accessPoint + "\n");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }
    }

    @Override
    public void backup(String filePath, int replicationDegree) throws Exception {

        this.state = State.BACKUP;

        System.out.print("\nBackup > File: " + filePath + ", RD: " + replicationDegree + "\n");
        SavedFile sf = new SavedFile(filePath, replicationDegree); // Stores file bytes and splits it into chunks

        ArrayList<Chunk> fileChunks = sf.getChunks();
        this.chunkOccurrences.addFile(sf.getId());
        for (int currentChunk = 0; currentChunk < fileChunks.size(); currentChunk++) {

            // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
            String header = String.join(" ",
                    this.protocolVersion, "PUTCHUNK", Integer.toString(this.peerID), sf.getId(),
                    Integer.toString(currentChunk), Integer.toString(replicationDegree), MyUtils.CRLF + MyUtils.CRLF);

            this.chunkOccurrences.addChunkSlot(sf.getId());

            byte[] headerBytes = MyUtils.convertToByteArray(header);
            byte[] chunkBytes = fileChunks.get(currentChunk).getData();
            byte[] putChunkMessage = MyUtils.concatByteArrays(headerBytes, chunkBytes);

            for (int i = 0; i < MyUtils.CHUNK_SEND_MAX_TRIES; i++) {

                this.scheduler.execute(new MessageSender(putChunkMessage, this.multicastDataBackupChannel));

                // Starts by waiting one second, and doubles the waiting time with each iteration
                Thread.sleep((long) (1000 * Math.pow(2, i)));

                if (this.chunkOccurrences.getChunkOccurrences(sf.getId(), currentChunk) >= sf.getReplicationDegree())
                    break;

                System.out.println("Desired number of occurrences: " + sf.getReplicationDegree() + ", " +
                        "Current number of occurrences: " + this.chunkOccurrences.getChunkOccurrences(sf.getId(),
                                                                                                      currentChunk));

                if (i == 4)
                    System.out.println("BACKUP " + filePath + " : " +
                            "Couldn't reach desired replication degree for chunk #" + currentChunk);

            }

        }

        System.out.println("BACKUP " + filePath + " : Operation completed");

        this.state = State.IDLE;

    }

    @Override
    public void restore(String filePath) throws Exception {
        this.state = State.RESTORE;
        System.out.println("[WIP] Restore");
        System.out.println("\nRestore > File: " + filePath);

        SavedFile sf =  new SavedFile(filePath);

//        TODO: Get filename from file path
        String fileName = filePath;
        this.fileRestorer = new FileRestorer(MyUtils.getRestorePath(this) + fileName);
        ArrayList<Chunk> chunks = sf.getChunks();

        for (int currentChunk = 0; currentChunk < chunks.size(); currentChunk++) {
            this.fileRestorer.addSlot();

            while (this.fileRestorer.getChunk(currentChunk) == null) {

                //  <Version> GETCHUNK <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
                String restoreMessageStr = String.join(" ",
                        this.protocolVersion, "GETCHUNK", Integer.toString(this.peerID),
                        sf.getId(), Integer.toString(currentChunk), MyUtils.CRLF+MyUtils.CRLF);


                this.executeThread(new MessageSender(restoreMessageStr.getBytes(), this.multicastControlChannel));
            }
        }

        Thread.sleep(500); // Probably isn't necessary

        if(this.fileRestorer.restoreFile()) {
            System.out.println("File :" + filePath + " successfully restored");
        } else {
            System.out.println("Failed to restore file: " + filePath);
        }

        this.state = State.IDLE;
    }

    @Override
    public void delete(String filePath) throws Exception {
        this.state = State.DELETE;
        System.out.println("\nDelete > File: " + filePath);
        SavedFile sf = new SavedFile(filePath);

        // <Version> DELETE <SenderId> <FileId> <CRLF><CRLF>
        String header = String.join(" ",
                this.protocolVersion, "DELETE", Integer.toString(this.peerID),
                sf.getId(), MyUtils.CRLF + MyUtils.CRLF);
        byte[] deleteMessage = MyUtils.convertToByteArray(header);
        this.scheduler.execute(new MessageSender(deleteMessage, this.multicastControlChannel));

        this.chunkOccurrences.deleteOccurrences(sf.getId());
        System.out.flush();
        System.out.println("DELETE " + filePath + " : Operation completed");
        System.out.flush();
        this.state = State.IDLE;
    }

    @Override
    public void reclaim(int amountOfSpace) throws Exception {
        this.state = State.RECLAIM;
        System.out.println("[WIP] Reclaim");
        this.state = State.IDLE;
    }

    @Override
    public void state() throws Exception {
        this.state = State.STATE;
        System.out.println("[WIP] State");
        this.state = State.IDLE;
    }


    public Channel getMulticastControlChannel() {
        return this.multicastControlChannel;
    }

    public Channel getMulticastDataBackupChannel() {
        return this.multicastDataBackupChannel;
    }

    public Channel getMulticastDataRestoreChannel() {
        return this.multicastDataRestoreChannel;
    }

    public void storeChunk(Chunk chunk) {
        this.chunkStorage.addChunk(chunk);
    }

    public void deleteFile(String fileId) {
        this.chunkStorage.deleteFile(fileId);
    }

    public boolean hasChunk(String fileId, int chunkNum) {
        return this.chunkStorage.hasChunk(fileId, chunkNum);
    }

    public Chunk getChunk(String fileId, int chunkNum) {
        return this.chunkStorage.getChunk(fileId, chunkNum);
    }

    public void saveChunkOccurrence(String fileId, int chunkNumber) {
        this.chunkOccurrences.incChunkOcc(fileId, chunkNumber);
    }

    public void deleteOccurrences(String fileId) {
        this.chunkOccurrences.deleteOccurrences(fileId);
    }

    public int getPeerID() {
        return this.peerID;
    }

    public String getProtocolVersion() {
        return this.protocolVersion;
    }

    public State getState() {
        return this.state;
    }

    public void saveRestoredChunk(int chunkNumber, byte[] data) {
        this.fileRestorer.saveData(chunkNumber, data);
    }

    public void saveReceivedChunkTime(String fileId, int chunkNumber) {
        String key = fileId + ":" + chunkNumber;
        this.receivedChunks.put(key, System.currentTimeMillis());
    }

    public boolean recentlyReceived(String fileId, int chunkNumber) {
        String key = fileId + ":" + chunkNumber;
        if (this.receivedChunks.containsKey(key)) {
            long value = this.receivedChunks.get(key);
            return (System.currentTimeMillis() - value) < 400;
        }

        return false;
    }

}
