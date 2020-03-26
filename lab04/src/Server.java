import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private final static int OK = 0;
    private final static int ERROR = -1;

    private static HashMap<String, String> dnsTable;
    private static AtomicBoolean closeRequested = new AtomicBoolean(false);

    private static ServerSocket serverSocket;

    private static int openSocket(int port) {

        serverSocket = null;

        try { serverSocket = new ServerSocket(port); }
        catch (IOException e) {
            return ERROR;
        }

        System.out.println("\nServer opened:");
        System.out.println("\tIP Address: " + serverSocket.getInetAddress().getHostAddress());
        System.out.println("\tPort: " + serverSocket.getLocalPort() + "\n");

        return OK;

    }

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
                closeRequested.set(true);
                return "CLOSED_SUCCESSFULLY";
            case "reset":
                dnsTable.clear();
                return "RESET_SUCCESSFULLY";
            default: return "ERROR";
        }

    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Usage: java Server <port number>");
            return;
        }

        dnsTable = new HashMap<String, String>();

        int port = Integer.parseInt(args[0]);
        if (openSocket(port) != OK) {
            System.out.println("An error occurred while creating the server");
            return;
        }

        while (!closeRequested.get()) {

            System.out.println("\nWaiting for request . . .\n");

            Socket socket;
            try { socket = serverSocket.accept(); }
            catch (IOException e) {
                System.out.println("Accept failed: " + port);
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String received = in.readLine();

            String[] arguments = received.split(Request.BREAK);
            Request request = Request.fromArgs(arguments);
            String reply = (request != null) ? process(request) : "ERROR";

            System.out.println(received + " :: " + reply);
            out.println(reply);

            out.close();
            in.close();
            socket.close();

        }

        System.out.println("Closing server socket ...");
        serverSocket.close();

        System.out.println("Server closed successfully!");

    }

}
