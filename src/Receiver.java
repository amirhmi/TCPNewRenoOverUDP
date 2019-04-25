import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver {
    public static void main(String[] args) throws Exception {
        TCPServerSocket tcpServerSocket = new TCPServerSocketImpl(Config.receiverPort);
        TCPSocket tcpSocket = tcpServerSocket.accept();
        tcpSocket.receive("Resources/received.txt");
        // tcpSocket.close();
        // tcpServerSocket.close();
    }
}
