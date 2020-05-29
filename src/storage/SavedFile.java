package storage;

import utils.MyUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class SavedFile implements java.io.Serializable {

    private final String id;
    private final File file;
    private final ArrayList<Chunk> chunks;

    public SavedFile(String filePath) {
        this.file = new File(filePath);
        this.chunks = new ArrayList<>();
        this.id = MyUtils.encryptFileID(filePath);
        this.splitIntoChunks();
    }

    public static int getNumChunks(String filePath) {
        File file = new File(filePath);
        long size = file.length();
        int counter = 0;
        while (size > 0) {
            size -= MyUtils.CHUNK_SIZE;
            counter++;
        }
        return counter;
    }

    public String getId() {
        return id;
    }

    public ArrayList<Chunk> getChunks() {
        return chunks;
    }

    private void splitIntoChunks() {

        int chunkCounter = 0;
        byte[] buffer = new byte[MyUtils.CHUNK_SIZE];

        try (FileInputStream fis = new FileInputStream(this.file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int numBytes;
            while ((numBytes = bis.read(buffer, 0, MyUtils.CHUNK_SIZE)) > 0) {
                byte[] body = Arrays.copyOf(buffer, numBytes);

                Chunk chunk = new Chunk(this.id, chunkCounter, body, numBytes);
                chunkCounter++;
                this.chunks.add(chunk);
                buffer = new byte[MyUtils.CHUNK_SIZE];
            }

            if (this.file.length() % MyUtils.CHUNK_SIZE == 0) {
                this.chunks.add(new Chunk(this.id, chunkCounter, new byte[0], 0));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
