import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkStorage {

    // Key:     <file_id>:<chunk_number>
    // Value:   Paths of the chunks that make up the file
    private ConcurrentHashMap<String, List<String>> chunkStorage;
    private Peer peer;

    public ChunkStorage(Peer peer) {
        this.chunkStorage = new ConcurrentHashMap<>();
        this.peer = peer;
    }

    public void addChunk(Chunk chunk) {
        String fileId = chunk.getFileID();
        if (!this.chunkStorage.containsKey(fileId)) {
            this.chunkStorage.put(fileId, new ArrayList<>());
        }
        String filePath = chunk.getFileID() + "_" + chunk.getNum() + MyUtils.CHUNK_FILE_EXTENSION;
        try {
            File file = new File(MyUtils.getBackupPath(peer) + filePath);
            if (file.getParentFile().mkdirs()) {
                System.out.println("\tCreated missing ./backup directory.");
            }
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(chunk.getData());
            fos.close();
            System.out.println("\t\tStored chunk #" + chunk.getNum() + ": " + chunk.getSize() + " bytes");
            this.chunkStorage.get(fileId).add(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        File file = new File(MyUtils.getBackupPath(peer) + filePath);
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
                String fileChunkNum = path.substring(
                        path.lastIndexOf("_") + 1, path.indexOf("."));
                if (Integer.parseInt(fileChunkNum) == chunkNumber)
                    return true;
            }
        return false;
    }

    public void deleteFile(String fileId) {
        this.chunkStorage.remove(fileId);
    }

}
