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
    private long oldcwnd;
    private long ssthresh;
    private int dup;
    private int last_dup = -1;
    private CongestionControlState senderState = CongestionControlState.slowStart;
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
    }

    @Override
    public void send(String pathToFile) throws Exception {
        establishSenderConnection();

        List<TCPSegment> nagleBuffer = new ArrayList<>();
        cwnd = Config.defaultCWND;
        ssthresh = Config.defaultSSTHRESH;
        List<TCPSegment> segments = Util.breakToSegment(pathToFile);
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        eds.setSoTimeout(Config.timeoutMS);
        List<TCPSegment> window = new ArrayList<>();

        while(segments.size() > 0 || window.size() > 0)
        {
            System.out.println(senderState.toString() + "dup: " + dup);
            if (window.size() < (int)((cwnd + Config.MSS - 1) / Config.MSS) && segments.size() > 0) {
                sendNagle(segments.get(0), eds, nagleBuffer, window.size() > 0);
                window.add(segments.remove(0));
            }
            try {
                byte[] buf = new byte[Config.maximumReceivedBytes];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                eds.receive(dp);
                TCPSegment segment = new TCPSegment(dp);
                System.out.println("ack num : " + segment.ackNumber);
                if (segment.ack && !segment.syn && !segment.fin)
                {
                    int rwnd = Util.bytesToInt(segment.payload, 0);
                    System.out.println("ack number: " + segment.ackNumber);
                    if (segment.ackNumber < window.get(0).seqNumber) {
                        if (segment.ackNumber == last_dup)
                            dup++;
                        else
                        {
                            dup = 1;
                            last_dup = segment.ackNumber;
                        }
                    }
                    else
                        dup = 0;
                    while (window.size() > 0 && segment.ackNumber >= window.get(0).seqNumber)
                        window.remove(0);
                    if (senderState == CongestionControlState.slowStart) {
                        cwnd += Config.MSS;
                        onWindowChange();
                        if (cwnd > ssthresh)
                            senderState = CongestionControlState.congestionAvoidance;
                    }
                    else if (senderState == CongestionControlState.congestionAvoidance) {
                        cwnd += (Config.MSS * Config.MSS) / cwnd;
                        if (dup >= 3)
                        {
                            oldcwnd = cwnd / 2;
                            ssthresh = oldcwnd;
                            cwnd = ssthresh + 3;
                            for (TCPSegment w : window)
                                if (w.seqNumber == last_dup) {
                                    sendNagle(w, eds, nagleBuffer, false);
                                    break;
                                }
                            //TODO ask
                            senderState = CongestionControlState.fastRecovery;
                        }
                        onWindowChange();
                    }
                    else if (senderState == CongestionControlState.fastRecovery)
                    {
                        if (dup == 0) {
                            cwnd = oldcwnd / 2;
                            senderState = CongestionControlState.congestionAvoidance;
                        } else {
                            cwnd++;
                            for (TCPSegment w : window)
                                if (w.seqNumber == last_dup + 1) {
                                    sendNagle(w, eds, nagleBuffer, false);
                                    break;
                                }
                        }
                        onWindowChange();
                    }
                    cwnd = Math.min(cwnd, rwnd * Config.MSS);
                }
            }
            catch (SocketTimeoutException e)
            {
                for (TCPSegment w : window)
                    sendNagle(w, eds, nagleBuffer, true);
                emptyNalge(nagleBuffer, eds);
                if (senderState == CongestionControlState.slowStart);
                else if (senderState == CongestionControlState.congestionAvoidance)
                {
                    ssthresh = cwnd/2;
                    cwnd = 1;
                    senderState = CongestionControlState.slowStart;
                    onWindowChange();
                }
                else if (senderState == CongestionControlState.fastRecovery)
                {
                    ssthresh = oldcwnd/2;
                    cwnd = 1;
                    senderState = CongestionControlState.slowStart;
                    onWindowChange();
                }
            }
        }
        saveCongestionWindowPlot();
        eds.close();
    }

    private void sendNagle(TCPSegment segment, EnhancedDatagramSocket eds, List<TCPSegment> nagleBuffer,
                           boolean buffer) throws Exception
    {
        if (!Config.nagleActive) {
            sendSegment(segment, eds);
            return;
        }
        nagleBuffer.add(segment);
        if (nagleBuffer.size() >= Config.nagleBufferMax || !buffer)
            emptyNalge(nagleBuffer, eds);
    }

    private void sendSegment(TCPSegment segment, EnhancedDatagramSocket eds) throws Exception
    {
        byte[] segmentBytes = segment.toBytes();
        DatagramPacket dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.receiverIP), Config.receiverPort);
        eds.send(dp);
    }

    private void emptyNalge(List<TCPSegment> nagleBuffer, EnhancedDatagramSocket eds) throws Exception
    {
        if (!Config.nagleActive || nagleBuffer.size() == 0)
            return;
        List<List<TCPSegment>> consequtive = new ArrayList<>();
        consequtive.add(new ArrayList<>());
        for (TCPSegment segment : nagleBuffer)
            System.out.println(segment.seqNumber);
        for (TCPSegment segment : nagleBuffer)
            if (consequtive.get(consequtive.size() - 1).size() == 0 ||
                    consequtive.get(consequtive.size() - 1).get(consequtive.get(consequtive.size() - 1).size() - 1).seqNumber + 1
                            == segment.seqNumber)
                consequtive.get(consequtive.size() - 1).add(segment);
            else
            {
                consequtive.add(new ArrayList<>());
                consequtive.get(consequtive.size() - 1).add(segment);
            }
        for (List<TCPSegment> segments : consequtive) {
            System.out.println("----------------------------" + segments.size());
            int bufferSize = 0;
            for (TCPSegment segment : segments)
                bufferSize += segment.payload.length;
            byte[] buffer = new byte[bufferSize];
            int i = 0;
            for (TCPSegment segment : segments)
                for (byte b : segment.payload)
                    buffer[i++] = b;
            TCPSegment all = new TCPSegment(false, false, false, segments.size(), segments.get(0).seqNumber, 0, buffer);
            sendSegment(all, eds);
        }
        nagleBuffer.clear();
    }

    @Override
    public void receive(String pathToFile) throws Exception {
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        byte[] receivedBytes = new byte[Config.maximumReceivedBytes];
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
                    if ((window.size() == 0 || segment.seqNumber > lastReceived) &&
                        (window.size() < (Config.receiverBufferSize - segment.packetNum) || segment.seqNumber == lastReceived + 1)) {
                        int pos = 0;
                        for (TCPSegment w : window)
                            if (w.seqNumber < segment.seqNumber)
                                pos++;
                            else
                                break;
                        window.add(pos, segment);
                    }
                    while (window.size() > 0 && window.get(0).seqNumber <= lastReceived)
                        window.remove(0);
                    while (window.size() > 0 && window.get(0).seqNumber == lastReceived + 1) {
                        lastReceived+=window.get(0).packetNum;
                        Util.addToFile(pathToFile, window.remove(0).payload, true);
                    }
                    System.out.println(lastReceived);
                    sendAck(eds, lastReceived, Config.receiverBufferSize - window.size());
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

    public void sendAck (EnhancedDatagramSocket eds, int ackNumber, int rwnd) throws Exception
    {
        TCPSegment segment = new TCPSegment(false, true, false, 0, ackNumber, Util.intToBytes(rwnd));
        byte[] segmentBytes = segment.toBytes();
        DatagramPacket dp = new DatagramPacket(segmentBytes, segmentBytes.length, InetAddress.getByName(Config.senderIP), Config.senderPort);
        eds.send(dp);
    }

    @Override
    public void close() throws Exception {
        EnhancedDatagramSocket eds = new EnhancedDatagramSocket(port);
        TCPSegment segment = new TCPSegment(false, false, true, 342, 0, new byte[0]);
        byte[] segmentBytes = segment.toBytes();
        byte[] receivedBytes = new byte[Config.maximumReceivedBytes];
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
        byte[] receivedBytes = new byte[Config.maximumReceivedBytes];
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


    private enum CongestionControlState {slowStart, congestionAvoidance, fastRecovery}
}
