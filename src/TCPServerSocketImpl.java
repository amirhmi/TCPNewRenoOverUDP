import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class TCPServerSocketImpl extends TCPServerSocket {
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
    }

    @Override
    public TCPSocket accept() throws Exception {
        //listen
        System.out.println("handshaking::listening");
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(Config.receiverPort);
        TCPSegment segment;
        byte[] segmentBytes;
        byte[] receivedBytes = new byte[1024];
        DatagramPacket dp = new DatagramPacket(receivedBytes, receivedBytes.length);
        eds.receive(dp);
        segment = new TCPSegment(dp);
        eds.setSoTimeout(Config.timeoutMS);
        if (segment.syn && !segment.ack)
        {
            System.out.println("handshaking::syn received");
            segment = new TCPSegment(true, true, false, 317, segment.seqNumber + 1, new byte[0]);
            segmentBytes = segment.toBytes();
            while (true) {
                dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.senderIP), Config.senderPort);
                eds.send(dp);
                //synAckSent
                System.out.println("handshaking::syn ack sent");
                try {
                    dp = new DatagramPacket(receivedBytes, receivedBytes.length);
                    eds.receive(dp);
                    segment = new TCPSegment(dp);
                    if (!segment.syn && segment.ack && segment.ackNumber == 318) {
                        //established
                        System.out.println("handshaking::ack received");
                        eds.close();
                        return new TCPSocketImpl(Config.receiverIP, Config.receiverPort);
                    }
                }
                catch (SocketTimeoutException e) { System.out.println("handshaking::ack timeout"); }
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
