import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkStorage {

    // Key:     <file_id>
    // Value:   Paths of the chunks that make up the file
    private ConcurrentHashMap<String, List<String>> chunkStorage;
    private Peer peer;
    private String dirPath;
    private long availableMemory;

    public ChunkStorage(Peer peer) {
        this.chunkStorage = new ConcurrentHashMap<>();
        this.peer = peer;
        this.availableMemory = MyUtils.PEER_MAX_MEMORY_USE;
        this.dirPath = MyUtils.getBackupPath(peer);
        this.loadChunks();
    }

    private void loadChunks() {
        int i = 0;
        File dir = new File(dirPath);
        File[] dirListing = dir.listFiles();
        if (dirListing != null) {
            for (File file : dirListing) {
                String fileName = file.getName();
                String fileId = fileName.substring(0, fileName.lastIndexOf("_"));

                this.availableMemory -= file.length();

                if (!this.chunkStorage.containsKey(fileId))
                    this.chunkStorage.put(fileId, new ArrayList<>());
                this.chunkStorage.get(fileId).add(file.getName());
                i++;
            }
        }

        if (i != 0)
            System.out.println("\n\tFound " + i + " files in memory!\n");
        else System.out.println("\n\tNo files found in memory!\n");

        if (this.availableMemory < 0) {
            System.out.println("\tWARNING: Using " + (-0.001*this.availableMemory) + " KB over the memory limit!\n");
        }
    }

    public int addChunk(Chunk chunk) {
        if (chunk.getSize() > this.availableMemory)
            return 1;

        String fileId = chunk.getFileID();
        if (!this.chunkStorage.containsKey(fileId)) {
            this.chunkStorage.put(fileId, new ArrayList<>());
        }
        String fileName = chunk.getFileID() + "_" + chunk.getNum() + MyUtils.CHUNK_FILE_EXTENSION;
        try {
            File file = new File(dirPath + fileName);
            if (file.getParentFile().mkdirs()) {
                System.out.println("\tCreated missing ./backup directory.");
            }
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(chunk.getData());
            fos.close();
            this.chunkStorage.get(fileId).add(fileName);
            this.availableMemory -= chunk.getSize();
            System.out.println("\t\tStored chunk #" + chunk.getNum() + ": " + chunk.getSize() + " bytes");
        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        }

        return 0;
    }

    public Chunk getChunk(String fileId, int chunkNumber) {
        if (!this.chunkStorage.containsKey(fileId))
            return null;

        String filePath = null;
        for (String path : this.chunkStorage.get(fileId)) {
            String fileChunkNum = path.substring(
                    path.lastIndexOf("_") + 1, path.indexOf("."));
            if (Integer.parseInt(fileChunkNum) == chunkNumber) {
                filePath = path;
                break;
            }
        }

        if (filePath == null)
            return null;

        File file = new File(dirPath + filePath);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[MyUtils.CHUNK_SIZE];
            int size = fis.read(buffer);
            fis.close();
            return new Chunk(fileId, chunkNumber, buffer, size, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public boolean hasChunk(String fileId, int chunkNumber) {
        if (this.chunkStorage.containsKey(fileId))
            for (String path : this.chunkStorage.get(fileId)) {
                if (path.equals(""))
                    continue;

                String fileChunkNum = path.substring(
                        path.lastIndexOf("_") + 1, path.indexOf("."));

                if (Integer.parseInt(fileChunkNum) == chunkNumber)
                    return true;
            }
        return false;
    }

    public void deleteFile(String fileId) {
        List<String> chunkPaths = this.chunkStorage.get(fileId);
        int numberDeletesFailed = 0;

        for (String fileName : chunkPaths) {

            String path = MyUtils.getBackupPath(this.peer);
            File file = new File(path+fileName);

            long fileSize = file.length();

            if (file.delete())
                this.availableMemory += fileSize;
            else
                numberDeletesFailed++;
        }

        if (numberDeletesFailed > 0)
            System.out.println("Failed to delete " + numberDeletesFailed + " chunks.");

        this.chunkStorage.remove(fileId);
    }

    private String buildRemovedMessage(String fileId, int chunkNumber) {
        // <Version> REMOVED <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
        return String.join(" ", peer.getProtocolVersion(), "REMOVED",
                Integer.toString(peer.getPeerId()), fileId, Integer.toString(chunkNumber), MyUtils.CRLF + MyUtils.CRLF);
    }

    public int reclaimSpace(int amountOfSpace) {

        int spaceInBytes = amountOfSpace * 1000;

        int freedSpace = 0;

        if (MyUtils.PEER_MAX_MEMORY_USE - this.availableMemory > spaceInBytes)
            for (List<String> chunks : chunkStorage.values()) {
                for (String path : chunks) {
                    File file = new File(MyUtils.getBackupPath(this.peer) + path);
                    long fileSize = file.length();
                    String fileId = path.substring(0, path.lastIndexOf("_"));
                    int chunkNumber = Integer.parseInt(path.substring(
                            path.lastIndexOf("_") + 1,
                            path.indexOf(MyUtils.CHUNK_FILE_EXTENSION)));
                    String removedMessage = buildRemovedMessage(fileId, chunkNumber);
                    if (file.delete()) {
                        this.chunkStorage.get(fileId).remove(path);
                        this.availableMemory += fileSize;
                        freedSpace += fileSize;
                        peer.executeThread(new MessageSender(
                                MyUtils.convertStringToByteArray(removedMessage),
                                peer.getMulticastControlChannel()));

                        if (MyUtils.PEER_MAX_MEMORY_USE - this.availableMemory <= spaceInBytes)
                            return freedSpace;
                    }
                }
            }
        return freedSpace;
    }

}
