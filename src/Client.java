import java.io.*;
import java.net.*;
import java.util.Properties;

@SuppressWarnings({"unused", "StatementWithEmptyBody", "FieldCanBeLocal"})
public class Client implements Closeable, Runnable {
    private static Client client;
    private static Thread clientThread;
    private static Address serverAddress;
    private static int gameId;

    private final DatagramSocket socket;
    private final DatagramPacket request;
    private final DatagramPacket response;
    private final byte[] requestBuff;
    private final byte[] responseBuff;
    private final Address localAddress;
    private final byte[] ipBuff = new byte[Protocol.IP_BYTES];
    private volatile boolean isRunning = true;
    private int boardVersion = 0;
    private String lastAnnouncementsOverview = "";

    public Client() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        requestBuff = new byte[Protocol.CLIENT_REQBUFF_BYTES];
        responseBuff = new byte[Protocol.CLIENT_RESBUFF_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localAddress = new Address(InetAddress.getLocalHost(), socket.getLocalPort());
    }

    public Client(int port) throws SocketException, UnknownHostException {
        socket = new DatagramSocket(port);
        requestBuff = new byte[Protocol.CLIENT_REQBUFF_BYTES];
        responseBuff = new byte[Protocol.CLIENT_RESBUFF_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localAddress = new Address(InetAddress.getLocalHost(), socket.getLocalPort());
    }

    public Client(InetSocketAddress address) throws SocketException, UnknownHostException {
        socket = new DatagramSocket(address);
        requestBuff = new byte[Protocol.CLIENT_REQBUFF_BYTES];
        responseBuff = new byte[Protocol.CLIENT_RESBUFF_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
        localAddress = new Address(InetAddress.getLocalHost(), socket.getLocalPort());
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
                process();
            }
            catch (IOException e) {
                System.out.println("\r" +  e.getMessage() + " ");
            }
        }
    }

