import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "StatementWithEmptyBody", "FieldCanBeLocal"})
public class Tollway implements Closeable, Runnable {
    private static Tollway tollway;
    private static Thread tollwayThread;

    private final Consumer<Integer> onDestroy;
    private final InetAddress serverIp;
    private final InetAddress localIp;
    private final byte[] serverIpBytes;
    private final byte[] localIpBytes;
    private final int serverIpInt;
    private final int localIpInt;
    private final String serverIpString;
    private final String localIpString;
    private final InetSocketAddress serverAddress;
    private final InetSocketAddress localAddress;
    private final String serverAddressString;
    private final String localAddressString;
    private final int serverPort;
    private final int localPort;
    private final int gate;
    private final int destIp;
    private final int destPort;
    private final InetSocketAddress destination;
    private final DatagramSocket socket;
    private final DatagramPacket request;
    private final DatagramPacket response;
    private final byte[] requestBuff;
    private final byte[] responseBuff;
    private final byte[] ipBuff = new byte[Protocol.IP_BYTES];
    private volatile boolean isRunning = true;

    public Tollway(
            int serverIpInt, int serverPort,
            int gate, int destIp, int destPort,
            Consumer<Integer> onCreate,
            Consumer<Integer> onDestroy)
            throws UnknownHostException, SocketException {
        this.onDestroy = onDestroy;
        this.serverIpInt = serverIpInt;
        Protocol.write(ipBuff, serverIpInt);
        this.serverIpString = Protocol.readIp(ipBuff);
        this.serverIp = InetAddress.getByName(serverIpString);
        this.serverIpBytes = serverIp.getAddress();
        this.serverPort = serverPort;
        this.serverAddress = new InetSocketAddress(InetAddress.getByAddress(ipBuff), serverPort);
        this.serverAddressString = serverIpString + ":" + serverPort;
        this.gate = gate;
        this.destIp = destIp;
        this.destPort = destPort;
        Protocol.write(ipBuff, destIp);
        this.destination = new InetSocketAddress(InetAddress.getByAddress(ipBuff), destPort);
        this.socket = new DatagramSocket();
        this.requestBuff = new byte[Protocol.TOLLWAY_REQBUFF_BYTES];
        this.responseBuff = new byte[Protocol.TOLLWAY_RESBUFF_BYTES];
        this.request = new DatagramPacket(requestBuff, requestBuff.length);
        this.response = new DatagramPacket(responseBuff, responseBuff.length);
        this.localIp = InetAddress.getLocalHost();
        this.localIpBytes = localIp.getAddress();
        this.localIpInt = Protocol.readInt(localIpBytes);
        this.localIpString = Protocol.readIp(localIpBytes);
        this.localPort = socket.getLocalPort();
        this.localAddress = new InetSocketAddress(InetAddress.getByAddress(localIpBytes), localPort);
        this.localAddressString = localIpString + ":" + localPort;

        if (onCreate != null) {
            onCreate.accept(localPort);
        }
    }

    @Override
    public void close() {
        stop();
        socket.close();
        if (onDestroy != null) {
            onDestroy.accept(localPort);
        }
    }

    @Override
    public void run() {
        while(isRunning) {
            try {
                process();
            }
            catch (IOException e) {
                System.out.println("\r" +  e.getMessage() + " ");
            }
        }
    }

    private void process() throws IOException {
        socket.receive(request);
    }

    public boolean getIsRunning() {
        return isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
    }

    public byte[] getLocalIpBytes() {
        return localIpBytes;
    }

    public InetAddress getLocalIp() {
        return localIp;
    }

    public String getLocalIpString() {
        return localIpString;
    }

    public int getLocalPort() {
        return localPort;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public String getLocalAddressString() {
        return localAddressString;
    }

    public byte[] getServerIpBytes() {
        return serverIpBytes;
    }

    public InetAddress getServerIp() {
        return serverIp;
    }

    public String getServerIpString() {
        return serverIpString;
    }

    public int getServerPort() {
        return serverPort;
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    public String getServerAddressString() {
        return serverAddressString;
    }

    private static Tollway createTollway(String[] args) throws IOException {
        try (FileInputStream config = new FileInputStream("config.properties")) {
            Properties properties = new Properties();
            properties.load(config);
            return new Tollway(
                Protocol.readInt(InetAddress.getByName(properties.getProperty("serverIp")).getAddress()),
                Integer.parseInt(properties.getProperty("serverPort")),
                Integer.parseInt(args[0]),
                Protocol.readInt(InetAddress.getByName(args[1]).getAddress()),
                Integer.parseInt(args[2]),
                (port) -> System.out.println("Create port: " + port),
                (port) -> System.out.println("Destroy port: " + port));
        }
    }

    public static void main(String[] args) throws IOException {
        Console console = System.console();

        try (Tollway t = createTollway(args)) {
            tollway = t;
            tollwayThread = new Thread(tollway);
            tollwayThread.start();
            System.out.println("\r------------------------------ ");
            while(processInput(console));
        }
    }

    private static boolean processInput(Console console) {
        try {
            String line = console.readLine();
            if (line == null) return false;
            String[] tokens = line.split(" ");
            switch(tokens[0]) {
                case "exit":
                    exit();
                    return false;
                case "help":
                    help();
                    return true;
                default:
                    System.out.println("\rUnknown command ");
                    return true;
            }
        }
        catch(Exception e) {
            System.out.println("\rError: " + e.getMessage() + " ");
            return true;
        }
        finally {
            System.out.println("\r------------------------------ ");
        }
    }

    private static void exit() {
        tollway = null;
        tollwayThread = null;
    }

    private static void help() {
        System.out.println("\r1 # exit ");
    }
}
