public class Chunk {

    private final int num;
    private final byte[] data;
    private final String fileID;
    private final int replicationDegree;
    private int currReplDegree = 0;
    private final int size;

    public Chunk(String fileID, int num, byte[] data, int size, int replicationDegree) {
        this.num = num;
        this.data = data;
        this.size = size;
        this.fileID = fileID;
        this.replicationDegree = replicationDegree;
        this.currReplDegree = 1;
    }

    public int getNum() {
        return num;
    }

    public byte[] getData() {
        return data;
    }

    public String getFileID() {
        return fileID;
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