    private void process() throws IOException {
        StringBuilder message;
        socket.receive(request);
        int header = Protocol.readInt(requestBuff);
        if ((header & Protocol.CLIENT_FREQUENT_HEADER) > 0) {
            switch(header) {
                case Protocol.Header.ANNOUNCEMENT_STATUS:
                    message = new StringBuilder("\rAnnouncement Status : ");
                    switch (Protocol.readInt(requestBuff, Protocol.AnnouncementStatus.STATUS_OFFSET)) {
                        case Protocol.AnnouncementStatus.CREATED:
                            message.append("CREATED ");
                            break;
                        case Protocol.AnnouncementStatus.ALREADY_EXISTS:
                            message.append("ALREADY_EXISTS ");
                            break;
                        case Protocol.AnnouncementStatus.MAINTAINED:
                            message.append("MAINTAINED ");
                            break;
                        case Protocol.AnnouncementStatus.NOT_EXISTS:
                            message.append("NOT_EXISTS ");
                            break;
                        case Protocol.AnnouncementStatus.REMOVED:
                            message.append("REMOVED ");
                            break;
                        case Protocol.AnnouncementStatus.BOARD_NO_CHANGE:
                            message.append("BOARD_NO_CHANGE \n");
                            message.append(lastAnnouncementsOverview);
                            break;
                        }
                        System.out.println(message);
                    break;
                case Protocol.Header.ANNOUNCEMENTS_OVERVIEW:
                    {
                        boardVersion = Protocol.readInt(
                            requestBuff,
                            Protocol.AnnouncementsOverview.VERSION_OFFSET);
                        int length = Protocol.readInt(requestBuff, Protocol.AnnouncementsOverview.LENGTH_OFFSET);
                        message = new StringBuilder("\rAnnouncements Overview ");
                        for(int i = 0; i < length; i++) {
                            int offset =
                                Protocol.AnnouncementsOverview.ITEMS_OFFSET +
                                Protocol.AnnouncementsOverview.ITEM_BYTES * i;
                            int announcementIdOffset = offset + Protocol.AnnouncementsOverview.ITEM_ANNOUNCEMENT_ID_OFFSET;
                            int nameOffset = offset + Protocol.AnnouncementsOverview.ITEM_NAME_OFFSET;
                            message.append("\n    ");
                            message.append(Protocol.readHexString(requestBuff, announcementIdOffset));
                            message.append(" ");
                            message.append(Protocol.readString(requestBuff, nameOffset));
                            message.append(" ");
                        }
                        lastAnnouncementsOverview = message.toString();
                        System.out.println(lastAnnouncementsOverview);
                    }
                    break;
                default:
                    break;
            }
        }
        else {
            switch(header) {
                case Protocol.Header.SIGNAL:
                    break;
                case Protocol.Header.CONTACT:
                    System.arraycopy(
                        request.getAddress().getAddress(), 0,
                        ipBuff, 0,
                        Protocol.IP_BYTES);
                    message = new StringBuilder("\r");
                    message.append(Protocol.readIp(ipBuff));
                    message.append(":");
                    message.append(request.getPort());
                    message.append(" -> Contact ");
                    System.out.println(message);
                    Protocol.write(
                        responseBuff,
                        Protocol.Header.ACKNOWLEDGE);
                    response.setLength(Protocol.Acknowledge.BYTES);
                    response.setSocketAddress(request.getSocketAddress());
                    socket.send(response);
                    break;
                case Protocol.Header.ACKNOWLEDGE:
                    System.arraycopy(
                        request.getAddress().getAddress(), 0,
                        ipBuff, 0,
                        Protocol.IP_BYTES);
                    message = new StringBuilder("\r");
                    message.append(Protocol.readIp(ipBuff));
                    message.append(":");
                    message.append(request.getPort());
                    message.append(" -> Acknowledge ");
                    System.out.println(message);
                    break;
                case Protocol.Header.ASK:
                    Protocol.write(responseBuff, Protocol.Header.ANSWER);
                    System.arraycopy(
                        request.getAddress().getAddress(), 0,
                        responseBuff, Protocol.Answer.EIP_OFFSET,
                        Protocol.IP_BYTES);
                    Protocol.write(
                        responseBuff,
                        Protocol.Answer.EPORT_OFFSET,
                        request.getPort());
                    response.setSocketAddress(request.getSocketAddress());
                    response.setLength(Protocol.Answer.BYTES);
                    socket.send(response);
                    break;
                case Protocol.Header.ANSWER:
                    System.arraycopy(
                        request.getAddress().getAddress(), 0,
                        ipBuff, 0,
                        Protocol.IP_BYTES);
                    message = new StringBuilder("\r");
                    message.append(Protocol.readIp(ipBuff));
                    message.append(":");
                    message.append(request.getPort());
                    message.append(" -> Answer ");
                    message.append(Protocol.readIp(requestBuff, Protocol.Answer.EIP_OFFSET));
                    message.append(":");
                    message.append(Protocol.readIp(requestBuff, Protocol.Answer.EPORT_OFFSET));
                    message.append(" ");
                    System.out.println(message);
                    break;
                case Protocol.Header.ANNOUNCEMENT_DETAIL:
                    message = new StringBuilder("\rAnnouncement Detail ");
                    message.append("\n    Local    : ");
                    message.append(Protocol.readIp(responseBuff, Protocol.AnnouncementDetail.LIP_OFFSET));
                    message.append(":");
                    message.append(Protocol.readInt(responseBuff, Protocol.AnnouncementDetail.LPORT_OFFSET));
                    message.append("\n    External : ");
                    message.append(Protocol.readIp(responseBuff, Protocol.AnnouncementDetail.EIP_OFFSET));
                    message.append(":");
                    message.append(Protocol.readInt(responseBuff, Protocol.AnnouncementDetail.EPORT_OFFSET));
                    message.append(" ");
                    System.out.println(message);
                    break;
            }
        }
    }

    public boolean getIsRunning() {
        return isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
    }

    public int getLocalIpInt() {
        return localAddress.getIpInt();
    }

    public byte[] getLocalIpBytes() {
        return localAddress.getIpBytes();
    }

    public InetAddress getLocalIp() {
        return localAddress.getIp();
    }

    public String getLocalIpString() {
        return localAddress.getIpString();
    }

    public int getLocalPort() {
        return localAddress.getPort();
    }

    public InetSocketAddress getLocalInetSocketAddress() {
        return localAddress.getInetSocketAddress();
    }

    public String getLocalAddressString() {
        return localAddress.toString();
    }

