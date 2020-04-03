import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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

    }

    public void executeWithScheduler(MessageReceivingThread thread) {
        scheduler.execute(thread);
    }

    public void scheduleWithScheduler(MessageReceivingThread thread, int interval, TimeUnit timeUnit) {
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
}