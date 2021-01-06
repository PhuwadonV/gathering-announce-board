import java.io.Closeable;
import java.net.*;

public class Tollway implements Closeable, Runnable {
    private final int gate;
    private final int destIp;
    private final int destPort;
    private final SocketAddress destination;
    private final DatagramSocket socket;
    private final DatagramPacket request;
    private final DatagramPacket response;
    private final byte[] requestBuff;
    private final byte[] responseBuff;
    private volatile boolean isRunning = true;
    private byte[] serverIpBytes = new byte[4];
    private int serverIp = 0;
    private String ipString;
    private int port;

    public Tollway(int gate, int destIp, int destPort) throws UnknownHostException {
        this.gate = gate;
        this.destIp = destIp;
        this.destPort = destPort;
        byte[] ipBuff = new byte[4];
        Protocol.write(ipBuff, destIp, 0);
        destination = new InetSocketAddress(InetAddress.getByAddress(ipBuff), destPort);
    }

    @Override
    public void close() {
        stop();
        socket.close();
    }

    @Override
    public void run() {

    }

    public synchronized void stop() {
        isRunning = false;
    }

    private static Tollway createTollway(String[] args) throws UnknownHostException {
        return new Tollway(
            Integer.parseInt(args[0]),
            Protocol.readInt(InetAddress.getByName(args[1]).getAddress()),
            Integer.parseInt(args[2]));
    }

    public static void main(String[] args) throws UnknownHostException {
        try (Tollway tollway = createTollway(args)) {
            tollway.run();
        }
    }
}
