public class Config {
    public static int senderPort = 12345;
    public static int receiverPort = 12346;
    public static String senderIP = "127.0.0.1";
    public static String receiverIP = "127.0.0.1";
    public static String localhost = "127.0.0.1";
    public static int payloadLength = 128;
    public static int MSS = payloadLength + 9;
    public static int timeoutMS = 20;
    public static int sendRepeat = 25;
    public static long defaultCWND = 8 * MSS;
    public static long defaultSSTHRESH = 16 * MSS;
    public static int receiverBufferSize = 20;
    public static int maximumReceivedBytes = 1024;
}
