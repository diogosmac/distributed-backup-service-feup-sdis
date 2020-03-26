import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class Client {

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Usage: Client <host> <port> <oper> <opnd>*");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        Socket socket = new Socket(hostname, port);

        Request request = Request.fromArgs(
                Arrays.copyOfRange(args, 2, args.length));
        if (request == null) return;

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(request.toString());

        String response = in.readLine();
        System.out.println(request + " :: " + response);

        out.close();
        in.close();
        socket.close();

    }

}
