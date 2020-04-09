import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OccurrencesStorage {

    private static class OccurrenceInfo {

        private String fileName;
        private int replicationDegree;
        private List<Integer> occurrences;

        public OccurrenceInfo(String fileName, int replicationDegree) {
            this.fileName = fileName;
            this.replicationDegree = replicationDegree;
            this.occurrences = new ArrayList<>();
        }

        public void addChunkSlot() {
            this.occurrences.add(0);
        }

        public int getChunkOccurrences(int chunkNumber) {
            return this.occurrences.get(chunkNumber);
        }

        public void updateChunkOccurrence(int chunkNumber, int delta) {
            int occurrences = getChunkOccurrences(chunkNumber) + delta;
            this.occurrences.set(chunkNumber, occurrences);
        }

        public String getFileName() { return this.fileName; }

        public int getReplicationDegree() { return this.replicationDegree; }

        public List<Integer> getListOccurrences() { return this.occurrences; }

    }

    private ConcurrentHashMap<String, OccurrenceInfo> chunkOccurrences;
    
    public OccurrencesStorage() {
        this.chunkOccurrences = new ConcurrentHashMap<>();
    }

    public void addFile (String fileId, String fileName, int replicationDegree) {
        this.chunkOccurrences.put(fileId, new OccurrenceInfo(fileName, replicationDegree));
    }

    public void addChunkSlot(String fileId) {
        this.chunkOccurrences.get(fileId).addChunkSlot();
    }

    public void incChunkOcc(String fileId, int chunkNumber) {
        this.getFileOccurrences(fileId).updateChunkOccurrence(chunkNumber, 1);
    }

    public void decChunkOcc(String fileId, int chunkNumber) {
        this.getFileOccurrences(fileId).updateChunkOccurrence(chunkNumber, -1);
    }

    public boolean checkChunkReplicationDegree(String fileId, int chunkNumber) {
        return this.getFileOccurrences(fileId).replicationDegree <= this.getChunkOccurrences(fileId, chunkNumber);
    }

    public OccurrenceInfo getFileOccurrences(String fileId) {
        return this.chunkOccurrences.get(fileId);
    }

    public int getChunkOccurrences(String fileId, int chunkNumber) {
        return this.getFileOccurrences(fileId).getChunkOccurrences(chunkNumber);
    }

    public void deleteOccurrences(String fileId) { this.chunkOccurrences.remove(fileId); }

    public String getFileName(String fileID) { return this.chunkOccurrences.get(fileID).getFileName(); }

    public int getReplicationDegree(String fileId) { return this.chunkOccurrences.get(fileId).getReplicationDegree(); }

}
