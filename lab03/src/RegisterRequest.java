public class RegisterRequest extends Request {

    private String dnsName;
    private String ipAddress;

    public RegisterRequest(String[] args) {
        this.type = "register";
        this.dnsName = args[1];
        this.ipAddress = args[2];
    }

    public String toString() {
        return type +
                Request.BREAK + dnsName +
                Request.BREAK + ipAddress;
    }

    @Override
    public String[] getData() {
        String[] ret = new String[2];
        ret[0] = dnsName; ret[1] = ipAddress;
        return ret;
    }
}
