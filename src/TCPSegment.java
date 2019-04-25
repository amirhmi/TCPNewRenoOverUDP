import java.nio.ByteBuffer;

public class TCPSegment {
    public boolean syn;
    public boolean ack;
    public int seqNumber;
    public int ackNumber;
    public byte[] payload;
    public byte[] toBytes()
    {
        byte[] ret = new byte[1+4+4+payload.length];
        ret[0] = Util.charToBytes((char)(syn?(ack?3:2):(ack?1:0)));
        for (int i = 0; i < 4; i++)
            ret[1+i] = Util.intToBytes(seqNumber)[i];
        for (int i = 0; i < 4; i++)
            ret[5+i] = Util.intToBytes(ackNumber)[i];
        for (int i = 0; i < payload.length; i++)
            ret[9+i] = payload[i];
        return ret;
    }
    public TCPSegment(byte[] bytes)
    {
        int synack = (int)Util.bytesToChar(bytes, 0);
        syn = synack > 1;
        ack = (synack % 2 == 1);
        seqNumber = Util.bytesToInt(bytes, 1);
        ackNumber = Util.bytesToInt(bytes, 5);
        payload = new byte[bytes.length - 9];
        for (int i = 9; i < bytes.length; i++)
            payload[i - 9] = bytes[i];
    }
    public TCPSegment(boolean syn, boolean ack, int seqNumber, int ackNumber, byte[] payload)
    {
        this.syn = syn;
        this.ack = ack;
        this.seqNumber = seqNumber;
        this.ackNumber = ackNumber;
        this.payload = payload;
    }
}
