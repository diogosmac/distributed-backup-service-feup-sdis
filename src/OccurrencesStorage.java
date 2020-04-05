import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OccurrencesStorage {

    private HashMap<String, List<Integer>> chunk_occurrences;
    
    public OccurrencesStorage() {
        this.chunk_occurrences = new HashMap<>();
    }

    public void addFile (String file_id) {
        this.chunk_occurrences.put(file_id, new ArrayList<>());
    }

    public void addChunkSlot(String file_id) {
        this.chunk_occurrences.get(file_id).add(0);
    }

    public void incChunkOcc(String file_id, int chunk_number) {
        int current_n_occurr = getChunkOccurrences(file_id, chunk_number);
        current_n_occurr++;
        this.chunk_occurrences.get(file_id).set(chunk_number, current_n_occurr);
    }

    public int getChunkOccurrences(String file_id, int chunk_number) {
        return this.chunk_occurrences.get(file_id).get(chunk_number);
    }

}
