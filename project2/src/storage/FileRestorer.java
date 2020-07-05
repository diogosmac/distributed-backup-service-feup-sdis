package storage;

import utils.MyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class FileRestorer {

    final String path;
    private final ConcurrentHashMap<String, List<byte[]>> fileData;
    private final ConcurrentHashMap<String, CountDownLatch> fileRestorationStatus;

    public FileRestorer(String path) {
        this.path = path;
        this.fileData = new ConcurrentHashMap<>();
        this.fileRestorationStatus = new ConcurrentHashMap<>();
    }

    public void addFile(String fileId, int numberChunks) {
        this.fileData.put(fileId, new ArrayList<>());
        for (int i = 0; i < numberChunks; i++) {
            addSlot(fileId);
        }
        this.fileRestorationStatus.put(fileId, new CountDownLatch(numberChunks));
    }

    public void addSlot(String fileId) {
        this.fileData.get(fileId).add(null);
    }

    public void saveRestoredChunk(String fileId, int chunkNumber, byte[] data) {
        this.fileData.get(fileId).set(chunkNumber, data);
        this.fileRestorationStatus.get(fileId).countDown();
    }

    public CountDownLatch getFileRestorationStatus(String fileID) {
        return this.fileRestorationStatus.get(fileID);
    }

    public boolean restoreFile(String fileId, String fileName) {

        List<byte[]> fileDataLocal = this.fileData.get(fileId);
        byte[] concatData = fileDataLocal.get(0);

        for (int currentChunk = 1; currentChunk < fileDataLocal.size(); currentChunk++) {
            byte[] currentChunkData = fileDataLocal.get(currentChunk);

            if (currentChunkData == null)
                return false;
            else
                concatData = MyUtils.concatByteArrays(concatData, currentChunkData);
        }

        try {
            File file = new File(this.path + fileName);

            if (file.getParentFile().mkdirs()) {
                System.out.println("\tCreated missing ./restored directory.");
            }

            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(concatData);
            fos.close();
        } catch (Exception e) {
            System.out.println("\tError while writing file!");
            return false;
        }

        return true;
    }

}
