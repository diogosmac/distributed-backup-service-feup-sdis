import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OccurrencesStorage {

    private HashMap<String, List<Integer>> chunkOccurrences;
    
    public OccurrencesStorage() {
        this.chunkOccurrences = new HashMap<>();
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

    public int getChunkOccurrences(String fileId, int chunkNumber) {
        return this.chunkOccurrences.get(fileId).get(chunkNumber);
    }

}
