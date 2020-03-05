package request;

public class LookupRequest extends Request {

    private String dnsName;

    public LookupRequest(String[] args) {
        this.type = "lookup";
        this.dnsName = args[1];
    }

    public String toString() {
        return type + Request.BREAK + dnsName;
    }

    @Override
    public String[] getData() {
        String[] ret = new String[1];
        ret[0] = dnsName;
        return ret;
    }

}
