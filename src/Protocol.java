import java.util.List;
import java.util.LinkedList;

public class Protocol {
    //#region
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
    public static final int CLIENT_REQBUFF_BYTES   = Proxy.BYTES + AnnouncementsOverview.BYTES;
    public static final int CLIENT_RESBUFF_BYTES   = Proxy.BYTES + PostAnnouncement.BYTES;

    public static final int SERVER_FREQUENT_HEADER = 1 << 31;
    public static final int CLIENT_FREQUENT_HEADER = 1 << 30;

    public static class Header {
        public static final int MAINTAIN_ANNOUNCEMENT  = SERVER_FREQUENT_HEADER | 0;
        public static final int LOOK_AT_BOARD          = SERVER_FREQUENT_HEADER | 1;

        public static final int ANNOUNCEMENT_STATUS    = CLIENT_FREQUENT_HEADER | 0;
        public static final int ANNOUNCEMENTS_OVERVIEW = CLIENT_FREQUENT_HEADER | 1;

        public static final int PROXY                  = 0;
        public static final int FORWARD                = 1;
        public static final int SIGNAL                 = 2;
        public static final int CONTACT                = 3;
        public static final int ACKNOWLEDGE            = 4;
        public static final int ASK                    = 5;
        public static final int ANSWER                 = 6;
        public static final int TOLL                   = 7;
        public static final int TOLLWAY                = 8;
        public static final int SEND                   = 9;
        public static final int POST_ANNOUNCEMENT      = 10;
        public static final int REMOVE_ANNOUNCEMENT    = 11;
        public static final int READ_ANNOUNCEMENT      = 12;
        public static final int ANNOUNCEMENT_DETAIL    = 13;
    }
    //#endregion
    //#region Any -> Any
    public static class Proxy {
        public static final int BYTES          = HEADER_BYTES + IP_BYTES + PORT_BYTES;
        public static final int EIP_OFFSET     = HEADER_BYTES;
        public static final int EPORT_OFFSET   = EIP_OFFSET + IP_BYTES;
        public static final int CONTENT_OFFSET = EPORT_OFFSET + PORT_BYTES;
    }

    public static class Forward {
        public static final int BYTES          = HEADER_BYTES + IP_BYTES + PORT_BYTES;
        public static final int EIP_OFFSET     = HEADER_BYTES;
        public static final int EPORT_OFFSET   = EIP_OFFSET + IP_BYTES;
        public static final int CONTENT_OFFSET = EPORT_OFFSET + PORT_BYTES;
    }

    public static class Signal {
        public static final int BYTES = HEADER_BYTES;
    }

    public static class Contact {
        public static final int BYTES = HEADER_BYTES;
    }

    public static class Acknowledge {
        public static final int BYTES = HEADER_BYTES;
    }

    public static class Ask {
        public static final int BYTES = HEADER_BYTES;
    }

    public static class Answer {
        public static final int BYTES        = HEADER_BYTES + IP_BYTES + PORT_BYTES;
        public static final int EIP_OFFSET   = HEADER_BYTES;
        public static final int EPORT_OFFSET = EIP_OFFSET + IP_BYTES;
    }
    //#endregion
    //#region Client -> Client
    public static class Toll {
        public static final int BYTES        = HEADER_BYTES + IP_BYTES + PORT_BYTES + PORT_BYTES;
        public static final int EIP_OFFSET   = HEADER_BYTES;
        public static final int EPORT_OFFSET = EIP_OFFSET + IP_BYTES;
        public static final int LPORT_OFFSET = EPORT_OFFSET + PORT_BYTES;
    }

    public static class Tollway {
        public static final int BYTES             = HEADER_BYTES + IP_BYTES + PORT_BYTES + IP_BYTES + PORT_BYTES;
        public static final int FROM_EIP_OFFSET   = HEADER_BYTES;
        public static final int FROM_EPORT_OFFSET = FROM_EIP_OFFSET + IP_BYTES;
        public static final int TO_EIP_OFFSET     = FROM_EPORT_OFFSET + PORT_BYTES;
        public static final int TO_EPORT_OFFSET   = TO_EIP_OFFSET + IP_BYTES;
    }

    public static class Send {
        public static final int BYTES          = HEADER_BYTES + MAX_STRING_BYTES;
        public static final int MESSAGE_OFFSET = HEADER_BYTES;
    }
    //#endregion
    //#region Client -> Server
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
    //#endregion
    //#region Server -> Client
    public static class AnnouncementStatus {
        public static final int BYTES         = HEADER_BYTES + STATUS_BYTES;
        public static final int STATUS_OFFSET = HEADER_BYTES;
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
        public static final int BYTES        = HEADER_BYTES + IP_BYTES + PORT_BYTES + IP_BYTES + PORT_BYTES;
        public static final int EIP_OFFSET   = HEADER_BYTES;
        public static final int EPORT_OFFSET = EIP_OFFSET + IP_BYTES;
        public static final int LIP_OFFSET   = EPORT_OFFSET + PORT_BYTES;
        public static final int LPORT_OFFSET = LIP_OFFSET + IP_BYTES;
    }
    //#endregion
    //#region Read/Write data
    public static void write(byte[] dst, int offset, int value) {
        dst[offset    ] = (byte)((value >> 24) & 0xFF);
        dst[offset + 1] = (byte)((value >> 16) & 0xFF);
        dst[offset + 2] = (byte)((value >>  8) & 0xFF);
        dst[offset + 3] = (byte)((value      ) & 0xFF);
    }

