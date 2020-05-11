package chord;

public class NodePair<A, B> {

    private A key;
	private B value;
	
	public NodePair(A key, B value) {
		this.key = key;
		this.value = value;
	}
	
	public NodePair(NodePair<A, B> pair) {
		this.key = pair.getKey();
		this.value = pair.getValue();
    }
    
    /**
     * @return the key
     */
    public A getKey() {
        return key;
    }

    /**
     * @return the value
     */
    public B getValue() {
        return value;
    }

    /**
     * @param key the key to set
     */
    public void setKey(A key) {
        this.key = key;
    }

    /**
     * @param value the value to set
     */
    public void setValue(B value) {
        this.value = value;
    }

	@Override
	public String toString() {
		return "[" + key + " : " + value + "]";
	}
}