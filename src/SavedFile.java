import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;


public class SavedFile implements java.io.Serializable {

    private String id;
    private File file;
    private int replicationDegree;
    private ArrayList<Chunk> chunks;

    public SavedFile(String filePath) {
        this.file = new File(filePath);
        this.id = MyUtils.encryptFileID(filePath);
    }

    public SavedFile(String filePath, int replicationDegree) {
        this.file = new File(filePath);
        this.replicationDegree = replicationDegree;
        this.chunks = new ArrayList<>();
        this.id = MyUtils.encryptFileID(filePath);
        this.splitIntoChunks();
    }

    public String getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public int getReplicationDegree() {
        return replicationDegree;
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

                chunkCounter++;
                Chunk chunk = new Chunk(this.id, chunkCounter, body, numBytes, getReplicationDegree());
                this.chunks.add(chunk);
                buffer = new byte[MyUtils.CHUNK_SIZE];
            }

            if (this.file.length() % MyUtils.CHUNK_SIZE == 0) {
                this.chunks.add(new Chunk(this.id, chunkCounter, null, 0, getReplicationDegree()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
