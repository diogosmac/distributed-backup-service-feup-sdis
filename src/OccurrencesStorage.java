import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OccurrencesStorage {

    private class OccurrenceInfo {
        private String fileName;
        private List<Integer> occurrences;

        public OccurrenceInfo(String fileName) {
            this.fileName = fileName;
            this.occurrences = new ArrayList<>();
        }

        public void addChunkSlot() {
            this.occurrences.add(0);
        }

        public int getChunkOccurrences(int chunkNumber) {
            return this.occurrences.get(chunkNumber);
        }

        public void incChunkOccurrence(int chunkNumber) {
            int occurrences = getChunkOccurrences(chunkNumber) + 1;
            this.occurrences.set(chunkNumber, occurrences);
        }

        public String getFileName() {
            return this.getFileName();
        }

        public List<Integer> getListOccurrences() {
            return this.occurrences;
        }

    }

    private ConcurrentHashMap<String, OccurrenceInfo> chunkOccurrences;
    
    public OccurrencesStorage() {
        this.chunkOccurrences = new ConcurrentHashMap<>();
    }

    public void addFile (String fileId, String fileName) {
        this.chunkOccurrences.put(fileId, new OccurrenceInfo(fileName));
    }

    public void addChunkSlot(String fileId) {
        this.chunkOccurrences.get(fileId).addChunkSlot();
    }

    public void incChunkOcc(String fileId, int chunkNumber) {
        this.getFileOccurrences(fileId).incChunkOccurrence(chunkNumber);
    }

    public OccurrenceInfo getFileOccurrences(String fileId) {
        return this.chunkOccurrences.get(fileId);
    }

    public int getChunkOccurrences(String fileId, int chunkNumber) {
        return this.getFileOccurrences(fileId).getChunkOccurrences(chunkNumber);
    }

    public void deleteOccurrences(String fileId) {
        this.chunkOccurrences.remove(fileId);
    }

    public String getFileName(String fileID) {
        return this.chunkOccurrences.get(fileID).getFileName();
    }

}
