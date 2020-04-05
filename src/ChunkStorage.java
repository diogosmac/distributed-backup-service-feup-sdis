import java.util.HashMap;

public class ChunkStorage {

    // Key:     <file_id>:<chunk_number>
    // Value:   Chunk
    private HashMap<String, Chunk> chunkStorage;

    public ChunkStorage() {
        this.chunkStorage = new HashMap<>();
    }

    public void addChunk(Chunk chunk) {
        String key = chunk.getFileID() + ":" + chunk.getNum();
        this.chunkStorage.put(key, chunk);
    }

    public Chunk getChunk(String fileId, int chunkNumber) {
        String key = fileId + ":" + chunkNumber;
        return this.chunkStorage.get(key);
    }

}
