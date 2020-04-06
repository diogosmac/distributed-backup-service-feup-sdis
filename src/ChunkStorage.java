import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkStorage {

    // Key:     <file_id>:<chunk_number>
    // Value:   Chunk
    private ConcurrentHashMap<String, List<Chunk>> chunkStorage;

    public ChunkStorage() { this.chunkStorage = new ConcurrentHashMap<>(); }

    public void addChunk(Chunk chunk) {
        String fileId = chunk.getFileID();
        if (!this.chunkStorage.containsKey(fileId))
            this.chunkStorage.put(fileId, new ArrayList<>());
        this.chunkStorage.get(fileId).add(chunk);
    }

    public Chunk getChunk(String fileId, int chunkNumber) {
        if (this.chunkStorage.containsKey(fileId))
            for (Chunk chunk : this.chunkStorage.get(fileId))
                if (chunk.getNum() == chunkNumber)
                    return chunk;
        return null;
    }

    public boolean hasChunk(String fileId, int chunkNumber) {
        if (this.chunkStorage.containsKey(fileId))
            for (Chunk chunk : this.chunkStorage.get(fileId))
                if (chunk.getNum() == chunkNumber)
                    return true;
        return false;
    }

    public void deleteFile(String fileId) {
        this.chunkStorage.remove(fileId);
    }

}
