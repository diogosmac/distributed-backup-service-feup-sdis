import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OccurrencesStorage {

    private static class OccurrenceInfo {

        private String fileName;
        private int replicationDegree;
        private List<List<Integer>> occurrences;

        public OccurrenceInfo(String fileName, int replicationDegree) {
            this.fileName = fileName;
            this.replicationDegree = replicationDegree;
            this.occurrences = new ArrayList<>();
            addChunkSlot();
        }

        public void addChunkSlot() { this.occurrences.add(new ArrayList<>()); }

        public void loadChunk(List<Integer> peers) { this.occurrences.add(peers); }

        public boolean removeChunkOccurrence(int chunkNumber, int peerId) {
            List<Integer> chunkOccurrences = this.occurrences.get(chunkNumber);
            if (chunkOccurrences.contains(peerId)) {
                chunkOccurrences.remove((Integer) peerId);
                return true;
            }
            return false;
        }

        public boolean addChunkOccurrence(int chunkNumber, int peerId) {
            while (this.occurrences.size() < chunkNumber + 1) {
                addChunkSlot();
            }
            if (this.occurrences.get(chunkNumber).contains(peerId))
                return false;

            this.occurrences.get(chunkNumber).add(peerId);
            Collections.sort(this.occurrences.get(chunkNumber));
            return true;
        }

        public String getFileName() { return this.fileName; }

        public int getReplicationDegree() { return this.replicationDegree; }

        public int getNumChunks() { return this.occurrences.size(); }

        public List<Integer> getChunkOccurrences(int chunkNumber) { return this.occurrences.get(chunkNumber); }

        public int getOccurrenceCount(int chunkNumber) { return getChunkOccurrences(chunkNumber).size(); }

    }

    private String statusFilePath;
//    private ConcurrentHashMap<String, OccurrenceInfo> chunkOccurrences;
    public ConcurrentHashMap<String, OccurrenceInfo> chunkOccurrences;

    public OccurrencesStorage(Peer peer) {
        this.statusFilePath = MyUtils.getStatusPath(peer);
        this.chunkOccurrences = loadFromFile();
    }

    public void exportToFile() {
        File file = new File(this.statusFilePath);
        if (file.getParentFile().mkdirs()) {
            String baseDir = this.statusFilePath.substring(0, this.statusFilePath.lastIndexOf('/'));
            System.out.println("Created new " + baseDir + " directory!");
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (Map.Entry<String, OccurrenceInfo> entry : chunkOccurrences.entrySet()) {
                String fileId = entry.getKey();
                OccurrenceInfo info = entry.getValue();
                StringBuilder fileOutput = new StringBuilder(
                        "id: " + fileId + " - name: " + info.fileName + " - rd: " + info.replicationDegree + '\n');
                for (int chunkNumber = 0; chunkNumber < info.getNumChunks(); chunkNumber++) {
                    fileOutput.append(chunkNumber).append(" - ");
                    for (int peer : info.getChunkOccurrences(chunkNumber))
                        fileOutput.append(peer).append(" ");
                    fileOutput.append('\n');
                }
                fileOutput.append('\n');
                bw.write(fileOutput.toString());
            }
            bw.close();
        } catch (Exception e) { System.out.println("Exception while writing to file: " + e.toString()); }
    }

    public ConcurrentHashMap<String, OccurrenceInfo> loadFromFile() {
        ConcurrentHashMap<String, OccurrenceInfo> infoMap = new ConcurrentHashMap<>();
        File file = new File(this.statusFilePath);
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.substring(0, line.indexOf(':') + 1).equals("id:")) {
                        String fileId = line.substring(4, line.indexOf(" - name: "));
                        String fileName = line.substring(line.indexOf("name: ") + 6, line.indexOf(" - rd: "));
                        int rd = Integer.parseInt(line.substring(line.indexOf("rd: ") + 4));
                        OccurrenceInfo info = new OccurrenceInfo(fileName, rd);
                        while (!(line = br.readLine()).equals("")) {
                            List<Integer> peers = new ArrayList<>();
                            String peersStr = line.substring(line.indexOf(" - ") + 3).strip();
                            if (peersStr.length() > 0)
                                for (String peer : peersStr.split(" "))
                                    peers.add(Integer.parseInt(peer));
                            info.loadChunk(peers);
                        }
                        infoMap.put(fileId, info);
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception while reading from file: " + e.toString());
            }
        }
        return infoMap;
    }

    public void addFile (String fileId, String fileName, int replicationDegree) {
        if (!this.chunkOccurrences.containsKey(fileId)) {
            this.chunkOccurrences.put(fileId, new OccurrenceInfo(fileName, replicationDegree));
            exportToFile();
        }
    }

    public void addChunkSlot(String fileId) {
        this.chunkOccurrences.get(fileId).addChunkSlot();
    }

    public void addChunkOcc(String fileId, int chunkNumber, int peerId) {
        if (this.getFileOccurrences(fileId).addChunkOccurrence(chunkNumber, peerId))
            exportToFile();
    }

    public void remChunkOcc(String fileId, int chunkNumber, int peerId) {
        if (this.getFileOccurrences(fileId).removeChunkOccurrence(chunkNumber, peerId))
            exportToFile();
    }

    public boolean checkChunkReplicationDegree(String fileId, int chunkNumber) {
        return this.getFileOccurrences(fileId).replicationDegree <= this.getChunkOccurrences(fileId, chunkNumber);
    }

    public OccurrenceInfo getFileOccurrences(String fileId) {
        return this.chunkOccurrences.get(fileId);
    }

    public int getChunkOccurrences(String fileId, int chunkNumber) {
        return this.getFileOccurrences(fileId).getOccurrenceCount(chunkNumber);
    }

    public void deleteOccurrences(String fileId) { this.chunkOccurrences.remove(fileId); }

    public String getFileName(String fileID) { return this.chunkOccurrences.get(fileID).getFileName(); }

    public int getReplicationDegree(String fileId) { return this.chunkOccurrences.get(fileId).getReplicationDegree(); }

}
