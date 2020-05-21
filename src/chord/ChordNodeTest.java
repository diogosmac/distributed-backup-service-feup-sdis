package chord;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Chord Node Test
 * 
 * Helper class to run a chord node instance
 */
public class ChordNodeTest {

    public static void main(String[] args) {

        int port;
        ChordNode node = null;

        // First node is joining the network
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
            try {
                node = new ChordNode(port);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        // Other node is joining
        } else if (args.length == 3) {
            try {
                port = Integer.parseInt(args[0]);
                InetSocketAddress thisAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port);
                InetSocketAddress knownAddress = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
                node = new ChordNode(thisAddress, knownAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        // You dumbass
        } else {
            System.out.println("Usage: java ChordNodeTest <node-id> [ <connection-address> <connection-port> ]");
            return;
        }

        System.out.println("Started chord node");
        System.out.println("\tID: " + node.getID());
        System.out.println("\tAddress: " + node.getAddress());

        Timer timer = new Timer(); 
        ChordNodePrinter printer = new ChordNodePrinter(node);
        timer.schedule(printer, 1000, 5000);
    }

}

/**
 * Chord Node Printer
 * 
 * Helper class to print chord node's information
 * about periodically
 */
class ChordNodePrinter extends TimerTask {

    private ChordNode node;

    public ChordNodePrinter(ChordNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        System.out.println("\nFINGER TABLE");
        System.out.println(node.getFingerTable());
        //
        System.out.println("\nSUCCESSOR LIST");
        CopyOnWriteArrayList<NodePair<Integer, InetSocketAddress>> successorList = node.getSuccessorList();
        for (NodePair<Integer, InetSocketAddress> successor : successorList)
            System.out.println(successor);
        //
        System.out.println("\nPREDECESSOR");
        System.out.println(node.getPredecessor());
    }
}