    public synchronized void signal(InetSocketAddress address) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.SIGNAL);
        response.setLength(Protocol.Signal.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    public synchronized void contact(InetSocketAddress address) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.CONTACT);
        response.setLength(Protocol.Contact.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    public synchronized void ask(InetSocketAddress address) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.ASK);
        response.setLength(Protocol.Ask.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    public synchronized void post(
            InetSocketAddress address, int gameId,
            int localIp, int localPort, String name) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.POST_ANNOUNCEMENT);
        Protocol.write(responseBuff, Protocol.PostAnnouncement.GAME_ID_OFFSET, gameId);
        Protocol.write(responseBuff, Protocol.PostAnnouncement.LIP_OFFSET, localIp);
        Protocol.write(responseBuff, Protocol.PostAnnouncement.LPORT_OFFSET, localPort);
        Protocol.write(responseBuff, Protocol.PostAnnouncement.NAME_OFFSET, name);
        response.setLength(Protocol.PostAnnouncement.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    public synchronized void maintain(InetSocketAddress address, int gameId) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.MAINTAIN_ANNOUNCEMENT);
        Protocol.write(responseBuff, Protocol.MaintainAnnouncement.GAME_ID_OFFSET, gameId);
        response.setLength(Protocol.MaintainAnnouncement.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    public synchronized void remove(InetSocketAddress address, int gameId) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.REMOVE_ANNOUNCEMENT);
        Protocol.write(responseBuff, Protocol.RemoveAnnouncement.GAME_ID_OFFSET, gameId);
        response.setLength(Protocol.RemoveAnnouncement.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    public synchronized void look(
            InetSocketAddress address, int gameId,
            int boardIndex) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.REMOVE_ANNOUNCEMENT);
        Protocol.write(responseBuff, Protocol.LookAtBoard.GAME_ID_OFFSET, gameId);
        Protocol.write(responseBuff, Protocol.LookAtBoard.VERSION_OFFSET, boardVersion);
        Protocol.write(responseBuff, Protocol.LookAtBoard.BOARD_INDEX_OFFSET, boardIndex);
        response.setLength(Protocol.LookAtBoard.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    public synchronized void read(
            InetSocketAddress address, int gameId,
            int announcementId) throws IOException {
        Protocol.write(responseBuff, Protocol.Header.READ_ANNOUNCEMENT);
        Protocol.write(responseBuff, Protocol.ReadAnnouncement.GAME_ID_OFFSET, gameId);
        Protocol.write(responseBuff, Protocol.ReadAnnouncement.ANNOUNCEMENT_ID_OFFSET, announcementId);
        response.setLength(Protocol.ReadAnnouncement.BYTES);
        response.setSocketAddress(address);
        socket.send(response);
    }

    private static Client createClient(String[] args) throws SocketException, UnknownHostException {
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

        try (FileInputStream config = new FileInputStream("client.properties")) {
            Properties properties = new Properties();
            properties.load(config);
            serverAddress = new Address(
                properties.getProperty("serverIp"),
                Integer.parseInt(properties.getProperty("serverPort")));
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
                case "signal":
                    signal(tokens);
                    return true;
                case "contact":
                    contact(tokens);
                    return true;
                case "ask":
                    ask(tokens);
                    return true;
                case "post":
                    post(tokens);
                    return true;
                case "maintain":
                    maintain(tokens);
                    return true;
                case "remove":
                    remove(tokens);
                    return true;
                case "look":
                    look(tokens);
                    return true;
                case "read":
                    read(tokens);
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
        if(token.equals("server")) {
            return serverAddress.getInetSocketAddress();
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
        if (tokens[1].equals("local-address")) {
            System.out.println(
                "\rLocal address: "            +
                client.getLocalAddressString() + " ");
        }
        else {
            System.out.println("\rUnknown command ");
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

    private static void post(String[] tokens) throws IOException {
        client.post(
            getAddress("server"), gameId,
            client.getLocalIpInt(), client.getLocalPort(),
            tokens[1]);
    }

    private static void maintain(String[] tokens) throws IOException {
        client.maintain(getAddress("server"), gameId);
    }

    private static void remove(String[] tokens) throws IOException {
        client.remove(getAddress("server"), gameId);
    }

    private static void look(String[] tokens) throws IOException {
        client.look(getAddress("server"), gameId, Integer.parseInt(tokens[1]));
    }

    private static void read(String[] tokens) throws IOException {
        client.read(getAddress("server"), gameId, Integer.parseInt(tokens[1]));
    }

    private static void help() {
        System.out.println("\r1   # exit ");
        System.out.println("\r2   # show local-address ");
        System.out.println("\r3.1 # signal <ip>:<port> ");
        System.out.println("\r3.2 # signal server ");
        System.out.println("\r4.1 # contact <ip>:<port> ");
        System.out.println("\r4.2 # contact server ");
        System.out.println("\r5   # ask server");
        System.out.println("\r6   # post <announcement-name> ");
        System.out.println("\r7   # maintain ");
        System.out.println("\r8   # remove ");
        System.out.println("\r9   # look <board-index> ");
        System.out.println("\r10  # read <announcement-id> ");
    }
}
