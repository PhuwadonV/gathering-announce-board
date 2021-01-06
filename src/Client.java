import java.util.Properties;
import java.io.Closeable;
import java.io.IOException;
import java.io.Console;
import java.io.FileInputStream;
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

    private static Client client;
    private static Thread clientThread;
    private static InetAddress serverIp;
    private static byte[] serverIpBytes = new byte[4];
    private static int serverIpInt;
    private static String serverIpString;
    private static int serverPort;
    private static int gameId;

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
    private final byte[] ipBuff = new byte[4];
    private volatile boolean isRunning = true;
    private int responseToIp;
    private int responseToPort;
    private PassMode passMode = PassMode.DIRECT;
    private SocketAddress passThrough;
    private SocketAddress passTo;
    private int passLength;
    private int passIp;
    private int passPort;
    private int passOffset;

    public Client() throws IOException  {
        socket = new DatagramSocket();
        requestBuff = new byte[Protocol.CLIENT_REQBUFF_BYTES];
        responseBuff = new byte[Protocol.CLIENT_RESBUFF_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localIpBytes = InetAddress.getLocalHost().getAddress();
        localIp = Protocol.readInt(localIpBytes);
        localIpString = Protocol.readIp(localIpBytes);
        localPort = socket.getLocalPort();
        localAddress = localIpString + ":" + localPort;
    }

    public Client(int port) throws IOException  {
        socket = new DatagramSocket(port);
        requestBuff = new byte[Protocol.CLIENT_REQBUFF_BYTES];
        responseBuff = new byte[Protocol.CLIENT_RESBUFF_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localIpBytes = InetAddress.getLocalHost().getAddress();
        localIp = Protocol.readInt(localIpBytes);
        localIpString = Protocol.readIp(localIpBytes);
        localPort = socket.getLocalPort();
        localAddress = localIpString + ":" + localPort;
    }

    public Client(InetSocketAddress address) throws IOException  {
        socket = new DatagramSocket(address);
        requestBuff = new byte[Protocol.CLIENT_REQBUFF_BYTES];
        responseBuff = new byte[Protocol.CLIENT_RESBUFF_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localIpBytes = InetAddress.getLocalHost().getAddress();
        localIp = Protocol.readInt(localIpBytes);
        localIpString = Protocol.readIp(localIpBytes);
        localPort = socket.getLocalPort();
        localAddress = localIpString + ":" + localPort;
    }

    @Override
    public void close() {
        stop();
        socket.close();
    }

    @Override
    public void run() {
        while(isRunning) {
            try {
                responseToIp = 0;
                socket.receive(request);

                int length = process(
                    0,
                    0,
                    Protocol.readInt(request.getAddress().getAddress()),
                    request.getPort());

                if (length >= 0) {
                    response.setLength(length);

                    if (responseToIp == 0){
                        response.setSocketAddress(request.getSocketAddress());
                    }
                    else {
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
        int header = Protocol.readInt(requestBuff, requestOffset);
        if ((header & Protocol.CLIENT_FREQUENT_HEADER) > 0) {
            switch(header) {
                case Protocol.Header.ANNOUNCEMENT_STATUS:
                    System.out.println("\rANNOUNCEMENT_STATUS ");
                    return -1;
                case Protocol.Header.ANNOUNCEMENTS_OVERVIEW:
                    System.out.println("\rANNOUNCEMENTS_OVERVIEW ");
                    return -1;
                default:
                    return -1;
            }
        }
        else {
            switch(header) {
                case Protocol.Header.PROXY:
                    {
                        int length = process(
                            requestOffset + Protocol.Proxy.CONTENT_OFFSET,
                            responseOffset + Protocol.Proxy.BYTES,
                            Protocol.readInt(
                                requestBuff,
                                requestOffset + Protocol.Proxy.EIP_OFFSET),
                            Protocol.readInt(
                                requestBuff,
                                requestOffset + Protocol.Proxy.EPORT_OFFSET));

                        if (length >= 0) {
                            System.arraycopy(
                                requestBuff,
                                requestOffset,
                                responseBuff,
                                responseOffset,
                                Protocol.Proxy.BYTES);
                            return length + Protocol.Proxy.BYTES;
                        }
                        else {
                            return length;
                        }
                    }
                case Protocol.Header.FORWARD:
                    {
                        int length = process(
                            requestOffset + Protocol.Forward.CONTENT_OFFSET,
                            responseOffset,
                            Protocol.readInt(
                                requestBuff,
                                requestOffset + Protocol.Forward.EIP_OFFSET),
                            Protocol.readInt(
                                requestBuff,
                                requestOffset + Protocol.Forward.EPORT_OFFSET));

                        if (length >= 0) {
                            responseTo = new InetSocketAddress(
                                Protocol.readIp(
                                    requestBuff,
                                    requestOffset + Protocol.Forward.EIP_OFFSET),
                                Protocol.readInt(
                                    requestBuff,
                                    requestOffset + Protocol.Forward.EPORT_OFFSET));
                            return length + Protocol.Forward.BYTES;
                        }
                        else {
                            return length;
                        }
                    }
                case Protocol.Header.CONTACT:
                    Protocol.write(ipBuff, requestIp);
                    System.out.println(
                        "\r" +
                        Protocol.readIp(ipBuff) + ":" +
                        requestPort             + " -> Contact ");
                    Protocol.write(
                        responseBuff,
                        responseOffset,
                        Protocol.Header.ACKNOWLEDGE);
                    return Protocol.Acknowledge.BYTES;
                case Protocol.Header.ACKNOWLEDGE:
                    Protocol.write(ipBuff, requestIp);
                    System.out.println(
                        "\r" +
                        Protocol.readIp(ipBuff) + ":" +
                        requestPort             + " -> Acknowledge ");
                    return -1;
                case Protocol.Header.ASK:
                    Protocol.write(
                        responseBuff,
                        responseOffset,
                        Protocol.Header.ANSWER);
                    Protocol.write(
                        responseBuff,
                        responseOffset + Protocol.Answer.EIP_OFFSET,
                        requestIp);
                    Protocol.write(
                        responseBuff,
                        responseOffset + Protocol.Answer.EPORT_OFFSET,
                        requestPort);
                    return Protocol.Ask.BYTES;
                case Protocol.Header.ANSWER:
                    Protocol.write(ipBuff, requestIp);
                    System.out.println(
                        "\r" +
                        Protocol.readIp(ipBuff)                           + ":" +
                        requestPort                                       + " -> Answer " +
                        Protocol.readIp(
                            requestBuff,
                            requestOffset + Protocol.Answer.EIP_OFFSET)   + ":" +
                        Protocol.readInt(
                            requestBuff,
                            requestOffset + Protocol.Answer.EPORT_OFFSET) + " "
                        );
                    return -1;
                case Protocol.Header.SEND:
                    Protocol.write(ipBuff, requestIp);
                    System.out.println(
                        "\r" +
                        Protocol.readIp(ipBuff)                           + ":" +
                        requestPort                                       + " -> Send: " +
                        Protocol.readString(
                            requestBuff,
                            requestOffset + Protocol.Send.MESSAGE_OFFSET) + " "
                        );
                    return -1;
                case Protocol.Header.ANNOUNCEMENT_DETAIL:
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

    public byte[] getLocalIpBytes() {
        return localIpBytes;
    }

    public int getLocalIp() {
        return localIp;
    }

    public String getLocalIpString() {
        return localIpString;
    }

    public int  getLocalPort() {
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
        passBegin(address, Protocol.Signal.BYTES);
        Protocol.write(responseBuff, passOffset, Protocol.Header.SIGNAL);
        passEnd();
    }

    public synchronized void contact(InetSocketAddress address) throws IOException {
        passBegin(address, Protocol.Contact.BYTES);
        Protocol.write(responseBuff, passOffset, Protocol.Header.CONTACT);
        passEnd();
    }

    public synchronized void ask(InetSocketAddress address) throws IOException {
        passBegin(address, Protocol.Ask.BYTES);
        Protocol.write(responseBuff, passOffset, Protocol.Header.ASK);
        passEnd();
    }

    public synchronized void send(InetSocketAddress address, String message) throws IOException {
        passBegin(address, Protocol.Send.BYTES);
        Protocol.write(responseBuff, passOffset, Protocol.Header.SEND);
        Protocol.write(responseBuff, passOffset + Protocol.Send.MESSAGE_OFFSET, message);
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
        passIp = Protocol.readInt(address.getAddress().getAddress());
        passPort = address.getPort();
        passOffset =
            passMode == PassMode.DIRECT ||
            (passIp == serverIpInt && passPort == serverPort) ||
            isLocalIp(passIp) ?
                0 :
                Protocol.Proxy.CONTENT_OFFSET;
    }

    private void passEnd() throws IOException {
        if (passOffset == 0) {
            response.setLength(passLength);
            response.setSocketAddress(passTo);
        }
        else {
            Protocol.write(
                responseBuff,
                Protocol.Header.PROXY);
            Protocol.write(
                responseBuff,
                Protocol.Proxy.EIP_OFFSET,
                passIp);
            Protocol.write(
                responseBuff,
                Protocol.Proxy.EPORT_OFFSET,
                passPort);
            response.setLength(Protocol.Proxy.BYTES + passLength);
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
            serverIp = InetAddress.getByName(serverIpString);
            serverIpBytes = serverIp.getAddress();
            serverIpInt = Protocol.readInt(serverIpBytes);
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
            return new InetSocketAddress(serverIp, serverPort);
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
        System.out.println("\r7   # toll <port> [.|<ip>:<port>] ");
        System.out.println("\r8   # send [.|<ip>:<port>] <message> ");
        System.out.println("\r9   # post <announcement-name> ");
        System.out.println("\r10  # maintain ");
        System.out.println("\r11  # remove ");
        System.out.println("\r12  # look <current-board-version> <board-index> ");
        System.out.println("\r13  # read <announcement-id> ");
    }
}
