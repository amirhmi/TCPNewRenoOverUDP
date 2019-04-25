import java.io.IOException;
import java.net.*;

public class Sender {
    public static void main(String[] args) throws Exception {
        TCPSocket tcpSocket = new TCPSocketImpl(Config.localhost, Config.clientPort);
        tcpSocket.send("resources/1MB.txt");
        // tcpSocket.send("sending.mp3");
        // tcpSocket.close();
        // tcpSocket.saveCongestionWindowPlot();
    }
}
