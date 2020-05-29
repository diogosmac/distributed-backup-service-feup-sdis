package storage;

import chord.Utils;
import peer.Peer;
import utils.MyUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkStorage {

    // Key:     file_id
    // Value:   List<chunk_num>
    private final ConcurrentHashMap<String, List<Integer>> fileChunks;

    // Key:     <file_id>_<chunk_number>
    // Value:   List<backup_initiator>
    private final ConcurrentHashMap<String, List<InetSocketAddress>> chunkStorage;

    private final Peer peer;
    private final String dirPath;
    private final String infoPath;
    private long availableMemory;

    public ChunkStorage(Peer peer) {
        this.fileChunks = new ConcurrentHashMap<>();
        this.chunkStorage = new ConcurrentHashMap<>();
        this.peer = peer;
        this.availableMemory = MyUtils.PEER_MAX_MEMORY_USE;
        this.dirPath = MyUtils.getBackupPath(peer);
        this.infoPath = MyUtils.getChunkInfoPath(peer);
        this.loadChunks();
    }

    private synchronized void loadChunks() {
        int i = 0;
        File dir = new File(dirPath);
        HashMap<String, List<InetSocketAddress>> initiators = this.loadChunkInfo();
        File[] dirListing = dir.listFiles();
        if (dirListing != null) {
            for (File file : dirListing) {
                String fileName = file.getName();

                String fileId = fileName.substring(0, fileName.lastIndexOf("_"));
                int chunkNumber = Integer.parseInt(fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf(".")));
                String storageKey = fileId + "_" + chunkNumber;

                this.availableMemory -= file.length();

                if (!this.chunkStorage.containsKey(storageKey))
                    this.chunkStorage.put(storageKey, new ArrayList<>());

                for (InetSocketAddress currentInitiator : initiators.get(storageKey))
                    this.chunkStorage.get(storageKey).add(currentInitiator);

                if (!this.fileChunks.containsKey(fileId))
                    this.fileChunks.put(fileId, new ArrayList<>());

                this.fileChunks.get(fileId).add(chunkNumber);
                i++;
            }
        }

        if (i != 0)
            System.out.println("\n\tFound " + i + " files in memory!\n");
        else System.out.println("\n\tNo files found in memory!\n");

        if (this.availableMemory < 0) {
            System.out.println("\tWARNING: Using " + (-0.001*this.availableMemory) + " KB over the memory limit!\n");
        }
    }

    private HashMap<String, List<InetSocketAddress>> loadChunkInfo() {
        HashMap<String, List<InetSocketAddress>> res = null;
        File infoFile = new File(this.infoPath);
        if (infoFile.exists()) {
            res = new HashMap<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(infoFile));
                String line;
                while ((line = br.readLine()) != null) {
                    // <fildId> <chunkNumber> <InitiatorIp> <InitiatorPort>
                    String[] info = line.split(" ");
                    String key = info[0] + "_" + info[1];

                    if (!res.containsKey(key)) {
                        res.put(key, new ArrayList<>());
                    }

                    InetSocketAddress initiator = new InetSocketAddress(info[2], Integer.parseInt(info[3]));

                    res.get(key).add(initiator);
                }

            } catch (Exception e) {
                System.out.println("Exception while loading chunk info from file: " + e.toString());
            }
        }

        return res;
    }

    private void exportChunkInfo() {
        File file = new File(infoPath);
        if (file.getParentFile().mkdirs()) {
            String baseDir = infoPath.substring(0, infoPath.lastIndexOf('/'));
            System.out.println("Created new " + baseDir + " directory!");
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                StringBuilder toWrite = new StringBuilder();
                for (Map.Entry<String, List<Integer>> entry : fileChunks.entrySet()) {
                    String fileID = entry.getKey();
                    List<Integer> chunkNumbers = entry.getValue();
                    for (Integer currentChunkNumber : chunkNumbers) {
                        List<InetSocketAddress> initiators = this.chunkStorage.get(fileID + "_" + currentChunkNumber);
                        for (InetSocketAddress currentInitiator : initiators) {
                            StringBuilder sb = new StringBuilder();
                            String address = currentInitiator.getHostString();
                            int port = currentInitiator.getPort();

                            sb.append(fileID).append(" ");
                            sb.append(currentChunkNumber).append(" ");
                            sb.append(address).append(" ");
                            sb.append(port).append("\n");

                            toWrite.append(sb.toString());
                        }
                    }
                    bw.write(toWrite.toString());
                }
            bw.close();
        } catch (Exception e) { System.out.println("Exception while exporting chunk info to file: " + e.toString()); }
    }

    public synchronized int addChunk(Chunk chunk, InetSocketAddress initiator) {
        if (chunk.getSize() > this.availableMemory)
            return 1;

        String fileID = chunk.getFileID();
        int chunkNumber = chunk.getNum();

        if (!this.fileChunks.containsKey(fileID)) {
            this.fileChunks.put(fileID, new ArrayList<>());
            this.fileChunks.get(fileID).add(chunkNumber);
        }
        else {
            List<Integer> presentChunks = this.fileChunks.get(fileID);
            if (!presentChunks.contains(chunkNumber))
                presentChunks.add(chunkNumber);
        }

        String chunkKey = buildChunkKey(fileID, chunkNumber);

        if (this.chunkStorage.containsKey(chunkKey)) {
            List<InetSocketAddress> initiators = this.chunkStorage.get(chunkKey);
            if (!initiators.contains(initiator)) {
                initiators.add(initiator);
            }
            return 0;
        }

        this.chunkStorage.put(chunkKey, new ArrayList<>());
        String fileName = chunkKey + MyUtils.CHUNK_FILE_EXTENSION;
        try {
            File file = new File(dirPath + fileName);
            if (file.getParentFile().mkdirs()) {
                System.out.println("\tCreated missing ./backup directory.");
            }
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(chunk.getData());
            fos.close();
            this.chunkStorage.get(chunkKey).add(initiator);
            this.availableMemory -= chunk.getSize();
            System.out.println("\t\tStored chunk #" + chunkNumber + ": " + chunk.getSize() + " bytes");
        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        }

        exportChunkInfo();
        return 0;
    }

    public synchronized Chunk getChunk(String fileID, int chunkNumber) {

        String chunkKey = buildChunkKey(fileID, chunkNumber);
        if (!this.chunkStorage.containsKey(chunkKey))
            return null;

        String filePath = chunkKey + MyUtils.CHUNK_FILE_EXTENSION;
        File file = new File(dirPath + filePath);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[MyUtils.CHUNK_SIZE];
            int size = fis.read(buffer);
            fis.close();
            return new Chunk(fileID, chunkNumber, buffer, size);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public synchronized boolean hasChunk(String fileID, int chunkNumber) {
        String chunkKey = buildChunkKey(fileID, chunkNumber);
        return this.chunkStorage.containsKey(chunkKey);
    }

    public synchronized void deleteChunk(String fileID, int chunkNumber, boolean cleanDataStructure) {

        String chunkKey = buildChunkKey(fileID, chunkNumber);
        String filePath = chunkKey + MyUtils.CHUNK_FILE_EXTENSION;

        File file = new File(dirPath + filePath);

        System.out.println("File Path: " + dirPath + filePath);

        long fileSize = file.length();

        if (file.delete()) {
            System.out.println("Chunk Deleted");
            this.availableMemory += fileSize;
            this.chunkStorage.remove(chunkKey);
            if (cleanDataStructure) {
                this.fileChunks.get(fileID).remove((Integer) chunkNumber);
            }
        }
        else {
            System.out.println("ERROR: Couldn't delete chunk #" + chunkNumber + " of file with ID " + fileID);
        }

    }

    public synchronized boolean deleteFile(String fileID) {

        if (!this.fileChunks.containsKey(fileID))
            return true;

        List<Integer> listChunks = this.fileChunks.get(fileID);
        System.out.println("Number chunks to delete:" + listChunks.size());
        System.out.println(listChunks);

        for (int chunkNum : listChunks) {
            deleteChunk(fileID, chunkNum, false);
        }

        this.fileChunks.remove(fileID);
        return true;
    }

    private String buildRemovedMessage(String fileID, int chunkNumber) {
        // REMOVED <file-id> <chunk-num>
        return String.join(" ", "REMOVED", fileID, Integer.toString(chunkNumber));
    }

    public synchronized int reclaimSpace(int amountOfSpace) {

        int spaceInBytes = amountOfSpace * 1000;

        int freedSpace = 0;

        for (Map.Entry<String, List<InetSocketAddress>> entry : this.chunkStorage.entrySet()) {

            String chunkKey = entry.getKey();
            String filePath = chunkKey + MyUtils.CHUNK_FILE_EXTENSION;
            File file = new File(dirPath + filePath);
            long fileSize = file.length();

            List<InetSocketAddress> initiators = entry.getValue();

            if (file.delete()) {

                String fileID = chunkKey.substring(0, filePath.lastIndexOf("_"));
                int chunkNumber = Integer.parseInt(chunkKey.substring(chunkKey.lastIndexOf("_") + 1));
                String removedMessage = buildRemovedMessage(fileID, chunkNumber);


                System.out.println("Deleted chunk backup initiated by:");
                for (InetSocketAddress initiator : initiators) {
                    this.peer.getNode().sendMessage(initiator, removedMessage);

                    // Places a wall, so that the node doesn't store the chunk that was removed due to a reclaim
                    // Hash: <initiatorIp> <initiatorPort> <fileID> <chunkNumber>
                    Integer hash = Utils.hash(initiator.getHostString() + " " + initiator.getPort() + " " + fileID + " " + chunkNumber);
                    this.peer.getNode().placeWall(hash.toString());

                    System.out.println("\t" + initiator.getHostString() + ":" + initiator.getPort());
                }

                this.chunkStorage.remove(chunkKey);
                this.availableMemory += fileSize;
                freedSpace += fileSize;
                if (MyUtils.PEER_MAX_MEMORY_USE - this.availableMemory <= spaceInBytes)
                    return freedSpace;

            }

        }

        return freedSpace;

    }

    public String getMemoryInfo() {

        String sectionHeader = "-- Peer Storage Section --\n|\n";
        String sectionFooter = "\n|\n--------------------------";

        long max = MyUtils.PEER_MAX_MEMORY_USE;
        long used = max - this.availableMemory;
        int usePercentage = (int) (used * 100 / max);
        int freePercentage = (int) (this.availableMemory * 100 / max);

        String info = String.join("\n|\t",
                "|\tNode ID: " + this.peer.getNode().getID(),
                "Storage Capacity: " + max * 0.001 + " KB",
                "Used space: " + used * 0.001 + " KB\t(approx. " + usePercentage + "%)",
                "Free space: " + this.availableMemory * 0.001 + " KB\t(approx. " + freePercentage + "%)");

        return sectionHeader + info + sectionFooter + "\n\n";

    }

    public String getChunkInfo() {

        String sectionHeader = "-- Stored Chunks Section --\n";
        String sectionFooter = "|\n---------------------------";
        StringBuilder infoBody = new StringBuilder();

        for (Map.Entry<String, List<InetSocketAddress>> entry : chunkStorage.entrySet()) {

            String chunkKey = entry.getKey();
            List<InetSocketAddress> initiators = entry.getValue();
            infoBody.append("|\n");

            String filePath = chunkKey + MyUtils.CHUNK_FILE_EXTENSION;
            File file = new File(dirPath + filePath);
            long fileSize = file.length();
            String fileId = filePath.substring(0, filePath.lastIndexOf("_"));
            int chunkNumber = Integer.parseInt(filePath.substring(
                    filePath.lastIndexOf("_") + 1,
                    filePath.indexOf(MyUtils.CHUNK_FILE_EXTENSION)));

//            int replicationDegree = this.peer.getChunkOccurrences().getChunkOccurrences(fileId, chunkNumber);

            StringBuilder chunkInfo = new StringBuilder(String.join("\n|\t",
                    "|\tChunk #" + chunkNumber + " of file with id: " + fileId,
                    "Chunk size: " + fileSize * 0.001 + " KB",
//                    "Perceived replication degree: " + replicationDegree,
                    "Backup Initiators:"));

            for (InetSocketAddress initiator : initiators) {
                chunkInfo.append("\n|\t\t").append(initiator.getHostString()).append(':').append(initiator.getPort());
            }

            infoBody.append(chunkInfo).append('\n');

        }

        return sectionHeader + infoBody + sectionFooter + "\n\n";

    }

    private String buildChunkKey(String fileID, int chunkNumber) {
        return fileID + '_' + chunkNumber;
    }

}
