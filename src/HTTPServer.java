import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;



public class HTTPServer {
    private static int port = 8080;
    private static ServerSocket serverSocket;
    
    public static void main(String[] args) throws IOException {
        serverSocket = new ServerSocket(port);
        while (true) {
            try {
                Socket s = serverSocket.accept();
                new Handler(s);
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
    
}
