import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver {
    public static void main(String[] args) throws Exception {
        TCPServerSocket tcpServerSocket = new TCPServerSocketImpl(Config.serverPort);
        TCPSocket tcpSocket = tcpServerSocket.accept();
        tcpSocket.receive("receiving.mp3");
        // tcpSocket.close();
        // tcpServerSocket.close();
    }
}
