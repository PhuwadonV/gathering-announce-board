import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class Protocol {
    // <editor-fold desc="">
    public static final int HEADER_BYTES           = Integer.BYTES;
    public static final int IP_BYTES               = Integer.BYTES;
    public static final int PORT_BYTES             = Integer.BYTES;
    public static final int VERSION_BYTES          = Integer.BYTES;
    public static final int ARRAY_LENGTH_BYTES     = Integer.BYTES;
    public static final int STRING_LENGTH_BYTES    = Integer.BYTES;
    public static final int GAME_ID_BYTES          = Integer.BYTES;
    public static final int STATUS_BYTES           = Integer.BYTES;
    public static final int BOARD_INDEX_BYTES      = Integer.BYTES;
    public static final int ANNOUNCEMENT_ID_BYTES  = IP_BYTES + PORT_BYTES;
    public static final int MAX_STRING_LENGTH      = 64;
    public static final int MAX_STRING_BYTES       = STRING_LENGTH_BYTES + MAX_STRING_LENGTH * 2;
    public static final int MAX_BUFFER_BYTES       = 65_535 - 8 - 20;

    public static final int SERVER_REQBUFF_BYTES   = MAX_BUFFER_BYTES;
    public static final int SERVER_RESBUFF_BYTES   = MAX_BUFFER_BYTES;
    public static final int CLIENT_REQBUFF_BYTES   = AnnouncementsOverview.BYTES;
    public static final int CLIENT_RESBUFF_BYTES   = PostAnnouncement.BYTES;

    public static final int SERVER_FREQUENT_HEADER = 1 << 30;
    public static final int CLIENT_FREQUENT_HEADER = 1 << 29;

    public static class Header {
        public static final int MAINTAIN_ANNOUNCEMENT  = SERVER_FREQUENT_HEADER | 1;
        public static final int LOOK_AT_BOARD          = SERVER_FREQUENT_HEADER | 2;

        public static final int ANNOUNCEMENT_STATUS    = CLIENT_FREQUENT_HEADER | 1;
        public static final int ANNOUNCEMENTS_OVERVIEW = CLIENT_FREQUENT_HEADER | 2;

        public static final int SIGNAL                 = 0;
        public static final int CONTACT                = 1;
        public static final int ACKNOWLEDGE            = 2;
        public static final int ASK                    = 3;
        public static final int ANSWER                 = 4;
        public static final int POST_ANNOUNCEMENT      = 5;
        public static final int REMOVE_ANNOUNCEMENT    = 6;
        public static final int READ_ANNOUNCEMENT      = 7;
        public static final int ANNOUNCEMENT_DETAIL    = 8;
    }
    // </editor-fold>
    // <editor-fold desc="Any -> Any">
    public static class Signal {
        public static final int BYTES = HEADER_BYTES;
    }

    public static class Contact {
        public static final int BYTES = HEADER_BYTES;
    }

    public static class Acknowledge {
        public static final int BYTES = HEADER_BYTES;
    }
    // </editor-fold>
    // <editor-fold desc="Client -> Server">
    public static class Ask {
        public static final int BYTES = HEADER_BYTES;
    }

    public static class PostAnnouncement {
        public static final int BYTES          = HEADER_BYTES + GAME_ID_BYTES + IP_BYTES + PORT_BYTES + MAX_STRING_BYTES;
        public static final int GAME_ID_OFFSET = HEADER_BYTES;
        public static final int LIP_OFFSET     = GAME_ID_OFFSET + GAME_ID_BYTES;
        public static final int LPORT_OFFSET   = LIP_OFFSET + IP_BYTES;
        public static final int NAME_OFFSET    = LPORT_OFFSET + PORT_BYTES;
    }

    public static class MaintainAnnouncement {
        public static final int BYTES          = HEADER_BYTES + GAME_ID_BYTES;
        public static final int GAME_ID_OFFSET = HEADER_BYTES;
    }

    public static class RemoveAnnouncement {
        public static final int BYTES          = HEADER_BYTES + GAME_ID_BYTES;
        public static final int GAME_ID_OFFSET = HEADER_BYTES;
    }

    public static class LookAtBoard {
        public static final int BYTES              = HEADER_BYTES + GAME_ID_BYTES + VERSION_BYTES + BOARD_INDEX_BYTES;
        public static final int GAME_ID_OFFSET     = HEADER_BYTES;
        public static final int VERSION_OFFSET     = GAME_ID_OFFSET + GAME_ID_BYTES;
        public static final int BOARD_INDEX_OFFSET = VERSION_OFFSET + VERSION_BYTES;
    }

    public static class ReadAnnouncement {
        public static final int BYTES                  = HEADER_BYTES + GAME_ID_BYTES + ANNOUNCEMENT_ID_BYTES;
        public static final int GAME_ID_OFFSET         = HEADER_BYTES;
        public static final int ANNOUNCEMENT_ID_OFFSET = GAME_ID_OFFSET + GAME_ID_BYTES;
    }
    // </editor-fold>
    // <editor-fold desc="Server -> Client">
    public static class Answer {
        public static final int BYTES        = HEADER_BYTES + IP_BYTES + PORT_BYTES;
        public static final int EIP_OFFSET   = HEADER_BYTES;
        public static final int EPORT_OFFSET = EIP_OFFSET + IP_BYTES;
    }

    public static class AnnouncementStatus {
        public static final int BYTES           = HEADER_BYTES + STATUS_BYTES;
        public static final int STATUS_OFFSET   = HEADER_BYTES;
        public static final int CREATED         = 0;
        public static final int ALREADY_EXISTS  = 1;
        public static final int MAINTAINED      = 2;
        public static final int NOT_EXISTS      = 3;
        public static final int REMOVED         = 4;
        public static final int BOARD_NO_CHANGE = 5;
    }

    public static class AnnouncementsOverview {
        public static final int ITEM_BYTES                  = ANNOUNCEMENT_ID_BYTES + MAX_STRING_BYTES;
        public static final int ITEM_ANNOUNCEMENT_ID_OFFSET = 0;
        public static final int ITEM_NAME_OFFSET            = ANNOUNCEMENT_ID_BYTES;
        public static final int ANNOUNCEMENT_PER_BOARD      = 5;
        public static final int BYTES                       = HEADER_BYTES + VERSION_BYTES + ARRAY_LENGTH_BYTES + ITEM_BYTES * ANNOUNCEMENT_PER_BOARD;
        public static final int VERSION_OFFSET              = HEADER_BYTES;
        public static final int LENGTH_OFFSET               = VERSION_OFFSET + VERSION_BYTES;
        public static final int ITEMS_OFFSET                = LENGTH_OFFSET + ARRAY_LENGTH_BYTES;
    }

    public static class AnnouncementDetail {
        public static final int BYTES        = HEADER_BYTES + (IP_BYTES + PORT_BYTES) * 2;
        public static final int EIP_OFFSET   = HEADER_BYTES;
        public static final int EPORT_OFFSET = EIP_OFFSET + IP_BYTES;
        public static final int LIP_OFFSET   = EPORT_OFFSET + PORT_BYTES;
        public static final int LPORT_OFFSET = LIP_OFFSET + IP_BYTES;
    }
    // </editor-fold>
    // <editor-fold desc="Data Conversion">
    public static void write(byte[] dst, int offset, int value) {
        dst[offset    ] = (byte)((value      ) & 0xFF);
        dst[offset + 1] = (byte)((value >>  8) & 0xFF);
        dst[offset + 2] = (byte)((value >> 16) & 0xFF);
        dst[offset + 3] = (byte)((value >> 24) & 0xFF);
    }

    public static void write(byte[] dst, int value) {
        write(dst, 0, value);
    }

    public static void write(byte[] dst, int offset, long value) {
        dst[offset    ] = (byte)((value      ) & 0xFFL);
        dst[offset + 1] = (byte)((value >>  8) & 0xFFL);
        dst[offset + 2] = (byte)((value >> 16) & 0xFFL);
        dst[offset + 3] = (byte)((value >> 24) & 0xFFL);
        dst[offset + 4] = (byte)((value >> 32) & 0xFFL);
        dst[offset + 5] = (byte)((value >> 40) & 0xFFL);
        dst[offset + 6] = (byte)((value >> 48) & 0xFFL);
        dst[offset + 7] = (byte)((value >> 56) & 0xFFL);
    }

    public static void write(byte[] dst, long value) {
        write(dst, 0, value);
    }

    public static void write(byte[] dst, int offset, String src) {
        byte[] data = src.getBytes();
        int byteLength = data.length;
        if (byteLength > MAX_STRING_BYTES) byteLength = MAX_STRING_BYTES;
        write(dst, offset, byteLength);
        System.arraycopy(data, 0, dst, offset + STRING_LENGTH_BYTES, byteLength);
    }

    public static void write(byte[] dst, String value) {
        write(dst, 0, value);
    }

    public static int readInt(byte[] data, int offset) {
        return
            (data[offset    ] & 0xFF)       |
            (data[offset + 1] & 0xFF) <<  8 |
            (data[offset + 2] & 0xFF) << 16 |
            (data[offset + 3] & 0xFF) << 24;
    }

    public static int readInt(byte[] data) {
        return readInt(data, 0);
    }

    public static long readLong(byte[] data, int offset) {
        return
            (data[offset    ] & 0xFFL)       |
            (data[offset + 1] & 0xFFL) <<  8 |
            (data[offset + 2] & 0xFFL) << 16 |
            (data[offset + 3] & 0xFFL) << 24 |
            (data[offset + 4] & 0xFFL) << 32 |
            (data[offset + 5] & 0xFFL) << 40 |
            (data[offset + 6] & 0xFFL) << 48 |
            (data[offset + 7] & 0xFFL) << 56;
    }

    public static long readLong(byte[] data) {
        return readLong(data, 0);
    }

    public static String readString(byte[] data, int offset) {
        int length = readInt(data, offset);
        return length == 0 ? "" : new String(data, offset + STRING_LENGTH_BYTES, readInt(data, offset));
    }

    public static String readString(byte[] data) {
        return readString(data, 0);
    }

    public static String readHexString(byte[] data, int offset) {
        StringBuilder hexString = new StringBuilder();
        for(int i = data.length - 1; i >= 0; i--) {
            int value = data[offset + i];
            int high = (value & 0xF0) >> 4;
            int low = value & 0x0F;
            high = high > 9 ? high + 55 : high + 48;
            low = low > 9 ? low + 55 : low + 48;
            hexString.append((char)high);
            hexString.append((char)low);
        }
        return hexString.toString();
    }

    public static String readHexString(byte[] data) {
        return readHexString(data, 0);
    }

    public static String readIp(byte[] data, int offset) {
        return
            (data[offset    ] & 0xFF) + "." +
            (data[offset + 1] & 0xFF) + "." +
            (data[offset + 2] & 0xFF) + "." +
            (data[offset + 3] & 0xFF);
    }

    public static String readIp(byte[] data) {
        return readIp(data, 0);
    }

    public static int hexStringToInt(String hexString) {
        int value = 0;
        for(int i = 0; i < Integer.BYTES; i++) {
            int charIndex = i * 2;
            int high = hexString.charAt(charIndex);
            int low = hexString.charAt(charIndex + 1);
            high = high > 64 ? high - 55 : high - 48;
            low = low > 64 ? low - 55 : low - 48;
            high <<= 28 - 8 * i;
            low <<= 24 - 8 * i;
            value |= high | low;
        }
        return value;
    }

    public static long hexStringToLong(String hexString) {
        long value = 0;
        for(int i = 0; i < Long.BYTES; i++) {
            int charIndex = i * 2;
            long high = hexString.charAt(charIndex);
            long low = hexString.charAt(charIndex + 1);
            high = high > 64 ? high - 55 : high - 48;
            low = low > 64 ? low - 55 : low - 48;
            high <<= 60 - (8 * i);
            low <<= 56 - (8 * i);
            value |= high | low;
        }
        return value;
    }
    // </editor-fold>
    // <editor-fold desc="Sorted protocol size">
    public static void main(String[] args) {
        List<Pair<Integer, String>> headers = new LinkedList<>();
        headers.add(new Pair<>(Signal.BYTES,                "SIGNAL                 :   Any  ->   Any "));
        headers.add(new Pair<>(Contact.BYTES,               "CONTACT                :   Any  ->   Any "));
        headers.add(new Pair<>(Acknowledge.BYTES,           "ACKNOWLEDGE            :   Any  ->   Any "));
        headers.add(new Pair<>(Ask.BYTES,                   "ASK                    : Client -> Server"));
        headers.add(new Pair<>(PostAnnouncement.BYTES,      "POST_ANNOUNCEMENT      : Client -> Server"));
        headers.add(new Pair<>(MaintainAnnouncement.BYTES,  "MAINTAIN_ANNOUNCEMENT  : Client -> Server"));
        headers.add(new Pair<>(RemoveAnnouncement.BYTES,    "REMOVE_ANNOUNCEMENT    : Client -> Server"));
        headers.add(new Pair<>(LookAtBoard.BYTES,           "LOOK_AT_BOARD          : Client -> Server"));
        headers.add(new Pair<>(ReadAnnouncement.BYTES,      "READ_ANNOUNCEMENT      : Client -> Server"));
        headers.add(new Pair<>(Answer.BYTES,                "ANSWER                 : Server -> Client"));
        headers.add(new Pair<>(AnnouncementStatus.BYTES,    "ANNOUNCEMENT_STATUS    : Server -> Client"));
        headers.add(new Pair<>(AnnouncementsOverview.BYTES, "ANNOUNCEMENTS_OVERVIEW : Server -> Client"));
        headers.add(new Pair<>(AnnouncementDetail.BYTES,    "ANNOUNCEMENT_DETAIL    : Server -> Client"));
        headers.stream()
            .sorted(Comparator.comparingInt(Pair::getItem1))
            .forEach(h -> System.out.println(h.getItem2() + " : " + h.getItem1()));
    }
    // </editor-fold>
}

class Pair<Item1, Item2> {
    private final Item1 item1;
    private final Item2 item2;

    public Pair(Item1 item1, Item2 item2) {
        this.item1 = item1;
        this.item2 = item2;
    }

    Item1 getItem1() {
        return item1;
    }

    Item2 getItem2() {
        return item2;
    }
}
