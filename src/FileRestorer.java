import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class FileRestorer {

    String fileName;
    private List<byte []> fileBytes;

    public FileRestorer (String fileName) {
        this.fileName = fileName;
        this.fileBytes = new ArrayList<>();
    }

    public void addSlot() {
        this.fileBytes.add(null);
    }

    public byte[] getChunk(int chunkNumber) {
        return this.fileBytes.get(chunkNumber);
    }

    public boolean restoreFile() {

        byte [] fileData = this.fileBytes.get(0);

        for(int currentChunk = 1; currentChunk < this.fileBytes.size(); currentChunk++) {
            byte [] currentChunkData = this.fileBytes.get(currentChunk);

            if(currentChunkData == null)
                return false;
            else
                fileData = MyUtils.concatByteArrays(fileData, currentChunkData);
        }

        try {
            FileOutputStream fos = new FileOutputStream(MyUtils.DEFAULT_RESTORE_PATH + this.fileName);
            fos.write(fileData);
        } catch (Exception e) {
            System.out.println("Error while writing file");
        }

        return true;
    }

}
