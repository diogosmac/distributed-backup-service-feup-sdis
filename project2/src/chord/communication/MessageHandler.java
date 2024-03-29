package chord.communication;

import chord.ChordNode;
import chord.NodePair;
import chord.Utils;
import storage.Chunk;
import utils.MyUtils;

import java.net.InetSocketAddress;

/**
 * Message Handler
 * <p>
 * This class is used by Chord Communication Channels to
 * delegate message request actions by creating an instance
 * of this class.
 * This is a working thread, that performs actions in a chord's
 * node information to update it based on incoming messages
 */
public class MessageHandler extends Thread {

    /**
     * Incoming Message
     */
    private final String message;

    /**
     * Node's Communication Channel
     */
    private final ChordChannel channel;

    /**
     * Chord's Node
     */
    private final ChordNode node;

    /**
     * Default Constructor
     *
     * @param message Message
     * @param channel Channel
     * @param node    Node
     */
    MessageHandler(String message, ChordChannel channel, ChordNode node) {
        this.message = message;
        this.channel = channel;
        this.node = node;
    }

    /**
     * Method performed by MessageHandler thread
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Override
    public void run() {
        // Handles a message received by the ChordChannel
        String[] args = message.split(" ");
        switch (args[0]) {
            case "FINDSUCCESSOR": {
                // Message format: FINDSUCCESSOR <requestedId> <originIP> <originPort>
                int id = Integer.parseInt(args[1]);
                InetSocketAddress fsAddress = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                this.node.findSuccessor(fsAddress, id);
                break;

            }

            case "SUCCESSORFOUND": {
                synchronized (this.node) {
                    this.channel.addMessageQueue(new Message(message));
                    this.node.notify();
                }
                break;

            }

            case "JOINING": {
                // Message format: JOINING <newNodeId> <newNodeIp> <newNodePort>
                int newNodeId = Integer.parseInt(args[1]);
                // Message format: SUCCESSORFOUND <requestedId> <successorId> <successorNodeIp>
                // <successorNodePort>
                String[] successorArgs = this.node.findSuccessor(newNodeId);
                InetSocketAddress newNodeInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                InetSocketAddress successorInfo = new InetSocketAddress(successorArgs[3],
                        Integer.parseInt(successorArgs[4]));
                int successorId = Integer.parseInt(successorArgs[2]);

                this.channel.sendWelcomeMessage(newNodeInfo, successorId, successorInfo);
                break;

            }

            case "WELCOME": {
                // Message format: WELCOME <successorId> <successorIP> <successorPort>
                int successorId = Integer.parseInt(args[1]);
                InetSocketAddress successorInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                NodePair<Integer, InetSocketAddress> successor = new NodePair<>(successorId, successorInfo);

                this.node.setSuccessor(successor);
                break;

            }

            case "GETPREDECESSOR": {
                NodePair<Integer, InetSocketAddress> predecessor = this.node.getPredecessor();
                InetSocketAddress destination = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

                if (predecessor.getKey() == null)
                    this.channel.sendNullPredecessorMessage(destination);
                else
                    this.channel.sendPredecessorMessage(predecessor.getKey(), predecessor.getValue(), destination);

                break;

            }

            case "PREDECESSOR": {
                synchronized (this.node) {
                    // get chord's successor's predecessor
                    NodePair<Integer, InetSocketAddress> successor = this.node.getSuccessor();

                    if (args[1].equals("NULL")) {
                        this.channel.sendNotifyMessage(this.node.getID(), this.node.getAddress(), successor.getValue());
                        return;
                    }

                    int predecessorId = Integer.parseInt(args[1]);
                    InetSocketAddress predecessorInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));

                    NodePair<Integer, InetSocketAddress> successorsPredecessor = new NodePair<>(predecessorId,
                            predecessorInfo);

                    // check if successor's predecessor ID is between 'chord' and 'chords's successor
                    // if so, then successorsPredecessor is our new successor
                    if (Utils.inBetween(successorsPredecessor.getKey(), this.node.getID(), successor.getKey(),
                            this.node.getM()))
                        this.node.setSuccessor(successorsPredecessor);

                    // notify 'chord's successor of 'chord's existence
                    this.channel.sendNotifyMessage(this.node.getID(), this.node.getAddress(), successor.getValue());
                    // send message to update successor list
                    this.channel.sendFindSuccessorListMessage(this.node.getAddress(), successor.getValue());
                }

                break;
            }

            case "NOTIFY": {
                int originId = Integer.parseInt(args[1]);
                InetSocketAddress originInfo = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
                this.node.notify(new NodePair<>(originId, originInfo));
                break;
            }

            case "PING": {
                InetSocketAddress originInfo = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
                this.channel.sendPongMessage(this.node.getAddress(), originInfo);
                break;
            }

            case "PONG": {
                synchronized (this.node) {
                    this.channel.addMessageQueue(new Message(message));
                    this.node.notify();
                }
                break;
            }

            case "FINDSUCCESSORLIST": {
                InetSocketAddress originInfo = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
                this.channel.sendSuccessorListMessage(originInfo);
                break;
            }

            case "SUCCESSORLIST": {
                NodePair<Integer, InetSocketAddress> successor = this.node.getSuccessor();
                this.node.getSuccessorList().clear();
                this.node.addSuccessor(successor);

                for (int i = 1; i < args.length; i += 3) {
                    NodePair<Integer, InetSocketAddress> node = new NodePair<>(Integer.parseInt(args[i]),
                            new InetSocketAddress(args[i + 1], Integer.parseInt(args[i + 2])));

                    this.node.addSuccessor(node);
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
                String dataStr = message.substring(message.indexOf(args[9]));
                byte[] data = MyUtils.convertStringToByteArray(dataStr);

                // If the request origin == this node => Send same message to successor
                if (initiatorAdd.equals(this.node.getAddress())) {
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                    break;
                } else {
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
                    } else {
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
                System.out.println("[DELETE]");
                // Message format: DELETE <origin-ip> <origin-port> <file-id>
                InetSocketAddress origin = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

                // If is stored, deletes the file
                this.node.deleteFile(args[3]);

                // If the message reaches the originPeer => chain is broken
                if (!origin.equals(this.node.getAddress())) {
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                }
                break;
            }

            case "REMOVED": {
                System.out.println("[REMOVED]");
                // Message format: REMOVED <file-id> <chunk-number>
                String fileID = args[1];
                int chunkNumber = Integer.parseInt(args[2]);

                this.node.getPeer().getFileOccurrences().incrementReplicationDegree(fileID, chunkNumber, -1);

                if (!this.node.getPeer().getFileOccurrences().isReplicationDegreeMet(fileID, chunkNumber)) {
                    Integer chunkID = Utils.hash(fileID + ":" + chunkNumber);
                    String[] reply = this.node.findSuccessor(chunkID);
                    InetSocketAddress succAddress = new InetSocketAddress(reply[3], Integer.parseInt(reply[4]));
                    this.channel.sendEnsureRDMessage(this.node.getAddress(), succAddress, fileID, chunkNumber, succAddress);
                }
                break;
            }

            case "ENSURERD": {
                System.out.println("[ENSURERD]");
                // Message format: ENSURERD <initiator-ip> <initiator-port> <first-successor-ip> <first-successor-port> <hash> <file-id> <chunk-number>
                InetSocketAddress initiatorAdd = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
                InetSocketAddress firstSuccessorAdd = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
                String hash = args[5];
                String fileID = args[6];
                int chunkNumber = Integer.parseInt(args[7]);

                if (this.node.getPeer().getChunkStorage().hasChunk(fileID, chunkNumber)) {
                    Chunk ck = this.node.getPeer().getChunkStorage().getChunk(fileID, chunkNumber);
                    byte[] data = MyUtils.trimMessage(ck.getData(), ck.getSize());

                    this.channel.sendSaveChunkMessage(fileID, chunkNumber, initiatorAdd, this.node.getSuccessorAddress(), data, this.node.getSuccessorAddress());
                } else {
                    if (firstSuccessorAdd.equals(this.node.getAddress())) {
                        if (this.node.hitWall(hash)) {
                            // Message has cycled, and came back => It wasn't possible to keep RD
                            System.out.println("Unable to keep desired rd of chunk #" + chunkNumber + " of file id=" + fileID + ":");
                            System.out.println("No nodes with chunk stored were found");
                            break;
                        } else {
                            this.node.placeWall(hash);
                        }
                    }

                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                }
                break;
            }

            case "SAVECHUNK": {
                System.out.println("[SAVECHUNK]");
                // Message format: SAVECHUNK <file-id> <chunk-number> <hash> <initiator-ip> <initiator-port> <first-successor-ip> <first-successor-port> <data>
                String fileID = args[1];
                int chunkNumber = Integer.parseInt(args[2]);
                InetSocketAddress initiatorAddress = new InetSocketAddress(args[4], Integer.parseInt(args[5]));
                InetSocketAddress firstSuccessor = new InetSocketAddress(args[6], Integer.parseInt(args[7]));
                String hash = args[3];

                if (firstSuccessor.equals(this.node.getAddress())) {
                    if (this.node.hitWall(hash)) {
                        // Message has cycled, and came back => It wasn't possible to keep RD
                        System.out.println("Unable to keep desired rd of chunk #" + chunkNumber + " of file id=" + fileID + ":");
                        System.out.println("No nodes available to store chunk");
                        break;
                    } else {
                        this.node.placeWall(hash);
                    }
                } else if (initiatorAddress.equals(this.node.getAddress())) {
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                    break;
                }

                if (!this.node.getPeer().getChunkStorage().hasChunk(fileID, chunkNumber)) {
                    Integer reclaimHash = Utils.hash(initiatorAddress.getHostString() + " " + initiatorAddress.getPort() + " " + fileID + " " + chunkNumber);
                    // Checks if the received chunk was recently deleted due to a reclaim
                    if (this.node.hitWall(reclaimHash.toString())) {
                        // Continues chain; Doesn't store chunk
                        this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                        break;
                    }

                    String dataStr = message.substring(message.indexOf(args[8]));
                    byte[] data = MyUtils.convertStringToByteArray(dataStr);
                    Chunk ck = new Chunk(fileID, chunkNumber, data, data.length);

                    int addChunkResult = this.node.getPeer().getChunkStorage().addChunk(ck, initiatorAddress);
                    if (addChunkResult == 1 || addChunkResult == 2) {
                        this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                    } else {
                        this.channel.sendChunkSavedMessage(fileID, chunkNumber, initiatorAddress);
                    }
                } else {
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                }
                break;

            }

            case "GETCHUNK": {
                System.out.println("[GETCHUNK]");
                // Message format: GETCHUNK <initiator-ip> <initiator-port> <first-successor-ip> <first-successor-port> <file-id> <chunk-number> <hash>
                InetSocketAddress firstSuccessor = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
                String fileID = args[5];
                int chunkNumber = Integer.parseInt(args[6]);

                // If ha chunk stored, sends it to the initiator; Ends chain of GetChunk
                if (this.node.getPeer().getChunkStorage().hasChunk(fileID, chunkNumber)) {
                    InetSocketAddress initiator = new InetSocketAddress(args[1], Integer.parseInt(args[2]));

                    Chunk ck = this.node.getPeer().getChunkStorage().getChunk(fileID, chunkNumber);
                    byte[] data = MyUtils.trimMessage(ck.getData(), ck.getSize());

                    this.channel.sendChunkMessage(fileID, chunkNumber, data, initiator);
                } else {
                    if (firstSuccessor.equals(this.node.getAddress())) {
                        String hash = args[7];

                        if (this.node.hitWall(hash)) {
                            // Message has cycled, and came back => Chunk doesn't exist => End of chain
                            System.out.println("Couldn't get chunk #" + chunkNumber + " of file with id=" + fileID);
                            break;
                        } else {
                            this.node.placeWall(hash);
                        }
                    }

                    // Continues chain
                    this.channel.sendMessage(this.node.getSuccessorAddress(), message);
                }
                break;
            }

            case "CHUNK": {
                System.out.println("[CHUNK]");
                // Message format: CHUNK <file-id> <chunk-number> <data>
                String fileID = args[1];
                int chunkNumber = Integer.parseInt(args[2]);
                String dataStr = message.substring(message.indexOf(args[3]));
                byte[] data = MyUtils.convertStringToByteArray(dataStr);
                this.node.getPeer().getFileRestorer().saveRestoredChunk(fileID, chunkNumber, data);
                break;
            }

            case "CHUNKSAVED": {
                System.out.println("[CHUNKSAVED]");
                // Message format: CHUNKSAVED <file-id> <chunk-number>
                String fileID = args[1];
                int chunkNumber = Integer.parseInt(args[2]);

                this.node.getPeer().getFileOccurrences().incrementReplicationDegree(fileID, chunkNumber, 1);
                break;
            }


        }

    }

}
