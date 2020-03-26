public class ResetRequest extends Request {
    public ResetRequest() { this.type = "reset"; }
    public String toString() { return type; }
    @Override
    public String[] getData() { return new String[0]; }
}
