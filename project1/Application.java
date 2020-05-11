import peer.PeerActionsInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Application {

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 4) {
            System.out.println("Usage: java Application <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        String peerAccessPoint = args[0];
        String subProtocol = args[1];

        try {
                Registry registry = LocateRegistry.getRegistry(null);
                PeerActionsInterface peer = (PeerActionsInterface) registry.lookup(peerAccessPoint);

                String filePath;
                int replicationDegree;
                int amountOfSpace;

                switch(subProtocol) {
                    case "BACKUP":
                        if (args.length != 4) {
                            System.out.println("BACKUP Usage for this access point:");
                            System.out.println("\tjava Application " + peerAccessPoint + " " +
                                    "BACKUP <file_path> <replication_degree>");
                            return;
                        }
                        filePath = args[2];
                        replicationDegree = Integer.parseInt(args[3]);
                        peer.backup(filePath, replicationDegree);
                        break;

                    case "RESTORE":
                        if (args.length != 3) {
                            System.out.println("RESTORE Usage for this access point:");
                            System.out.println("\tjava Application " + peerAccessPoint + " " +
                                    "RESTORE <file_path>");
                            return;
                        }
                        filePath = args[2];
                        peer.restore(filePath);
                        break;

                    case "DELETE":
                        if (args.length != 3) {
                            System.out.println("DELETE Usage for this access point:");
                            System.out.println("\tjava Application " + peerAccessPoint + " " +
                                    "DELETE <file_path>");
                            return;
                        }
                        filePath = args[2];
                        peer.delete(filePath);
                        break;

                    case "RECLAIM":
                        if (args.length != 3) {
                            System.out.println("DELETE Usage for this access point:");
                            System.out.println("\tjava Application " + peerAccessPoint + " " +
                                    "RECLAIM <amount_of_space>");
                            return;
                        }
                        amountOfSpace = Integer.parseInt(args[2]);
                        peer.reclaim(amountOfSpace);
                        break;

                    case "STATE":
                        if (args.length != 2) {
                            System.out.println("DELETE Usage for this access point:");
                            System.out.println("\tjava Application " + peerAccessPoint + " " +
                                    "STATE");
                            return;
                        }
                        String response = peer.state();
                        System.out.println(response);
                        break;

                    default:
                        System.out.println("Available Sub-Protocols: " +
                                "BACKUP | RESTORE | DELETE | RECLAIM | STATE");
                        break;
                }


        } catch (Exception e) {
            System.err.println("Client exception : " + e.toString());
            e.printStackTrace();
        }

    }
}
