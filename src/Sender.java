import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocket tcpSocket = new TCPSocketImpl(Config.localhost, Config.senderPort);
        tcpSocket.send("resources/1MB.txt");
        tcpSocket.close();
        // tcpSocket.saveCongestionWindowPlot();
    }
}
