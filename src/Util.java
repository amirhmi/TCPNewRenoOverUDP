import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static List<TCPSegment> breakToSegment(String path) throws Exception {
        List<TCPSegment> ret = new ArrayList<>();
        byte[] fileBytes = Files.readAllBytes(Paths.get(path));
        int retSize = fileBytes.length / Config.payloadLength;
        if (fileBytes.length % Config.payloadLength == 0)
            retSize --;
        for (int i = 0; i <= retSize; i++)
            ret.add(
                    new TCPSegment(false, false, false, i, 0,
                    Arrays.copyOfRange(fileBytes, i * Config.payloadLength,
                            Math.min((i + 1) * Config.payloadLength, fileBytes.length))));
        System.out.println("packet number:" + ret.size());
        return ret;
    }

    public static void addToFile(String path, byte[] data, boolean isAppend) throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path, isAppend));
        if (isAppend)
            writer.append(new String(data, "utf-8"));
        else
            writer.write(new String(data, "utf-8"));
        writer.close();
    }
}