    public static void write(byte[] dst, int offset, long value) {
        dst[offset    ] = (byte)((value >> 56) & 0xFFL);
        dst[offset + 1] = (byte)((value >> 48) & 0xFFL);
        dst[offset + 2] = (byte)((value >> 40) & 0xFFL);
        dst[offset + 3] = (byte)((value >> 32) & 0xFFL);
        dst[offset + 4] = (byte)((value >> 24) & 0xFFL);
        dst[offset + 5] = (byte)((value >> 16) & 0xFFL);
        dst[offset + 6] = (byte)((value >>  8) & 0xFFL);
        dst[offset + 7] = (byte)((value      ) & 0xFFL);
    }

    public static void write(byte[] dst, int offset, String src) {
        byte[] data = src.getBytes();
        int byteLength = data.length;
        if (byteLength > MAX_STRING_BYTES) byteLength = MAX_STRING_BYTES;
        write(dst, offset, byteLength);
        System.arraycopy(data, 0, dst, offset + STRING_LENGTH_BYTES, byteLength);
    }

    public static int readInt(byte[] data, int offset) {
        return
            (data[offset    ] & 0xFF) << 24 |
            (data[offset + 1] & 0xFF) << 16 |
            (data[offset + 2] & 0xFF) <<  8 |
            (data[offset + 3] & 0xFF);
    }

    public static int readInt(byte[] data) {
        return readInt(data, 0);
    }

    public static long readLong(byte[] data, int offset) {
        return
            (data[offset    ] & 0xFFL) << 56 |
            (data[offset + 1] & 0xFFL) << 48 |
            (data[offset + 2] & 0xFFL) << 40 |
            (data[offset + 3] & 0xFFL) << 32 |
            (data[offset + 4] & 0xFFL) << 24 |
            (data[offset + 5] & 0xFFL) << 16 |
            (data[offset + 6] & 0xFFL) <<  8 |
            (data[offset + 7] & 0xFFL);
    }

    public static long readLong(byte[] data) {
        return readLong(data, 0);
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

    public static String readString(byte[] data, int offset) {
        int length = readInt(data, offset);
        return length == 0 ? "" : new String(data, offset + STRING_LENGTH_BYTES, readInt(data, offset));
    }

    public static String readString(byte[] data) {
        return readString(data, 0);
    }
    //#endregion
    //#region Test
    public static void main(String[] args) {
        List<Pair<Integer, String>> headers = new LinkedList<>(); 
        headers.add(new Pair<>(Signal.BYTES,                "SIGNAL                 :   Any  ->   Any "));
        headers.add(new Pair<>(Contact.BYTES,               "CONTACT                :   Any  ->   Any "));
        headers.add(new Pair<>(Acknowledge.BYTES,           "ACKNOWLEDGE            :   Any  ->   Any "));
        headers.add(new Pair<>(Ask.BYTES,                   "ASK                    :   Any  ->   Any "));
        headers.add(new Pair<>(Answer.BYTES,                "ANSWER                 :   Any  ->   Any "));
        headers.add(new Pair<>(Toll.BYTES,                  "TOLL                   : Client -> Client"));
        headers.add(new Pair<>(Tollway.BYTES,               "TOLLWAY                : Client -> Client"));
        headers.add(new Pair<>(Send.BYTES,                  "SEND                   : Client -> Client"));
        headers.add(new Pair<>(PostAnnouncement.BYTES,      "POST_ANNOUNCEMENT      : Client -> Server"));
        headers.add(new Pair<>(MaintainAnnouncement.BYTES,  "MAINTAIN_ANNOUNCEMENT  : Client -> Server"));
        headers.add(new Pair<>(RemoveAnnouncement.BYTES,    "REMOVE_ANNOUNCEMENT    : Client -> Server"));
        headers.add(new Pair<>(LookAtBoard.BYTES,           "LOOK_AT_BOARD          : Client -> Server"));
        headers.add(new Pair<>(ReadAnnouncement.BYTES,      "READ_ANNOUNCEMENT      : Client -> Server"));
        headers.add(new Pair<>(AnnouncementStatus.BYTES,    "ANNOUNCEMENT_STATUS    : Server -> Client"));
        headers.add(new Pair<>(AnnouncementsOverview.BYTES, "ANNOUNCEMENTS_OVERVIEW : Server -> Client"));
        headers.add(new Pair<>(AnnouncementDetail.BYTES,    "ANNOUNCEMENT_DETAIL    : Server -> Client"));
        headers.stream()
            .sorted((l, r) -> r.getItem1() - l.getItem1())
            .forEach(h -> System.out.println(h.getItem2() + " : " + h.getItem1()));
    }
    //#endregion
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
