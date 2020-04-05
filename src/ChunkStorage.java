import java.util.HashMap;

public class ChunkStorage {

    // Key: file_id:chunk_number
    private HashMap<String, Chunk> chunk_storage;

    public ChunkStorage() {

    }

    public void addChunk (Chunk chunk) {
        String key = chunk.getFileID() + ":" + chunk.getNum();
        this.chunk_storage.put(key, chunk);
    }

    public Chunk getChunk (String file_id, int chunk_number) {
        String key = file_id + ":" + chunk_number;
        return this.chunk_storage.get(key);
    }

}
