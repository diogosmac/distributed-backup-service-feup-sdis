import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Peer implements PeerActionsInterface {

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
    public void backup(String filePath, int replicationDegree) throws RemoteException {
        System.out.println("[WIP] Backup");
        System.out.println("File: " + filePath);
        System.out.println("RD: " + replicationDegree + "\n");
        SavedFile sf = new SavedFile(filePath, replicationDegree); // Stores file bytes and splits it into chunks

        ArrayList<Chunk> fileChunks = sf.getChunks();
        this.chunkOccurrences.addFile(sf.getId());
        for (int currentChunk = 0; currentChunk < fileChunks.size(); currentChunk++) {

            // <Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
            String header = this.protocolVersion + " PUTCHUNK " + this.peerID + " " + sf.getId() +
                    " " + currentChunk + " " + replicationDegree + " " + MyUtils.CRLF + MyUtils.CRLF;

            this.chunkOccurrences.addChunkSlot(sf.getId());

            byte[] headerBytes = MyUtils.convertToByteArray(header);
            byte[] chunkBytes = fileChunks.get(currentChunk).getData();
            byte[] message = MyUtils.concatByteArrays(headerBytes, chunkBytes);

            this.scheduler.execute(new MessageSender(message, this.multicastDataBackupChannel));

        }

    }

    @Override
    public void restore(String filePath) throws RemoteException {
        System.out.println("[WIP] Restore");
    }

    @Override
    public void delete(String filePath) throws RemoteException {
        System.out.println("[WIP] Delete");
    }

    @Override
    public void reclaim(int amountOfSpace) throws RemoteException {
        System.out.println("[WIP] Reclaim");
    }

    @Override
    public void state() throws RemoteException {
        System.out.println("[WIP] Reclaim");
    }

    public Channel getMulticastControlChannel() {
        return multicastControlChannel;
    }

    public Channel getMulticastDataBackupChannel() {
        return multicastDataBackupChannel;
    }

    public Channel getMulticastDataRestoreChannel() {
        return multicastDataRestoreChannel;
    }

    public void storeChunk(Chunk chunk) {
        this.chunkStorage.addChunk(chunk);
    }

    public void saveChunkOccurrence(String fileId, int chunkNumber) {
        this.chunkOccurrences.incChunkOcc(fileId, chunkNumber);
    }

    public int getPeerID() {
        return this.peerID;
    }

    public String getProtocolVersion() {
        return this.protocolVersion;
    }

}
