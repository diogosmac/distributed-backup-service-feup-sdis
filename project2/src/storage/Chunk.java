package storage;

public class Chunk {

    private final int num;
    private final byte[] data;
    private final String fileID;
    private final int size;

    public Chunk(String fileID, int chunkNumber, byte[] data, int size) {
        this.data = data;
        this.size = size;
        this.fileID = fileID;
        this.num = chunkNumber;
    }

    public int getNum() {
        return num;
    }

    public byte[] getData() {
        return data;
    }

    public String getFileID() {
        return this.fileID;
    }

    public int getSize() {
        return size;
    }

}
