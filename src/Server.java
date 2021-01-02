import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class Server implements Closeable, Runnable {
    private volatile boolean isRunning = true;
    private final DatagramSocket socket;
    private final DatagramPacket request;
    private final DatagramPacket response;
    private final byte[] requestBuff;
    private final byte[] responseBuff;

    public Server() throws IOException  {
        this(5000);
    }

    public Server(int port) throws IOException  {
        this(new InetSocketAddress("0.0.0.0", port));
    }

    public Server(InetSocketAddress address) throws IOException  {
        socket = new DatagramSocket(address);
        requestBuff = new byte[Protocol.SERVER_REQBUFF_BYTES];
        responseBuff = new byte[Protocol.SERVER_RESBUFF_BYTES];
        request = new DatagramPacket(requestBuff, requestBuff.length);
        response = new DatagramPacket(responseBuff, responseBuff.length);
    }

    @Override
    public void run() {
        while(isRunning) {
            try {
               socket.receive(request);
               process();
            }
            catch (IOException e) {
                System.out.println("\r" +  e.getMessage() + " ");
            }
        }
    }

    private void process() throws IOException {
        int header = Protocol.readInt(requestBuff, 0);
        if ((header & Protocol.SERVER_FREQUENT_HEADER) > 0) {
            switch(header) {
                case Protocol.Header.MAINTAIN_ANNOUNCEMENT:
                    System.out.println("\rMAINTAIN_ANNOUNCEMENT ");
                    break;
                case Protocol.Header.LOOK_AT_BOARD:
                    System.out.println("\rLOOK_AT_BOARD ");
                    break;
            }
        }
        else {
            switch(header) {
                case Protocol.Header.PROXY:
                case Protocol.Header.FORWARD:
                    {
                        int length = request.getLength();
                        System.arraycopy(requestBuff, 0, responseBuff, 0, length);
                        System.arraycopy(
                            request.getAddress().getAddress(), 0,
                            responseBuff, Protocol.Proxy.EIP_OFFSET,
                            Protocol.IP_BYTES);
                        Protocol.write(
                            responseBuff,
                            Protocol.Proxy.EPORT_OFFSET,
                            request.getPort());
                        response.setSocketAddress(new InetSocketAddress(
                            Protocol.readIp(requestBuff, Protocol.Proxy.EIP_OFFSET),
                            Protocol.readInt(requestBuff, Protocol.Proxy.EPORT_OFFSET)));
                        response.setLength(length);
                        socket.send(response);
                    }
                    break;
                case Protocol.Header.SIGNAL:
                    break;
                case Protocol.Header.CONTACT:
                    Protocol.write(
                        responseBuff,
                        0,
                        Protocol.Header.ACKNOWLEDGE);
                    response.setSocketAddress(request.getSocketAddress());
                    response.setLength(Protocol.Acknowledge.BYTES);
                    socket.send(response);
                    break;
                case Protocol.Header.ASK:
                    Protocol.write(
                        responseBuff,
                        0,
                        Protocol.Header.ANSWER);
                    Protocol.write(
                        responseBuff,
                        Protocol.Answer.EIP_OFFSET,
                        Protocol.readInt(request.getAddress().getAddress(), 0));
                    Protocol.write(
                        responseBuff,
                        Protocol.Answer.EPORT_OFFSET,
                        request.getPort());
                    response.setSocketAddress(request.getSocketAddress());
                    response.setLength(Protocol.Answer.BYTES);
                    socket.send(response);
                    break;
                case Protocol.Header.POST_ANNOUNCEMENT:
                    System.out.println("\rPOST_ANNOUNCEMENT ");
                    break;
                case Protocol.Header.REMOVE_ANNOUNCEMENT:
                    System.out.println("\rREMOVE_ANNOUNCEMENT ");
                    break;
                case Protocol.Header.READ_ANNOUNCEMENT:
                    System.out.println("\rREAD_ANNOUNCEMENT ");
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

    @Override
    public void close() {
        stop();
        socket.close();
    }

    private static Server createServer(String[] args) throws IOException {
        switch(args.length) {
            case 0:
                return new Server();
            case 1:
                return new Server(Integer.parseInt(args[0]));
            default:
                return new Server(new InetSocketAddress(args[1], Integer.parseInt(args[0])));
        }
    }

    public static void main(String[] args) throws IOException {
        try (Server server = createServer(args)) {
            server.run();
        }
    }
}