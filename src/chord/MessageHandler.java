package chord;

import utils.MyUtils;

import javax.net.ssl.SSLSocket;
import java.net.InetSocketAddress;

public class MessageHandler extends Thread {
    private final String message;
    private final SSLSocket socket;
    private final ChordChannel channel;
    private final ChordNode node;

    MessageHandler(SSLSocket sk, String message, ChordChannel channel, ChordNode node) {
        this.socket = sk;
        this.message = message;
        this.channel = channel;
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
        // TODO: Handle received message
        String[] args = message.split(" ");
        switch(args[0]) {
            case "FINDSUCCESSOR": {
                System.out.println("[FINDSUCCESSOR]");
                // Message format: FINDSUCCESSOR <requestedId> <originIP> <originPort>
                System.out.println("IN HANDLER -> " + this.message);
                int id = Integer.parseInt(args[1]);
                InetSocketAddress fsAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                System.out.println("BEFORE FIND SUCCESSOR IN HANDLER -> " + this.message);
                this.node.findSuccessor(fsAddress, id);
                break;
            }

            case "SUCCESSORFOUND": {
                System.out.println("[SUCCESSORFOUND]");
                synchronized (this.node) {
                    InetSocketAddress sfAddress = getAddress(socket);
                    this.channel.addMessageQueue(new Message(sfAddress, message));
                    this.node.notify();
                }
                break;
            }

            case "JOINING": {
                System.out.println("[JOINING]");
                // Message format: JOINING <newNodeId> <newNodeIp> <newNodePort>
                int newNodeId = Integer.parseInt(args[1]);
                InetSocketAddress newNodeInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

                String[] successorArgs = this.node.findSuccessor(newNodeId);
                // Message format: SUCCESSORFOUND <requestedId> <successorId> <successorNodeIp> <successorNodePort>
                int successorId = Integer.parseInt(successorArgs[2]);
                InetSocketAddress successorInfo = new InetSocketAddress(
                        successorArgs[3], Integer.parseInt(successorArgs[4]));

                this.channel.sendWelcomeMessage(newNodeInfo, successorId, successorInfo);
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
                    this.channel.sendNullPredecessorMessage(destination);
                else
                    this.channel.sendPredecessorMessage(predecessor.getKey(), predecessor.getValue(), destination);
                break;
            }

            case "PREDECESSOR": {
                System.out.println("[PREDECESSOR]");

                synchronized (this.node) {
                    // get chord's successor's predecessor
                    NodePair<Integer, InetSocketAddress> successor = this.node.getSuccessor();

                    if (args[1].equals("NULL")) {
                        this.channel.sendNotifyMessage(this.node.getId(), this.node.getAddress(), successor.getValue());
                        return;
                    }

                    int predecessorId = Integer.parseInt(args[1]);
                    InetSocketAddress predecessorInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

                    NodePair<Integer, InetSocketAddress> successorsPredecessor = new NodePair<>(predecessorId, predecessorInfo);

                    // check if successor's predecessor ID is between 'chord' and 'chords's successor
                    // if so, then successorsPredecessor is our new successor
                    if (Utils.inBetween(successorsPredecessor.getKey(), this.node.getId(), successor.getKey(), this.node.getM()))
                        this.node.setSuccessor(successorsPredecessor);

                    // notify 'chord's successor of 'chord's existence
                    this.channel.sendNotifyMessage(this.node.getId(), this.node.getAddress(), successor.getValue());
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
                this.channel.sendPongMessage(this.node.getAddress(), originInfo);
                break;
            }

            case "PONG": {
                System.out.println("[PONG]");
                synchronized (this.node) {
                    InetSocketAddress sfAddress = getAddress(socket);
                    this.channel.addMessageQueue(new Message(sfAddress, message));
                    this.node.notify();
                }
                break;
            }

            case "PUTCHUNK": {
                System.out.println("[PUTCHUNK]");
                // PUTCHUNK <chunk-id> <replication-degree> <origin-ip> <origin-port> <data>
                InetSocketAddress senderAddress = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
                if (senderAddress.equals(this.node.getAddress())) {
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                }
                else {
                    Integer chunkID = Integer.parseInt(args[1]);
                    int replicationDegree = Integer.parseInt(args[2]);
                    byte[] data = MyUtils.convertStringToByteArray(args[5]);

                    // TODO: check if chunk is not yet stored

                    // TODO: store chunk
                    //
                    //      this is the part where we store the chunk
                    //
                    // chunk is now stored

                    if (replicationDegree > 1) {
                        this.channel.sendPutchunkMessage(chunkID, replicationDegree - 1, data,
                                this.node.getAddress(), this.node.getSuccessorAddress());
                    }
                }
                break;

            }

        }

    }

}
