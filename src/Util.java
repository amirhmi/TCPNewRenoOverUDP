import java.nio.ByteBuffer;

public class Util {
    public static byte[] intToBytes(int x)
    {
        return ByteBuffer.allocate(4).putInt(x).array();
    }
    public static byte charToBytes(char c)
    {
        return (byte)c;
    }
    public static int bytesToInt(byte[] b, int start)
    {
        byte[] b2 = new byte[4];
        for (int i = start; i < start + 4; i++)
            b2[i - start] = b[i];
        return ByteBuffer.wrap(b2).getInt();
    }
    public static char bytesToChar(byte[] b, int start)
    {
        return (char)b[start];
    }
}
