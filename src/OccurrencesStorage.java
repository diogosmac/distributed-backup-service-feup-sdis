import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OccurrencesStorage {

    private static class OccurrenceInfo {

        private String fileName;
        private int replicationDegree;

        // The INNER lists correspond to the IDs of the peers that have STORED each chunk
        // The OUTER list corresponds to the various chunks that compose a file
        private final List<List<Integer>> occurrences;

        public OccurrenceInfo(String fileName, int replicationDegree) {
            this.fileName = fileName;
            this.replicationDegree = replicationDegree;
            this.occurrences = new ArrayList<>();
            addChunkSlot();
        }

        public void addChunkSlot() { this.occurrences.add(new ArrayList<>()); }

        public void loadChunk(List<Integer> peers, int chunkNumber) {
            for (int peer : peers) {
                addChunkOccurrence(chunkNumber, peer);
            }
        }

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
        public void setFileName(String fileName) { this.fileName = fileName; }

        public int getReplicationDegree() { return this.replicationDegree; }
        public void setReplicationDegree(int replicationDegree) { this.replicationDegree = replicationDegree; }

        public int getNumChunks() { return this.occurrences.size(); }

        public List<Integer> getChunkOccurrences(int chunkNumber) { return this.occurrences.get(chunkNumber); }

        public int getOccurrenceCount(int chunkNumber) { return getChunkOccurrences(chunkNumber).size(); }

    }

    private final String statusFilePath;
    private final ConcurrentHashMap<String, OccurrenceInfo> chunkOccurrences;

    public OccurrencesStorage(Peer peer) {
        this.statusFilePath = MyUtils.getStatusPath(peer);
        this.chunkOccurrences = loadFromFile();
    }

    private void exportToFile() {
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

    private ConcurrentHashMap<String, OccurrenceInfo> loadFromFile() {
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
                            int chunkNumber = Integer.parseInt(line.substring(0, line.indexOf(" - ")));
                            List<Integer> peers = new ArrayList<>();
                            String peersStr = line.substring(line.indexOf(" - ") + 3).strip();
                            if (peersStr.length() > 0)
                                for (String peer : peersStr.split(" "))
                                    peers.add(Integer.parseInt(peer));
                            info.loadChunk(peers, chunkNumber);
                        }
                        infoMap.put(fileId, info);
                    }
                }
                br.close();
            } catch (Exception e) {
                System.out.println("Exception while reading from file: " + e.toString());
            }
        }
        return infoMap;
    }

    public void addFile (String fileId, int replicationDegree) {
        addFile(fileId, "", replicationDegree);
    }

    public void addFile (String fileId, String fileName, int replicationDegree) {
        if (!this.chunkOccurrences.containsKey(fileId)) {
            this.chunkOccurrences.put(fileId, new OccurrenceInfo(fileName, replicationDegree));
        }
        else {
            if (!fileName.equals(""))
                getFileOccurrences(fileId).setFileName(fileName);
            getFileOccurrences(fileId).setReplicationDegree(replicationDegree);
        }
        exportToFile();

    }

    public boolean hasChunkSlot(String fileId, int chunkNumber) {
        return getFileOccurrences(fileId).getNumChunks() > chunkNumber;
    }

    public void addChunkSlot(String fileId, int chunkNumber) {
        while (!hasChunkSlot(fileId, chunkNumber))
            getFileOccurrences(fileId).addChunkSlot();
    }

    public void saveChunkOccurrence(String fileId, int chunkNumber, int peerId) {
        if (this.getFileOccurrences(fileId).addChunkOccurrence(chunkNumber, peerId))
            exportToFile();
    }

    public void saveChunkDeletion(String fileId, int chunkNumber, int peerId) {
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

    public void deleteOccurrences(String fileId) {
        this.chunkOccurrences.remove(fileId);
        exportToFile();
    }

    public int getReplicationDegree(String fileId) { return this.chunkOccurrences.get(fileId).getReplicationDegree(); }

    public String getOccurrencesInfo() {

        String sectionHeader = "-- Backed up files Section --\n";
        String sectionFooter = "|\n-----------------------------";
        StringBuilder infoBody = new StringBuilder();

        for (Map.Entry<String, OccurrenceInfo> entry : this.chunkOccurrences.entrySet()) {

            OccurrenceInfo oi = entry.getValue();
            String fileName = oi.getFileName();
            if (fileName.equals(""))
                continue;
            String fileId = entry.getKey();
            int replicationDegree = oi.getReplicationDegree();

            infoBody.append("|\n");
            infoBody.append("|\tFile Name: ").append(fileName).append("\n");
            infoBody.append("|\tFile ID (for the Backup Service): ").append(fileId).append("\n");
            infoBody.append("|\tDesired Replication Degree: ").append(replicationDegree).append("\n");

            for (int chunk = 0; chunk < oi.getNumChunks(); chunk++) {
                infoBody.append("|\t\tChunk #").append(chunk).append(
                        " - Perceived Replication Degree: ").append(oi.getOccurrenceCount(chunk)).append('\n');
            }

        }

        return sectionHeader + infoBody + sectionFooter + "\n\n";

    }

}
