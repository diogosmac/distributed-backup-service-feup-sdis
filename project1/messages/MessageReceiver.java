package messages;

import messages.replies.ChunkReceiver;
import messages.replies.DeletedFileReceiver;
import messages.replies.RemovedReceiver;
import messages.replies.StoredChunkReceiver;
import messages.requests.DeleteReceiver;
import messages.requests.GetChunkReceiver;
import messages.requests.HelloWorldReceiver;
import messages.requests.PutChunkReceiver;
import peer.Peer;
import utils.MyUtils;

public class MessageReceiver implements Runnable {

    private final byte[] message;
    private final int length;
    private final Peer peer;

    public MessageReceiver(byte[] message, int length, Peer peer) {
        this.message = MyUtils.trimMessage(message, length);
        this.length = length;
        this.peer = peer;
    }

    @Override
    public void run() {

        String messageStr = MyUtils.convertByteArrayToString(message);
        String[] args = messageStr.split(" ");
        int senderId = Integer.parseInt(args[2]);

        String messageType = args[1];

        switch (messageType) {

            case "PUTCHUNK":
                if (senderId == peer.getPeerId()) break;
                System.out.println("\tPUTCHUNK Message received    | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2] + ", " +
                                                                    "storage.Chunk #" + args[4] + ", " +
                                                                    "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new PutChunkReceiver(message, peer));
                break;

            case "STORED":
                if (senderId == peer.getPeerId()) break;
                System.out.println("\tSTORED Message received      | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2] + ", " +
                                                                    "storage.Chunk #" + args[4] + ", " +
                                                                    "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new StoredChunkReceiver(message, peer));
                break;


            case "GETCHUNK":
                if (senderId == peer.getPeerId()) break;
                System.out.println("\tGETCHUNK Message received    | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2] + ", " +
                                                                    "storage.Chunk #" + args[4] + ", " +
                                                                    "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new GetChunkReceiver(message, peer));
                break;

            case "CHUNK":
                if (senderId == peer.getPeerId()) break;
                System.out.println("\tCHUNK Message received       | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2] + ", " +
                                                                    "storage.Chunk #" + args[4] + ", " +
                                                                    "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new ChunkReceiver(message, peer));
                break;


            case "DELETE":
                System.out.println("\tDELETE Message received      | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2] + ", " +
                                                                    "File ID: " + args[3] + ", " +
                                                                    "Number bytes: " + length);
                System.out.flush();
                peer.executeThread(new DeleteReceiver(message, peer));
                break;


            case "REMOVED":
                System.out.println("\tREMOVED Message received     | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2] + ", " +
                                                                    "File ID: " + args[3] + ", " +
                                                                    "storage.Chunk #" + args[4]);
                System.out.flush();
                peer.executeThread(new RemovedReceiver(message, peer));
                break;

            case "DELETEDFILE":
                System.out.println("\tDELETEDFILE Message received | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2] + ", " +
                                                                    "File ID: " + args[3]);
                System.out.flush();
                peer.executeThread(new DeletedFileReceiver(message, peer));
                break;

            case "HELLOWORLD":
                System.out.println("\tHELLOWORLD Message received  | Type: " + args[1] + ", " +
                                                                    "Sender: " + args[2]);
                System.out.flush();
                peer.executeThread(new HelloWorldReceiver(message, peer));
                break;

            default:
                System.out.println("\tUnknown Message received  | " + messageStr);
                System.out.flush();
                break;

        }

    }

}
