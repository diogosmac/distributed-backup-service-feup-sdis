import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileRestorer {

    String path;
    private List<byte []> fileBytes;

    public FileRestorer (String path) {
        this.path = path;
        this.fileBytes = new ArrayList<>();
    }

    public void addSlot() {
        this.fileBytes.add(null);
    }

    public byte[] getChunkData(int chunkNumber) {
        return this.fileBytes.get(chunkNumber);
    }

    public void saveData(int chunkNumber, byte[] data) {
        this.fileBytes.set(chunkNumber, data);
    }

    public boolean restoreFile() {

        byte[] fileData = this.fileBytes.get(0);

        for (int currentChunk = 1; currentChunk < this.fileBytes.size(); currentChunk++) {
            byte[] currentChunkData = this.fileBytes.get(currentChunk);

            if (currentChunkData == null)
                return false;
            else
                fileData = MyUtils.concatByteArrays(fileData, currentChunkData);
        }

        try {
            File file = new File(this.path);
            if (file.getParentFile().mkdirs()) {
                System.out.println("\tCreated missing ./restored directory.");
            }
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.write(fileData);
            fos.close();
        } catch (Exception e) {
            System.out.println("\tError while writing file!");
        }

        return true;
    }

}
