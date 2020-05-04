import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;

public class SSLClient {

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.out.println("Usage: java SSLClient <host> <port> <oper> <opnd>* <cypher-suite>*");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostname, port);

        Request request = Request.fromArgs(Arrays.copyOfRange(args, 2, args.length));
        if (request == null) return;

        PrintWriter out = new PrintWriter(sslSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

        out.println(request.toString());
        String response = in.readLine();
        System.out.println("SSLClient: " + request.toString() + " : " + response);

        out.close();
        in.close();
        sslSocket.close();

    }

}
