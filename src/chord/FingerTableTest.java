package chord;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class FingerTableTest {

    public static void main(String[] args) {
        // create finger table
        int m = 5;
        FingerTable table = new FingerTable(m);
        table.addNodePair(0, new NodePair<Integer,InetSocketAddress>(4, null));
        table.addNodePair(1, new NodePair<Integer,InetSocketAddress>(4, null));
        table.addNodePair(2, new NodePair<Integer,InetSocketAddress>(9, null));
        table.addNodePair(3, new NodePair<Integer,InetSocketAddress>(9, null));
        table.addNodePair(4, new NodePair<Integer,InetSocketAddress>(18, null));

        // lookup
        Integer result = table.lookup(1, 8);
        System.out.println(result);

        System.out.println("___________________");

        table = new FingerTable(m);
        table.addNodePair(0, new NodePair<Integer,InetSocketAddress>(1, null));
        table.addNodePair(1, new NodePair<Integer,InetSocketAddress>(2, null));
        table.addNodePair(2, new NodePair<Integer,InetSocketAddress>(3, null));
        table.addNodePair(3, new NodePair<Integer,InetSocketAddress>(4, null));
        table.addNodePair(4, new NodePair<Integer,InetSocketAddress>(14, null));

        // lookup
        result = table.lookup(28, 12);
        System.out.println("RESULT: " + result);
    }
    
}