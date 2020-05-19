package chord.communication;

import javax.net.ssl.SSLSocket;

import chord.ChordNode;
import chord.NodePair;
import chord.Utils;

import java.net.InetSocketAddress;

public class MessageHandler extends Thread {
    
    private final String message;
    private final SSLSocket socket;
    private final ChordChannel ch;
    private final ChordNode node;

    MessageHandler(SSLSocket sk, String message, ChordChannel ch, ChordNode node) {
        this.socket = sk;
        this.message = message;
        this.ch = ch;
        this.node = node;
    }

    private InetSocketAddress getAddress(SSLSocket socket) {
        return (socket == null
                ? this.node.getAddress()
                : (InetSocketAddress) socket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        // Handles a message received by the ChordChannel
        String[] args = message.split(" ");
        switch(args[0]) {
            case "FINDSUCCESSOR": {
                System.out.println("[FINDSUCCESSOR]");
                // Message format: FINDSUCCESSOR <requestedId> <originIP> <originPort>
                int id = Integer.parseInt(args[1]);
                InetSocketAddress fsAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                this.node.findSuccessor(fsAddress, id);
                break;
            }

            case "SUCCESSORFOUND": {
                System.out.println("[SUCCESSORFOUND]");
                synchronized (this.node) {
                    InetSocketAddress sfAddress = getAddress(socket);
                    this.ch.addMessageQueue(new Message(sfAddress, message));
                    this.node.notify();
                }
                break;
            }

            case "JOINING": {
                System.out.println("[JOINING]");
                // Message format: JOINING <newNodeId> <newNodeIp> <newNodePort>
                int newNodeId = Integer.parseInt(args[1]);
                // Message format: SUCCESSORFOUND <requestedId> <successorId> <successorNodeIp> <successorNodePort>
                String[] successorArgs = this.node.findSuccessor(newNodeId);
                InetSocketAddress newNodeInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                InetSocketAddress successorInfo = new InetSocketAddress(successorArgs[3], Integer.parseInt(successorArgs[4]));
                int successorId = Integer.parseInt(successorArgs[2]);

                this.ch.sendWelcomeMessage(newNodeInfo, successorId, successorInfo);
                break;
            }

            case "WELCOME": {
                System.out.println("[WELCOME]");

                // Message format: WELCOME <successorId> <successorIP> <successorPort>
                int successorId = Integer.parseInt(args[1]);
                InetSocketAddress successorInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                NodePair<Integer, InetSocketAddress> successor = new NodePair<>(successorId, successorInfo);

                this.node.setSuccessor(successor);
                break;
            }

            case "GETPREDECESSOR": {
                System.out.println("[GETPREDECESSOR]");
                NodePair<Integer, InetSocketAddress> predecessor = this.node.getPredecessor();
                InetSocketAddress destination = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

                if (predecessor == null)
                    this.ch.sendNullPredecessorMessage(destination);
                else
                    this.ch.sendPredecessorMessage(predecessor.getKey(), predecessor.getValue(), destination);
                break;
            }

            case "PREDECESSOR": {
                System.out.println("[PREDECESSOR]");

                synchronized (this.node) {
                    // get 'chord's successor's predecessor
                    NodePair<Integer, InetSocketAddress> successor = this.node.getSuccessor();

                    if (args[1].equals("NULL")) {
                        this.ch.sendNotifyMessage(this.node.getId(), this.node.getAddress(), successor.getValue());
                        return;
                    }

                    int predecessorId = Integer.parseInt(args[1]);
                    InetSocketAddress predecessorInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

                    NodePair<Integer, InetSocketAddress> successorsPredecessor = new NodePair<>(predecessorId, predecessorInfo);

                    // check if successor's predecessor ID is between 'chord' and 'chords's successor
                    // if so, then successorsPredecessor is our new successor
                    if (Utils.inBetween(successorsPredecessor.getKey(), this.node.getId(), successor.getKey(), this.node.getM()))
                        this.node.setSuccessor(successorsPredecessor);

                    // notify 'chord's successor of 'chord's existance
                    this.ch.sendNotifyMessage(this.node.getId(), this.node.getAddress(), successor.getValue());
                }

                break;
            }

            case "NOTIFY": {
                System.out.println("[NOTIFY]");
                int originId = Integer.parseInt(args[1]);
                InetSocketAddress originInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                this.node.notify(new NodePair<>(originId, originInfo));
                break;
            }

            case "PING": {
                System.out.println("[PING]");
                InetSocketAddress originInfo = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
                this.ch.sendPongMessage(this.node.getAddress(), originInfo);
                break;
            }

            case "PONG": {
                System.out.println("[PONG]");
                synchronized (this.node) {
                    InetSocketAddress sfAddress = getAddress(socket);
                    this.ch.addMessageQueue(new Message(sfAddress, message));
                    this.node.notify();
                }
                break;
            }
        }

    }

}
