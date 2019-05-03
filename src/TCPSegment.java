import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCPSegment {
    public boolean syn;
    public boolean ack;
    public boolean fin;
    public int packetNum;
    public int seqNumber;
    public int ackNumber;
    public byte[] payload;
    public byte[] toBytes()
    {
        byte[] ret = new byte[1+4+4+payload.length];
        ret[0] = Util.charToBytes((char)(packetNum*8 + (fin?4:0)+(syn?2:0)+(ack?1:0)));
        for (int i = 0; i < 4; i++)
            ret[1+i] = Util.intToBytes(seqNumber)[i];
        for (int i = 0; i < 4; i++)
            ret[5+i] = Util.intToBytes(ackNumber)[i];
        for (int i = 0; i < payload.length; i++)
            ret[9+i] = payload[i];
        return ret;
    }
    public TCPSegment(DatagramPacket dp)
    {
        byte[] bytes = Arrays.copyOfRange(dp.getData(), 0, dp.getLength());//new String(dp.getData(), 0, dp.getLength()).getBytes();
        int flags = (int)Util.bytesToChar(bytes, 0);
        packetNum = flags / 8;
        flags %= 8;
        fin = (flags >= 4);
        syn = (flags % 4) >= 2;
        ack = (flags % 2) >= 1;
        seqNumber = Util.bytesToInt(bytes, 1);
        ackNumber = Util.bytesToInt(bytes, 5);
        payload = new byte[bytes.length - 9];
        for (int i = 9; i < bytes.length; i++)
            payload[i - 9] = bytes[i];
    }
    public TCPSegment(boolean syn, boolean ack, boolean fin, int seqNumber, int ackNumber, byte[] payload)
    {
        this.syn = syn;
        this.ack = ack;
        this.fin = fin;
        this.packetNum = 1;
        this.seqNumber = seqNumber;
        this.ackNumber = ackNumber;
        this.payload = payload;
    }
    public TCPSegment(boolean syn, boolean ack, boolean fin, int packetNum, int seqNumber, int ackNumber, byte[] payload)
    {
        this.syn = syn;
        this.ack = ack;
        this.fin = fin;
        this.packetNum = packetNum;
        if (this.packetNum > 31 || this.packetNum < 1)
            throw new RuntimeException();
        this.seqNumber = seqNumber;
        this.ackNumber = ackNumber;
        this.payload = payload;
    }
}
