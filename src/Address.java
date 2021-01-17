import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@SuppressWarnings({"unused"})
public class Address {
    private final int ipInt;
    private final byte[] ipBytes;
    private final InetAddress ip;
    private final String ipString;
    private final int port;
    private final InetSocketAddress socketAddress;
    private final String addressString;

    public Address(int ipInt, int port) throws UnknownHostException {
        this.port = port;
        this.ipInt = ipInt;
        ipBytes = new byte[Protocol.IP_BYTES];
        Protocol.write(ipBytes, ipInt);
        ip = InetAddress.getByAddress(ipBytes);
        ipString = Protocol.readIp(ipBytes);
        socketAddress = new InetSocketAddress(ip, port);
        addressString = ipString + ":" + port;
    }

    public Address(byte[] ipBytes, int port) throws UnknownHostException {
        this(Protocol.readInt(ipBytes), port);
    }

    public Address(InetAddress ip, int port) throws UnknownHostException {
        this(ip.getAddress(), port);
    }

    public Address(String ipString, int port) throws UnknownHostException {
        this(InetAddress.getByName(ipString), port);
    }

    public Address(InetSocketAddress socketAddress) throws UnknownHostException {
        this(socketAddress.getAddress(), socketAddress.getPort());
    }

    public int getIpInt() {
        return ipInt;
    }

    public byte[] getIpBytes() {
        return ipBytes;
    }

    public InetAddress getIp() {
        return ip;
    }

    public String getIpString() {
        return ipString;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress getInetSocketAddress() {
        return socketAddress;
    }

    @Override
    public String toString() {
        return addressString;
    }
}
