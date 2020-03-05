package request;

public abstract class Request {
    static final String BREAK = " ";
    static final int MAX_SIZE = 256;
    public String type;

    abstract public String toString();
    abstract public String[] getData();

    public static Request fromArgs(String[] args) {
        switch(args[0].toLowerCase()) {
            case "register":
                if (args.length != 3) {
                    System.out.println("\nIncorrect parameters!");
                    System.out.println("Usage: register <DNS name> <IP address>\n");
                    return null;
                }
                return new RegisterRequest(args);
            case "lookup":
                if (args.length != 2) {
                    System.out.println("\nIncorrect parameters!");
                    System.out.println("Usage: lookup <DNS name>\n");
                    return null;
                }
                return new LookupRequest(args);
            case "close":
                if (args.length != 1) {
                    System.out.println("\nIncorrect parameters!");
                    System.out.println("Usage: close\n");
                    return null;
                }
                return new CloseRequest();
            case "reset":
                if (args.length != 1) {
                    System.out.println("\nIncorrect parameters!");
                    System.out.println("Usage: reset\n");
                    return null;
                }
                return new ResetRequest();
            default:
                System.out.println("Invalid operation!");
                return null;
        }
    }
}
