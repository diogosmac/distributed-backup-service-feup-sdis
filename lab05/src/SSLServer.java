import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

public class SSLServer {

    private static HashMap<String, String> dnsTable;

//    private List<String> cypherSuites;
    private static boolean closeRequested = false;

    private static String process(Request request) {

        String[] params = request.getData();

        switch(request.type) {
            case "register":
                if (dnsTable.containsKey(params[0])) { return "ALREADY_REGISTERED"; }
                else {
                    dnsTable.put(params[0], params[1]);
                    return Integer.toString(dnsTable.size());
                }
            case "lookup":
                return dnsTable.getOrDefault(params[0], "NOT_FOUND");
            case "close":
                closeRequested = true;
                return "CLOSED_SUCCESSFULLY";
            case "reset":
                dnsTable.clear();
                return "RESET_SUCCESSFULLY";
            default: return "ERROR";
        }

    }


    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: java SSLServer <port> <cypher-suite>*");
            return;
        }

        dnsTable = new HashMap<>();

        int port = Integer.parseInt(args[0]);
//        cypherSuites.addAll(Arrays.asList(args).subList(1, args.length));

        SSLServerSocket sslSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(port);
        System.out.println("\nServer opened at " + sslSocket.getInetAddress().getHostName() + ":" + port + '\n');

        while (!closeRequested) {

            SSLSocket socket = null;
            try {
                socket = (SSLSocket) sslSocket.accept();
            } catch (IOException e) {
                System.out.println("Accept failed: " + port);
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String received = in.readLine();

            String[] arguments = received.split(Request.BREAK);
            Request request = Request.fromArgs(arguments);
            String reply = (request != null) ? process(request) : "ERROR";

            System.out.println("SSLServer: " + received);
            out.println(reply);

            out.close();
            in.close();
            socket.close();

        }

    }
}
