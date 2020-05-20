package storage;

public class Chunk {

    private final int num;
    private final byte[] data;
    private final String fileID;
    private final int replicationDegree;
    private int currReplDegree;
    private final int size;

    public Chunk(String fileID, int chunkNumber, byte[] data, int size, int replicationDegree) {
        this.data = data;
        this.size = size;
        this.fileID = fileID;
        this.num = chunkNumber;
        this.replicationDegree = replicationDegree;
        this.currReplDegree = replicationDegree;
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

    public int getReplicationDegree() {
        return replicationDegree;
    }

    public int getCurrReplDegree() {
        return currReplDegree;
    }

    public int getSize() {
        return size;
    }

    public void setCurrReplDegree(int currReplDegree) {
        this.currReplDegree = currReplDegree;
    }

}
