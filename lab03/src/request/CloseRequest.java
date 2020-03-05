package request;

public class CloseRequest extends Request {
    public CloseRequest() { this.type = "close"; }
    public String toString() { return type; }
    @Override
    public String[] getData() { return new String[0]; }
}
