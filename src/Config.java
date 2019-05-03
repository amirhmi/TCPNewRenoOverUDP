public class Config {
    public static int senderPort = 12345;
    public static int receiverPort = 12346;
    public static String senderIP = "127.0.0.1";
    public static String receiverIP = "127.0.0.1";
    public static String localhost = "127.0.0.1";
    public static int payloadLength = 256;
    public static int MSS = payloadLength + 9;
    public static int timeoutMS = 25;
    public static int sendRepeat = 25;
    public static long defaultCWND = 2 * MSS;
    public static long defaultSSTHRESH = 4 * MSS;
    public static int receiverBufferSize = 20;
    public static int maximumReceivedBytes = 1024;
    public static boolean nagleActive = false;
    public static int nagleBufferMax = 4;
}
