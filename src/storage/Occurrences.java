package storage;

import peer.Peer;
import utils.MyUtils;

import java.io.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Occurrences {

    private static class OccurrenceInfo {

        private String filePath;
        private int desiredReplicationDegree;
        private final List<Integer> perceivedReplicationDegrees;

        private OccurrenceInfo(String filePath, int desired) {
            this.filePath = filePath;
            this.desiredReplicationDegree = desired;
            this.perceivedReplicationDegrees = new ArrayList<>();
        }

        private void reset(int desired) {
            this.desiredReplicationDegree = desired;
            this.perceivedReplicationDegrees.clear();
        }

        private void updateReplicationDegree(int chunkNumber, int missing) {
            this.perceivedReplicationDegrees.set(chunkNumber, desiredReplicationDegree - missing);
        }

        private List<Integer> getPerceivedReplicationDegrees() {
            return this.perceivedReplicationDegrees;
        }

        private int getDesiredReplicationDegree() {
            return this.desiredReplicationDegree;
        }

        public void addChunkSlot(int chunkNumber) {
            while (perceivedReplicationDegrees.size() <= chunkNumber) {
                perceivedReplicationDegrees.add(0);
            }
        }

        private void loadChunkInfo(int chunkNumber, int perceived) {
            addChunkSlot(chunkNumber);
            this.perceivedReplicationDegrees.set(chunkNumber, perceived);
        }

        private void incrementPerceivedReplicationDegree(int chunkNumber, int delta) {
            int newDegree = this.perceivedReplicationDegrees.get(chunkNumber) + delta;
            this.perceivedReplicationDegrees.set(chunkNumber, newDegree);
        }

        private boolean isReplicationDegreeMet(int chunkNumber) {
            int desired = this.getDesiredReplicationDegree();
            return this.getPerceivedReplicationDegrees().get(chunkNumber) >= desired;
        }

    }

    private final String statusFilePath;

    // Key:     <file-id>
    // Value:   occurrence-counts
    private final ConcurrentHashMap<String, OccurrenceInfo> occurrenceTable;

    public Occurrences(Peer peer) {
        this.statusFilePath = MyUtils.getStatusPath(peer);
        this.occurrenceTable = this.loadFromFile();
    }

    private void exportToFile() {
        File file = new File(this.statusFilePath);
        if (file.getParentFile().mkdirs()) {
            String baseDir = this.statusFilePath.substring(0, this.statusFilePath.lastIndexOf('/'));
            System.out.println("Created new " + baseDir + " directory!");
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (Map.Entry<String, OccurrenceInfo> entry : occurrenceTable.entrySet()) {
                String fileID = entry.getKey();
                OccurrenceInfo info = entry.getValue();
                StringBuilder fileOutput = new StringBuilder(
                        "ID: " + fileID + " - " +
                        "Name: " + info.filePath + " - " +
                        "Desired Replication Degree: " + info.desiredReplicationDegree + "\n"
                );
                List<Integer> chunkOccurrences = info.getPerceivedReplicationDegrees();
                for (int chunkNumber = 0; chunkNumber < chunkOccurrences.size(); chunkNumber++) {
                    fileOutput.append(chunkNumber).append(" - ").append(chunkOccurrences.get(chunkNumber)).append('\n');
                }
                fileOutput.append('\n');
                bw.write(fileOutput.toString());
            }
            bw.close();
        } catch (Exception e) { System.out.println("Exception while writing occurrences to file: " + e.toString()); }

    }

    private ConcurrentHashMap<String, OccurrenceInfo> loadFromFile() {

        ConcurrentHashMap<String, OccurrenceInfo> occurrenceTable = new ConcurrentHashMap<>();

        File file = new File(this.statusFilePath);
        if (file.exists()) {

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {

                    if (line.substring(0, line.indexOf(':') + 1).equals("ID:")) {

                        String fileID = line.substring(
                                4, line.indexOf(" - Name: "));

                        String filePath = line.substring(
                                line.indexOf("Name: ") + 6, line.indexOf(" - Desired Replication Degree: "));

                        int desired = Integer.parseInt(
                                line.substring(line.lastIndexOf(':') + 2));

                        OccurrenceInfo info = new OccurrenceInfo(filePath, desired);

                        while (!(line=br.readLine()).equals("")) {

                            int chunkNumber = Integer.parseInt(
                                    line.substring(0, line.indexOf(" - ")));

                            int perceived = Integer.parseInt(
                                    line.substring(line.indexOf(" - ") + 3));

                            info.loadChunkInfo(chunkNumber, perceived);
                        }
                        occurrenceTable.put(fileID, info);
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception while reading from file: " + e.toString());
            }
        }

        return occurrenceTable;
    }

    public boolean hasFile(String fileID) {
        return this.occurrenceTable.containsKey(fileID);
    }

    public synchronized void addFile(String fileID, String fileName, int desiredReplicationDegree) {
        this.occurrenceTable.put(fileID, new OccurrenceInfo(fileName, desiredReplicationDegree));
        exportToFile();
    }

    public synchronized void updateFileChunk(String fileID, int chunkNumber, int missing) {
        addChunkSlot(fileID, chunkNumber);
        this.occurrenceTable.get(fileID).updateReplicationDegree(chunkNumber, missing);
        exportToFile();
    }

    public synchronized void addChunkSlot(String fileID, int chunkNumber) {
        this.occurrenceTable.get(fileID).addChunkSlot(chunkNumber);
    }

    public synchronized void deleteFile(String fileID) {
        this.occurrenceTable.remove(fileID);
        exportToFile();
    }

    public void resetFile(String fileID, int desiredReplicationDegree) {
        this.occurrenceTable.get(fileID).reset(desiredReplicationDegree);
    }

    public void incrementReplicationDegree(String fileID, int chunkNumber, int delta) {
        OccurrenceInfo oi = this.occurrenceTable.get(fileID);
        oi.incrementPerceivedReplicationDegree(chunkNumber, delta);
        exportToFile();
    }

    public boolean isReplicationDegreeMet(String fileID, int chunkNumber) {
        OccurrenceInfo oi = this.occurrenceTable.get(fileID);
        return oi.isReplicationDegreeMet(chunkNumber);
    }

    public String getOccurrencesInfo() {

        String sectionHeader = "-- Backed up files Section --\n";
        String sectionFooter = "|\n-----------------------------";
        StringBuilder infoBody = new StringBuilder();

        for (Map.Entry<String, OccurrenceInfo> entry : this.occurrenceTable.entrySet()) {
            String fileID = entry.getKey();
            OccurrenceInfo info = entry.getValue();
            String filePath = info.filePath;
            if (filePath.equals(""))
                continue;
            int desiredReplicationDegree = info.desiredReplicationDegree;

            infoBody.append("|\n");
            infoBody.append("|\tFile Path: ").append(filePath).append("\n");
            infoBody.append("|\tFile ID (for the Backup Service): ").append(fileID).append("\n");
            infoBody.append("|\tDesired Replication Degree: ").append(desiredReplicationDegree).append("\n");

            List<Integer> chunkOccurrences = info.getPerceivedReplicationDegrees();
            for (int chunk = 0; chunk < chunkOccurrences.size(); chunk++) {
                infoBody.append("|\t\tChunk #").append(chunk)
                        .append(" - Perceived Replication Degree: ")
                        .append(chunkOccurrences.get(chunk)).append('\n');
            }

        }

        return sectionHeader + infoBody + sectionFooter + "\n\n";

    }

}
