import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return this.getFileOccurrences(fileId).replicationDegree > this.getChunkOccurrences(fileId, chunkNumber);
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

    public String getOccurencesInfo() {
//        For each file whose backup it has initiated:
//        The file pathname
//        The backup service id of the file
//        The desired replication degree
//        For each chunk of the file:
//        Its id
//        Its perceived replication degree

        String sectionHeader = "Backed up files Section ============\n\n";
        String infoBody = "";

        for (Map.Entry<String, OccurrenceInfo> entry : this.chunkOccurrences.entrySet()) {
            String fileId = entry.getKey();
            OccurrenceInfo oi = entry.getValue();
            String fileName = oi.fileName;
            List<Integer> occurrences = oi.occurrences;

            infoBody += String.join("\n\t", "\tFile name: " + fileName, "File id: " + fileId,
                    "Desired replication degree: " + getReplicationDegree(fileId));

            for (int i = 0; i < occurrences.size(); i++) {
                infoBody += String.join("\t\t\n", "\t\tChunk #" + i,
                        "Perceived replication degree: " + occurrences.get(i));
                infoBody += "\n\n";
            }
        }
        return sectionHeader + infoBody;
    }

}
