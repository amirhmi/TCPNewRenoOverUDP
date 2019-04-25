import java.awt.desktop.SystemSleepEvent;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TCPSocketImpl extends TCPSocket {
    private long cwnd;
    private long ssthresh;
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        establishSenderConnection();
        List<TCPSegment> segments = Util.breakToSegment(pathToFile);
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        TCPSegment segment = new TCPSegment(false, false, false, 1, 0, "salam\n".getBytes());
        DatagramPacket dp = new DatagramPacket(segment.toBytes(), segment.toBytes().length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
        eds.send(dp);
        segment = new TCPSegment(false, false, false, 0, 0, "aleyk\n".getBytes());
        dp = new DatagramPacket(segment.toBytes(), segment.toBytes().length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
        eds.send(dp);
        eds.close();
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        byte[] segmentBytes = new byte[1024];
        DatagramPacket dp = new DatagramPacket(segmentBytes, 1024);
        List<TCPSegment> window = new ArrayList<>();
        int lastReceived = -1;
        Util.addToFile(pathToFile, new byte[0], false);
        TCPSegment segment;
        while(true) {
            eds.receive(dp);
            if (dp.getLength() > 0) {
                //System.out.println(new String(dp.getData(), 0, dp.getLength()));
                segment = new TCPSegment(new String(dp.getData(), 0, dp.getLength()).getBytes());
                if(segment.fin && !segment.ack && !segment.syn)
                    break;
                else if (!segment.fin && !segment.ack && !segment.syn)
                {
                    int pos = 0;
                    for (TCPSegment w : window)
                        if (w.seqNumber < segment.seqNumber)
                            pos++;
                        else
                            break;
                    window.add(pos, segment);
                    while (window.size() > 0 && window.get(0).seqNumber == lastReceived + 1)
                    {
                        lastReceived++;
                        Util.addToFile(pathToFile, window.remove(0).payload, true);
                    }
                }
            }
        }
        System.out.println("close::fin received");
        segment = new TCPSegment(false, true, true, 284, segment.seqNumber + 1, new byte[0]);
        segmentBytes = segment.toBytes();
        dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.senderIP), Config.senderPort);
        for (int i = 0; i < Config.sendRepeat; i++)
            eds.send(dp);
        System.out.println("close::finack sent");
        eds.close();
    }

    @Override
    public void close() throws Exception {
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        TCPSegment segment = new TCPSegment(false, false, true, 342, 0, new byte[0]);
        byte[] segmentBytes = segment.toBytes();
        DatagramPacket dp;
        eds.setSoTimeout(Config.timeoutMS);
        while (true) {
            dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
            eds.send(dp);
            System.out.println("close::fin sent");
            try {
                eds.receive(dp);
                segment = new TCPSegment(dp.getData());
                if (segment.fin && segment.ack && segment.ackNumber == 343)
                    break;
            } catch (SocketTimeoutException e) { System.out.println("close::receiving finack timeout");}
        }
        System.out.println("close::finack received");
        eds.close();
    }

    @Override
    public long getSSThreshold() {
        return ssthresh;
    }

    @Override
    public long getWindowSize() {
        return cwnd;
    }

    private void establishSenderConnection() throws Exception{
        //idle
        System.out.println("handshaking::started");
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        TCPSegment segment = new TCPSegment(true, false, false, 505, 0, new byte[0]);
        byte[] segmentBytes = segment.toBytes();
        DatagramPacket dp;
        eds.setSoTimeout(Config.timeoutMS);
        while (true) {
            dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
            eds.send(dp);
            //synSent;
            System.out.println("handshaking::syn sent");
            try {
                eds.receive(dp);
                segment = new TCPSegment(dp.getData());
                if (segment.ack && segment.syn && segment.ackNumber == 506) {
                    System.out.println("handshaking::synack received");
                    segment = new TCPSegment(false, true, false, 1222, segment.seqNumber + 1, new byte[0]);
                    segmentBytes = segment.toBytes();
                    dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
                    for (int i = 0; i < Config.sendRepeat; i++)
                        eds.send(dp);
                    //established
                    System.out.println("handshaking::ack sent");
                    eds.close();
                    return;
                }
            }
            catch (SocketTimeoutException e) { System.out.println("handshaking::receiving synack timeout"); }
        }
    }


    private enum SenderState {}

    private enum RecieverState {}
}
