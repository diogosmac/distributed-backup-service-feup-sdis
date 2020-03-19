import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {

    private static String[] parseArguments(String[] args) {

        List<String> retList = new ArrayList<>();

        switch (args[0].toLowerCase()) {

            case "lookup":
                if (args.length != 2) {
                    System.out.println("Usage: lookup <ip_address>");
                    return null;
                }
                else {
                    retList.add(args[0].toLowerCase());
                    retList.add(args[1]);
                }
                break;

            case "register":
                if (args.length != 3) {
                    System.out.println("Usage: register <dns_name> <ip_address>");
                    return null;
                }
                else {
                    retList.add(args[0].toLowerCase());
                    retList.add(args[1]);
                    retList.add(args[2]);
                }
                break;

            case "reset":
                if (args.length != 1) {
                    System.out.println("Usage: reset");
                    return null;
                }
                else {
                    retList.add(args[0].toLowerCase());
                }
                break;

            default:
                return null;

        }

        String[] ret = new String[retList.size()];
        retList.toArray(ret);
        return ret;

    }

    public static void main(String[] args) {

        if (args.length < 3) {

            System.out.println("Usage: java Client <host name> <remote_object_name> <operation> <args>*");
            return;

        }

        String hostName = args[0];
        String remoteObjectName = args[1];

        String[] command = parseArguments(Arrays.copyOfRange(args, 2, args.length));
        if (command == null)
            return;

        try {

            Registry registry = LocateRegistry.getRegistry(hostName);
            ServerInterface server = (ServerInterface) registry.lookup(remoteObjectName);

            switch (command[0]) {

                case "lookup":
                    String lookupResponse = server.lookup(command[1]);
                    System.out.println("Lookup " + command[1] + " :: " + lookupResponse);
                    break;

                case "register":
                    String registerResponse = server.register(command[1], command[2]);
                    System.out.println("Register " + command[1] + " " + command[2] + " :: " + registerResponse);
                    break;

                case "reset":
                    String resetResponse = server.reset();
                    System.out.println("Reset :: " + resetResponse);
                    break;

            }

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
        }

    }

}
