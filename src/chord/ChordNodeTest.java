package chord;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ChordNodeTest {

    public static void main(String[] args) {

        int m = 5;

        int id;
        InetSocketAddress connectionPeer;
        ChordNode node;

        if (args.length == 1) {
            id = Integer.parseInt(args[0]);
            try {
                node = new ChordNode(id, m);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
        }
        else if (args.length == 3) {
            id = Integer.parseInt(args[0]);
            connectionPeer = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

            node = new ChordNode(id, m, connectionPeer);
        }
        else {
            System.out.println("Usage: java ChordNodeTest <node-id> [ <connection-address> <connection-port> ]");
            return;
        }

        switch (id) {

            case 1:
                node.setFingerTableEntry(0, new NodePair<Integer, InetSocketAddress>(4, null));
                node.setFingerTableEntry(1, new NodePair<Integer, InetSocketAddress>(4, null));
                node.setFingerTableEntry(2, new NodePair<Integer, InetSocketAddress>(9, null));
                node.setFingerTableEntry(3, new NodePair<Integer, InetSocketAddress>(9, null));
                node.setFingerTableEntry(4, new NodePair<Integer, InetSocketAddress>(18, null));
                break;

            case 18:
                node.setFingerTableEntry(0, new NodePair<Integer, InetSocketAddress>(20, null));
                node.setFingerTableEntry(1, new NodePair<Integer, InetSocketAddress>(20, null));
                node.setFingerTableEntry(2, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(3, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(4, new NodePair<Integer, InetSocketAddress>(4, null));
                break;

            case 20:
                node.setFingerTableEntry(0, new NodePair<Integer, InetSocketAddress>(21, null));
                node.setFingerTableEntry(1, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(2, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(3, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(4, new NodePair<Integer, InetSocketAddress>(4, null));
                break;

            case 21:
                node.setFingerTableEntry(0, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(1, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(2, new NodePair<Integer, InetSocketAddress>(28, null));
                node.setFingerTableEntry(3, new NodePair<Integer, InetSocketAddress>(1, null));
                node.setFingerTableEntry(4, new NodePair<Integer, InetSocketAddress>(9, null));
                break;

            default: return;

        }

        System.out.println("h3h3h3");

    }

}
