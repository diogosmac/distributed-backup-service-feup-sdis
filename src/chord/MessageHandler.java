package chord;

import storage.Chunk;
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

        switch (args[0]) {

            case "FINDSUCCESSOR": {
                System.out.println("[FINDSUCCESSOR]");
                // Message format: FINDSUCCESSOR <requested-id> <origin-ip> <origin-port>
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
                // Message format: JOINING <new-node-id> <new-node-ip> <new-node-port>
                int newNodeId = Integer.parseInt(args[1]);
                InetSocketAddress newNodeInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

                String[] successorArgs = this.node.findSuccessor(newNodeId);
                // Message format: SUCCESSORFOUND <requested-id> <successor-id> <successor-node-ip> <successor-node-port>
                int successorId = Integer.parseInt(successorArgs[2]);
                InetSocketAddress successorInfo = new InetSocketAddress(
                        successorArgs[3], Integer.parseInt(successorArgs[4]));

                this.channel.sendWelcomeMessage(newNodeInfo, successorId, successorInfo);
                break;

            }

            case "WELCOME": {
                System.out.println("[WELCOME]");
                // Message format: WELCOME <successor-id> <successor-ip> <successor-port>
                int successorId = Integer.parseInt(args[1]);
                InetSocketAddress successorInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                NodePair<Integer, InetSocketAddress> successor = new NodePair<>(successorId, successorInfo);

                this.node.setSuccessor(successor);
                break;

            }

            case "GETPREDECESSOR": {
                System.out.println("[GETPREDECESSOR]");
                // Message format: GETPREDECESSOR <destination-ip> <destination-port>
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
                // Message format: PREDECESSOR <predecessor-id> <predecessor-ip> <predecessor-port>
                synchronized (this.node) {
                    // get chord's successor's predecessor
                    NodePair<Integer, InetSocketAddress> successor = this.node.getSuccessor();

                    if (args[1].equals("NULL")) {
                        this.channel.sendNotifyMessage(this.node.getID(), this.node.getAddress(), successor.getValue());
                        return;
                    }

                    int predecessorId = Integer.parseInt(args[1]);
                    InetSocketAddress predecessorInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

                    NodePair<Integer, InetSocketAddress> successorsPredecessor = new NodePair<>(predecessorId, predecessorInfo);

                    // check if successor's predecessor ID is between 'chord' and 'chords's successor
                    // if so, then successorsPredecessor is our new successor
                    if (Utils.inBetween(successorsPredecessor.getKey(), this.node.getID(), successor.getKey(), this.node.getM()))
                        this.node.setSuccessor(successorsPredecessor);

                    // notify 'chord's successor of 'chord's existence
                    this.channel.sendNotifyMessage(this.node.getID(), this.node.getAddress(), successor.getValue());
                }

                break;
            }

            case "NOTIFY": {
                System.out.println("[NOTIFY]");
                // Message format: NOTIFY <origin-id> <origin-ip> <origin-port>
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
                // PUTCHUNK <file-id> <chunk-number> <replication-degree> <initiator-ip> <initiator-port> <first-successor-ip> <first-successor-port> <data>
                String fileID = args[1];
                int chunkNumber = Integer.parseInt(args[2]);
                int replicationDegree = Integer.parseInt(args[3]);
                InetSocketAddress initiatorAdd = new InetSocketAddress(args[4], Integer.parseInt(args[5]));
                InetSocketAddress firstSuccAdd = new InetSocketAddress(args[6], Integer.parseInt(args[7]));
                String hash = args[8];
                byte[] data = MyUtils.convertStringToByteArray(args[9]);

                // If the request origin == this node => Send same message to successor
                if (initiatorAdd.equals(this.node.getAddress())) {
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                    break;
                }
                else {
                    if (firstSuccAdd.equals(this.node.getAddress())) {
                        if (this.node.hitWall(hash)) {
                            this.channel.sendUpdateFileReplicationDegree(fileID, chunkNumber, replicationDegree, initiatorAdd);
                            break;
                        } else {
                            this.node.placeWall(hash);
                        }
                    }

                    if (!this.node.isChunkStored(fileID, chunkNumber)) {

                        // Stores chunk and saves return to be handled
                        int storeReply = this.node.storeChunk(fileID, chunkNumber, data, data.length, initiatorAdd);
                        if (storeReply == 1 || storeReply == 2) {
                            // storeReply == 1 => Not Enough Memory
                            // storeReply == 2 => Error Writing file
                            this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                            break;
                        }

                    }

                    // Updates RD and continues the chain
                    if (replicationDegree > 1) {
                        this.channel.sendPutchunkMessage(fileID, chunkNumber, replicationDegree - 1,
                                hash, data, initiatorAdd, firstSuccAdd, this.node.getSuccessorAddress());
                    }
                    else {
                        this.channel.sendUpdateFileReplicationDegree(
                                fileID, chunkNumber, replicationDegree - 1, initiatorAdd);
                        break;
                    }

                }

                break;

            }
            case "UPDATERD": {
                System.out.println("[UPDATERD]");
                // Message format: UPDATERD <file-id> <chunk-number> <missing-replicas>
                String fileID = args[1];
                int chunkNumber = Integer.parseInt(args[2]);
                int missing = Integer.parseInt(args[3]);

                if (!this.node.getPeer().getFileOccurrences().hasFile(fileID)) {
                    System.out.println("ERROR: File with ID " + fileID + " has not been backed up by node #" + this.node.getID());
                    break;
                }

                this.node.getPeer().getFileOccurrences().updateFileChunk(fileID, chunkNumber, missing);
                break;

            }
            case "DELETE": {
                // Message format: DELETE <origin-ip> <origin-port> <file-id>
                InetSocketAddress origin = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

                // If is stored, deletes the file
                this.node.deleteFile(args[3]);

                // If the message reaches the originPeer => chain is broken
                if (!origin.equals(this.node.getAddress())) {
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                }
            }

        }

    }

}
