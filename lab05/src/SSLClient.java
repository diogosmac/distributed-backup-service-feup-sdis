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

        // Keystore
        System.setProperty("javax.net.ssl.keyStore", "keys/client.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        // Truststore
        System.setProperty("javax.net.ssl.trustStore", "keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostname, port);

        int n_arguments_before_cypher;

        switch (args[2]) {
            case "register":
                n_arguments_before_cypher = 5;
                break;
            case "lookup":
                n_arguments_before_cypher = 4;
                break;
            default:
                n_arguments_before_cypher = 3;
                break;
        }

        Request request = Request.fromArgs(Arrays.copyOfRange(args, 2, n_arguments_before_cypher));
        if (request == null) return;


        if (args.length > n_arguments_before_cypher) {
            String [] cyphers = Arrays.copyOfRange(args, n_arguments_before_cypher, args.length);
            sslSocket.setEnabledCipherSuites(cyphers);
        }

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
