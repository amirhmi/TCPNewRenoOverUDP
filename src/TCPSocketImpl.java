import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TCPSocketImpl extends TCPSocket {
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        establishClientConnection();
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        DatagramPacket dp = new DatagramPacket(pathToFile.getBytes(), pathToFile.length(), InetAddress.getByName(Config.serverIP), Config.serverPort);
        eds.send(dp);
        eds.close();
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        byte[] buf = new byte[1024];
        DatagramPacket dp = new DatagramPacket(buf, 1024);
        eds.receive(dp);
        if(dp.getLength() > 0)
            System.out.println(new String(dp.getData(), 0, dp.getLength()));
         eds.close();
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }

    private void establishClientConnection() throws Exception{
        //idle
        System.out.println("handshaking::started");
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        TCPSegment segment = new TCPSegment(true, false, 505, 0, new byte[0]);
        byte[] segmentBytes = segment.toBytes();
        DatagramPacket dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.serverIP), Config.serverPort);
        eds.send(dp);
        //synSent;
        System.out.println("handshaking::syn sent");
        segmentBytes = new byte[1024];
        dp = new DatagramPacket(segmentBytes, 1024);
        eds.receive(dp);
        segment = new TCPSegment(dp.getData());
        if (segment.ack && segment.syn && segment.ackNumber == 506)
        {
            segment = new TCPSegment(false, true, 1222, segment.seqNumber + 1, new byte[0]);
            segmentBytes = segment.toBytes();
            dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.serverIP), Config.serverPort);
            eds.send(dp);
            //established
            System.out.println("handshaking::connection established");
            eds.close();
            return;
        }
        eds.close();
        throw new HandshakeFailureException();
    }


    private enum SenderState {}

    private enum RecieverState {}
}
