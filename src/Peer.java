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

class Peer implements PeerActionsInterface {

    private String protocol_version;
    private int peerID;

    private Channel multicastControlChannel;
    private Channel multicastDataBackupChannel;
    private Channel multicastDataRestoreChannel;

    private ScheduledThreadPoolExecutor scheduler;

    private int port;
    private ServerSocket serverSocket;

    private OccurrencesStorage chunk_occurrences;
    private ChunkStorage chunk_storage;

    public Peer(String protocol_version, int peerID,
                String MCAddress, String MCPort,
                String MDBAddress, String MDBPort,
                String MDRAddress, String MDRPort) throws IOException {

        this.protocol_version = protocol_version;
        this.peerID = peerID;

        this.multicastControlChannel = new Channel(MCAddress, Integer.parseInt(MCPort), this);
        this.multicastDataBackupChannel = new Channel(MDBAddress, Integer.parseInt(MDBPort), this);
        this.multicastDataRestoreChannel = new Channel(MDRAddress, Integer.parseInt(MDRPort), this);

        this.scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(300);

        this.port = MyUtils.BASE_PORT + this.peerID;
        this.serverSocket = new ServerSocket(this.port);

        this.chunk_occurrences = new OccurrencesStorage();
        this.chunk_storage = new ChunkStorage();
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
            Peer obj = new Peer(version, id, args[3], args[4], args[5], args[6], args[7], args[8]);
            PeerActionsInterface peerInterface = (PeerActionsInterface) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(accessPoint, peerInterface);
            System.out.println("Peer "+id+" ready. v" + version + " accessPoint: " +  accessPoint);

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
        }

    }

    @Override
    public void backup(String filePath, int replicationDegree) throws RemoteException {
        System.out.println("[WIP] Backup");
        System.out.println("File: " + filePath);
        System.out.println("RD: " + replicationDegree);
        SavedFile sf = new SavedFile(filePath, replicationDegree); //Stores file bytes and splits it into chunks

        ArrayList<Chunk> file_chunks = sf.getChunks();
        this.chunk_occurrences.addFile(sf.getId());
        for (int current_chunk = 0; current_chunk < file_chunks.size(); current_chunk++) {

            //<Version> PUTCHUNK <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
            String header = this.protocol_version + " PUTCHUNK " + this.peerID + " " + sf.getId() +
                    " " + current_chunk + " " + replicationDegree + " " + MyUtils.CRLF + MyUtils.CRLF;

            this.chunk_occurrences.addChunkSlot(sf.getId());

            byte [] header_bytes = MyUtils.convertToByteArray(header);
            byte [] chunk_bytes = file_chunks.get(current_chunk).getData();
            byte [] send_message = MyUtils.concatByteArrays(header_bytes, chunk_bytes);

            this.scheduler.execute(new MessageSender(send_message, this.multicastDataBackupChannel));
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
        this.chunk_storage.addChunk(chunk);
    }

    public void saveChunkOccurrence(String file_id, int chunk_number) {
        this.chunk_occurrences.incChunkOcc(file_id, chunk_number);
    }

    public int getPeerID() {
        return this.peerID;
    }

    public String getProtocolVersion() {
        return this.protocol_version;
    }
}