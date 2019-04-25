import java.net.DatagramPacket;
import java.net.InetAddress;

public class TCPServerSocketImpl extends TCPServerSocket {
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
    }

    @Override
    public TCPSocket accept() throws Exception {
        //listen
        System.out.println("handshaking::listening");
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(Config.serverPort);
        TCPSegment segment;
        byte[] segmentBytes = new byte[1024];
        DatagramPacket dp = new DatagramPacket(segmentBytes, 1024);
        eds.receive(dp);
        segment = new TCPSegment(dp.getData());
        if (segment.syn && !segment.ack)
        {
            segment = new TCPSegment(true, true, 317, segment.seqNumber + 1, new byte[0]);
            segmentBytes = segment.toBytes();
            dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.clientIP), Config.clientPort);
            eds.send(dp);
            //synAckSent
            System.out.println("handshaking::syn ack sent");
            segmentBytes = new byte[1024];
            dp = new DatagramPacket(segmentBytes, 1024);
            eds.receive(dp);
            segment = new TCPSegment(dp.getData());
            if (!segment.syn && segment.ack && segment.ackNumber == 318) {
                //established
                System.out.println("handshaking::connection established");
                eds.close();
                return new TCPSocketImpl(Config.serverIP, Config.serverPort);
            }
        }
        eds.close();
        throw new HandshakeFailureException();
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
