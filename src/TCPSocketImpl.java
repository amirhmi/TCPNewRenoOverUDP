import java.awt.desktop.SystemSleepEvent;
import java.net.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
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

        cwnd = Config.defaultCWND;
        ssthresh = Config.defaultSSTHRESH;
        List<TCPSegment> segments = Util.breakToSegment(pathToFile);
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        eds.setSoTimeout(Config.timeoutMS);
        List<TCPSegment> window = new ArrayList<>();

        while(segments.size() > 0 || window.size() > 0)
        {
            while (window.size() < cwnd && segments.size() > 0) {
                sendSegment(segments.get(0), eds);
                segments.remove(0);
                //window.add(segments.remove(0));
            }
            byte[] buf = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            //eds.receive(dp);
        }

        eds.close();
    }

    private void sendSegment(TCPSegment segment, EnhancedDatagramSocket eds) throws Exception
    {
        byte[] segmentBytes = segment.toBytes();
        DatagramPacket dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
        eds.send(dp);
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        byte[] receivedBytes = new byte[1024];
        DatagramPacket dp;
        List<TCPSegment> window = new ArrayList<>();
        int lastReceived = -1;
        Util.addToFile(pathToFile, new byte[0], false);
        TCPSegment segment;
        while(true) {
            dp = new DatagramPacket(receivedBytes, receivedBytes.length);
            eds.receive(dp);
            if (dp.getLength() > 0) {
                //System.out.println(new String(dp.getData(), 0, dp.getLength()));
                segment = new TCPSegment(dp);
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
                    sendAck(eds, lastReceived);
                }
            }
        }
        System.out.println("close::fin received");
        byte[] segmentBytes;
        segment = new TCPSegment(false, true, true, 284, segment.seqNumber + 1, new byte[0]);
        segmentBytes = segment.toBytes();
        dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.senderIP), Config.senderPort);
        for (int i = 0; i < Config.sendRepeat; i++)
            eds.send(dp);
        System.out.println("close::finack sent");
        eds.close();
    }

    public void sendAck (EnhancedDatagramSocket eds, int ackNumber) throws Exception
    {
        TCPSegment segment = new TCPSegment(false, true, false, 0, ackNumber, new byte[0]);
        byte[] segmentBytes = segment.toBytes();
        DatagramPacket dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.senderIP), Config.senderPort);
        eds.send(dp);
    }

    @Override
    public void close() throws Exception {
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        TCPSegment segment = new TCPSegment(false, false, true, 342, 0, new byte[0]);
        byte[] segmentBytes = segment.toBytes();
        byte[] receivedBytes = new byte[1024];
        DatagramPacket dp;
        eds.setSoTimeout(Config.timeoutMS);
        while (true) {
            dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
            eds.send(dp);
            System.out.println("close::fin sent");
            try {
                dp = new DatagramPacket(receivedBytes, receivedBytes.length);
                eds.receive(dp);
                segment = new TCPSegment(dp);
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
        byte[] receivedBytes = new byte[1024];
        DatagramPacket dp;
        eds.setSoTimeout(Config.timeoutMS);
        while (true) {
            dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
            eds.send(dp);
            //synSent;
            System.out.println("handshaking::syn sent");
            try {
                dp = new DatagramPacket(receivedBytes, receivedBytes.length);
                eds.receive(dp);
                segment = new TCPSegment(dp);
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
