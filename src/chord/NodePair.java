package chord;

/**
 * Node Pair
 * <p>
 * This class is to be used to map a node identifier in the chord network
 * to the node's actual address.
 *
 * @param <A> Key Object
 * @param <B> Value Object
 */
public class NodePair<A, B> {

    /**
     * Key Object
     */
    private A key;

    /**
     * Value Object
     */
    private B value;

    /**
     * Default constructor
     *
     * @param key   the key object
     * @param value the value object
     */
    public NodePair(A key, B value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Copy constructor
     *
     * @param pair NodePair to be copied
     */
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

    /**
     * toString() method
     */
    @Override
    public String toString() {
        return "[" + key + " : " + value + "]";
    }
}