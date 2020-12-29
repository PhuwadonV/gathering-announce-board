import java.util.Properties;
import java.io.Closeable;
import java.io.IOException;
import java.io.Console;
import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class Client implements Closeable, Runnable {
    private enum PassMode {
        DIRECT,
        PROXY,
        FORWARD;
    }

    private static Client client = null;
    private static Thread clientThread = null;
    private static byte[] serverIpBytes = new byte[4];
    private static int serverIp = 0;
    private static String serverIpString = "0.0.0.0";
    private static int serverPort = 5000;
    private static int gameId = -1;

    private volatile boolean isRunning = true;
    private final DatagramSocket socket;
    private final DatagramPacket request;
    private final DatagramPacket response;
    private final byte[] requestBuff;
    private final byte[] responseBuff;
    private final byte[] localIpBytes;
    private final int localIp;
    private final String localIpString;
    private final int localPort;
    private final String localAddress;
    private SocketAddress responseTo;
    private byte[] ipBuff = new byte[4];
    private PassMode passMode = PassMode.DIRECT;
    private SocketAddress passThrough;
    private SocketAddress passTo;
    private int passLength;
    private int passIp;
    private int passPort;
    private int passOffset;

    public Client() throws IOException  {
        socket = new DatagramSocket();
        requestBuff = new byte[Protocal.CLIENT_REQUEST_BUFFER_BYTES];
        responseBuff = new byte[Protocal.CLIENT_RESPONSE_BUFFER_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localIpBytes = InetAddress.getLocalHost().getAddress();
        localIp = Protocal.readInt(localIpBytes, 0);
        localIpString = Protocal.readIp(localIpBytes, 0);
        localPort = socket.getLocalPort();
        localAddress = localIpString + ":" + localPort;
    }

    public Client(int port) throws IOException  {
        socket = new DatagramSocket(port);
        requestBuff = new byte[Protocal.CLIENT_REQUEST_BUFFER_BYTES];
        responseBuff = new byte[Protocal.CLIENT_RESPONSE_BUFFER_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localIpBytes = InetAddress.getLocalHost().getAddress();
        localIp = Protocal.readInt(localIpBytes, 0);
        localIpString = Protocal.readIp(localIpBytes, 0);
        localPort = socket.getLocalPort();
        localAddress = localIpString + ":" + localPort;
    }

    public Client(InetSocketAddress address) throws IOException  {
        socket = new DatagramSocket(address);
        requestBuff = new byte[Protocal.CLIENT_REQUEST_BUFFER_BYTES];
        responseBuff = new byte[Protocal.CLIENT_RESPONSE_BUFFER_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localIpBytes = InetAddress.getLocalHost().getAddress();
        localIp = Protocal.readInt(localIpBytes, 0);
        localIpString = Protocal.readIp(localIpBytes, 0);
        localPort = socket.getLocalPort();
        localAddress = localIpString + ":" + localPort;
    }

    @Override
    public void run() {
        while(isRunning) {
            try {
                responseTo = null;
                socket.receive(request);

                int length = process(
                    0,
                    0,
                    Protocal.readInt(request.getAddress().getAddress(), 0),
                    request.getPort());

                if (length >= 0) {
                    response.setLength(length);

                    if (responseTo == null){
                        response.setSocketAddress(request.getSocketAddress());
                    }

                    socket.send(response);
                }
            }
            catch (IOException e) {
                System.out.println("\r" +  e.getMessage() + " ");
            }
        }
    }

    private int process(int requestOffset, int responseOffset, int requestIp, int requestPort) {
        int header = Protocal.readInt(requestBuff, requestOffset);
        if ((header & Protocal.CLIENT_FREQUENT_HEADER) > 0) {
            switch(header) {
                case Protocal.Header.ANNOUNCEMENT_STATUS:
                    System.out.println("\rANNOUNCEMENT_STATUS ");
                    return -1;
                case Protocal.Header.ANNOUNCEMENTS_OVERVIEW:
                    System.out.println("\rANNOUNCEMENTS_OVERVIEW ");
                    return -1;
                default:
                    return -1;
            }
        }
        else {
            switch(header) {
                case Protocal.Header.PROXY:
                    {
                        int length = process(
                            requestOffset + Protocal.Proxy.CONTENT_OFFSET,
                            responseOffset + Protocal.Proxy.BYTES,
                            Protocal.readInt(
                                requestBuff,
                                requestOffset + Protocal.Proxy.EIP_OFFSET),
                            Protocal.readInt(
                                requestBuff,
                                requestOffset + Protocal.Proxy.EPORT_OFFSET));

                        if (length >= 0) {
                            System.arraycopy(
                                requestBuff,
                                requestOffset,
                                responseBuff,
                                responseOffset,
                                Protocal.Proxy.BYTES);
                            return length + Protocal.Proxy.BYTES;
                        }
                        else {
                            return length;
                        }
                    }
                case Protocal.Header.FORWARD:
                    {
                        int length = process(
                            requestOffset + Protocal.Forward.CONTENT_OFFSET,
                            responseOffset,
                            Protocal.readInt(
                                requestBuff,
                                requestOffset + Protocal.Forward.EIP_OFFSET),
                            Protocal.readInt(
                                requestBuff,
                                requestOffset + Protocal.Forward.EPORT_OFFSET));

                        if (length >= 0) {
                            responseTo = new InetSocketAddress(
                                Protocal.readIp(
                                    requestBuff,
                                    requestOffset + Protocal.Forward.EIP_OFFSET),
                                Protocal.readInt(
                                    requestBuff,
                                    requestOffset + Protocal.Forward.EPORT_OFFSET));
                            return length + Protocal.Forward.BYTES;
                        }
                        else {
                            return length;
                        }
                    }
                case Protocal.Header.SIGNAL:
                    return -1;
                case Protocal.Header.CONTACT:
                    Protocal.write(ipBuff, 0, requestIp);
                    System.out.println(
                        "\r" +
                        Protocal.readIp(ipBuff, 0) + ":" +
                        requestPort                + " -> Contact ");
                    Protocal.write(
                        responseBuff,
                        responseOffset,
                        Protocal.Header.ACKNOWLEDGE);
                    return Protocal.Acknowledge.BYTES;
                case Protocal.Header.ACKNOWLEDGE:
                    Protocal.write(ipBuff, 0, requestIp);
                    System.out.println(
                        "\r" +
                        Protocal.readIp(ipBuff, 0) + ":" +
                        requestPort                + " -> Acknowledge ");
                    return -1;
                case Protocal.Header.ASK:
                    Protocal.write(
                        responseBuff,
                        responseOffset,
                        Protocal.Header.ANSWER);
                    Protocal.write(
                        responseBuff,
                        responseOffset + Protocal.Answer.EIP_OFFSET,
                        requestIp);
                    Protocal.write(
                        responseBuff,
                        responseOffset + Protocal.Answer.EPORT_OFFSET,
                        requestPort);
                    return Protocal.Ask.BYTES;
                case Protocal.Header.ANSWER:
                    Protocal.write(ipBuff, 0, requestIp);
                    System.out.println(
                        "\r" +
                        Protocal.readIp(ipBuff, 0)                        + ":" +
                        requestPort                                       + " -> Answer " +
                        Protocal.readIp(
                            requestBuff,
                            requestOffset + Protocal.Answer.EIP_OFFSET)   + ":" +
                        Protocal.readInt(
                            requestBuff,
                            requestOffset + Protocal.Answer.EPORT_OFFSET) + " "
                        );
                    return -1;
                case Protocal.Header.SEND:
                    Protocal.write(ipBuff, 0, requestIp);
                    System.out.println(
                        "\r" +
                        Protocal.readIp(ipBuff, 0)                        + ":" +
                        requestPort                                       + " -> Send: " +
                        Protocal.readString(
                            requestBuff,
                            requestOffset + Protocal.Send.MESSAGE_OFFSET) + " "
                        );
                    return -1;
                case Protocal.Header.ANNOUNCEMENT_DETAIL:
                    System.out.println("\rANNOUNCEMENT_DETAIL ");
                    return -1;
                default:
                    return -1;
            }
        }
    }

    public boolean getIsRunning() {
        return isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
    }

    @Override
    public void close() {
        stop();
        socket.close();
    }

    public byte[] getLocalIpBytes() {
        return localIpBytes;
    }

    public int getLocalIp() {
        return localIp;
    }

    public String getLocalIpString() {
        return localIpString;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public PassMode getPassMode() {
        return passMode;
    }

    public SocketAddress getPassThrough() {
        return passThrough;
    }

    public synchronized void usePassDirect() {
        passMode = PassMode.DIRECT;
        passThrough = null;
    }

    public synchronized void usePassProxy(SocketAddress address) {
        passMode = PassMode.PROXY;
        passThrough = address;
    }

    public synchronized void usePassForward(SocketAddress address) {
        passMode = PassMode.FORWARD;
        passThrough = address;
    }

    public synchronized void signal(InetSocketAddress address) throws IOException {
        passBegin(address, Protocal.Signal.BYTES);
        Protocal.write(responseBuff, passOffset, Protocal.Header.SIGNAL);
        passEnd();
    }

    public synchronized void contact(InetSocketAddress address) throws IOException {
        passBegin(address, Protocal.Contact.BYTES);
        Protocal.write(responseBuff, passOffset, Protocal.Header.CONTACT);
        passEnd();
    }

    public synchronized void ask(InetSocketAddress address) throws IOException {
        passBegin(address, Protocal.Ask.BYTES);
        Protocal.write(responseBuff, passOffset, Protocal.Header.ASK);
        passEnd();
    }

    public synchronized void send(InetSocketAddress address, String message) throws IOException {
        passBegin(address, Protocal.Send.BYTES);
        Protocal.write(responseBuff, passOffset, Protocal.Header.SEND);
        Protocal.write(responseBuff, passOffset + Protocal.Send.MESSAGE_OFFSET, message);
        passEnd();
    }

    private boolean isLocalIp(int ip) {
        return
            (ip & 0xFFFF0000) == 0xC0A80000 ||
            (ip & 0xFFF00000) == 0xAC100000 ||
            (ip & 0xFF000000) == 0x0A000000;
    }

    private void passBegin(InetSocketAddress address, int length) {
        passTo = address;
        passLength = length;
        passIp = Protocal.readInt(address.getAddress().getAddress(), 0);
        passPort = address.getPort();
        passOffset =
            passMode == PassMode.DIRECT ||
            (passIp == serverIp && passPort == serverPort) ||
            isLocalIp(passIp) ?
                0 :
                Protocal.Proxy.CONTENT_OFFSET;
    }

    private void passEnd() throws IOException {
        if (passOffset == 0) {
            response.setLength(passLength);
            response.setSocketAddress(passTo);
        }
        else {
            Protocal.write(
                responseBuff,
                0,
                passMode == PassMode.PROXY ? Protocal.Header.PROXY : Protocal.Header.FORWARD);
            Protocal.write(
                responseBuff,
                Protocal.Proxy.EIP_OFFSET,
                passIp);
            Protocal.write(
                responseBuff,
                Protocal.Proxy.EPORT_OFFSET,
                passPort);
            response.setLength(Protocal.Proxy.BYTES + passLength);
            response.setSocketAddress(passThrough);
        }

        socket.send(response);
    }

    private static Client createClient(String[] args) throws IOException {
        switch(args.length) {
            case 0:
                return new Client();
            case 1:
                return new Client(Integer.parseInt(args[0]));
            default:
                return new Client(new InetSocketAddress(args[1], Integer.parseInt(args[0])));
        }
    }

    public static void main(String[] args) throws IOException {
        Console console = System.console();

        try (FileInputStream config = new FileInputStream("config.properties")) {
            Properties properties = new Properties();
            properties.load(config);
            serverIpString = properties.getProperty("serverIp");
            serverIpBytes = InetAddress.getByName(serverIpString).getAddress();
            serverIp = Protocal.readInt(serverIpBytes, 0);
            serverPort = Integer.parseInt(properties.getProperty("serverPort"));
            gameId = Integer.parseInt(properties.getProperty("gameId"));
        }

        try (Client c = createClient(args)) {
            client = c;
            clientThread = new Thread(client);
            clientThread.start();
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
                case "show":
                    show(tokens);
                    return true;
                case "pass":
                    pass(tokens);
                    return true;
                case "signal":
                    signal(tokens);
                    return true;
                case "contact":
                    contact(tokens);
                    return true;
                case "ask":
                    ask(tokens);
                    return true;
                case "send":
                    send(tokens);
                    return true;
                case "post":
                    return true;
                case "maintain":
                    return true;
                case "remove":
                    return true;
                case "look":
                    return true;
                case "read":
                    return true;
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

    private static InetSocketAddress getAddress(String token) {
        if(token.equals(".")) {
            return new InetSocketAddress(
                serverIpString,
                serverPort);
        }
        else {
            int separatorIndex = token.indexOf(":");
            return new InetSocketAddress(
                token.substring(0, separatorIndex),
                Integer.parseInt(token.substring(separatorIndex + 1)));
        }
    }

    private static void exit() {
        client = null;
        clientThread = null;
    }

    private static void show(String[] tokens) {
        switch(tokens[1]) {
            case "local-address":
                System.out.println("\rLocal address: " + client.getLocalAddress() + " ");
                break;
            case "pass-mode":
                switch(client.getPassMode()) {
                    case DIRECT:
                        System.out.println("\rPass mode: Direct ");
                        break;
                    case PROXY:
                        System.out.println("\rPass mode: Proxy ");
                        break;
                    case FORWARD:
                        System.out.println("\rPass mode: Forward ");
                        break;
                }
                break;
            default:
                System.out.println("\rUnknown command ");
                break;
        }
    }

    private static void pass(String[] tokens) {
        switch(tokens[1]) {
            case "direct":
                client.usePassDirect();
                break;
            case "proxy":
                client.usePassProxy(getAddress(tokens[2]));
                break;
            case "forward":
                client.usePassForward(getAddress(tokens[2]));
                break;
            default:
                System.out.println("\rUnknown command ");
                break;
        }
    }

    private static void signal(String[] tokens) throws IOException {
        client.signal(getAddress(tokens[1]));
    }

    private static void contact(String[] tokens) throws IOException {
        client.contact(getAddress(tokens[1]));
    }

    private static void ask(String[] tokens) throws IOException {
        client.ask(getAddress(tokens[1]));
    }

    private static void send(String[] tokens) throws IOException {
        client.send(getAddress(tokens[1]), tokens[2]);
    }

    private static void help() {
        System.out.println("\r1   # exit ");
        System.out.println("\r2   # show [local-address|pass-mode] ");
        System.out.println("\r3.1 # pass [direct] ");
        System.out.println("\r .2 # pass [forward|proxy] [.|<ip>:<port>]");
        System.out.println("\r4   # signal [.|<ip>:<port>] ");
        System.out.println("\r5   # contact [.|<ip>:<port>] ");
        System.out.println("\r6   # ask [.|<ip>:<port>] ");
        System.out.println("\r7   # send [.|<ip>:<port>] <message> ");
        System.out.println("\r8   # post <announcement-name> ");
        System.out.println("\r9   # maintain ");
        System.out.println("\r10  # remove ");
        System.out.println("\r11  # look <current-board-version> <board-index> ");
        System.out.println("\r12  # read <announcement-id> ");
    }
}
