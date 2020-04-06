import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OccurrencesStorage {

    private ConcurrentHashMap<String, List<Integer>> chunkOccurrences;
    
    public OccurrencesStorage() {
        this.chunkOccurrences = new ConcurrentHashMap<>();
    }

    public void addFile (String fileId) {
        this.chunkOccurrences.put(fileId, new ArrayList<>());
    }

    public void addChunkSlot(String fileId) {
        this.chunkOccurrences.get(fileId).add(0);
    }

    public void incChunkOcc(String fileId, int chunkNumber) {
        int occurrenceCount = getChunkOccurrences(fileId, chunkNumber);
        occurrenceCount++;
        this.chunkOccurrences.get(fileId).set(chunkNumber, occurrenceCount);
    }

    public List<Integer> getFileOccurrences(String fileId) {
        return this.chunkOccurrences.get(fileId);
    }

    public int getChunkOccurrences(String fileId, int chunkNumber) {
        return this.getFileOccurrences(fileId).get(chunkNumber);
    }

    public void deleteOccurrences(String fileId) {
        this.chunkOccurrences.remove(fileId);
    }

}
