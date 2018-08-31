package org.michoko.lazyconnectionclient;

public final class CanbusMessageHelper {
    private static final int CAN_EFF_FLAG = 0x80000000; /* EFF/SFF is set in the MSB */
    private static final int CAN_RTR_FLAG = 0x40000000; /* remote transmission request */
    private static final int CAN_ERR_FLAG = 0x20000000; /* error message frame */
    private static final int CAN_ERR_MASK = 0x1FFFFFFF; /* omit EFF, RTR, ERR flags */

    public static CanbusMessage parseMessage(String msg) {
        try {
            String[] splittedMsg = msg.split(",");
            if (splittedMsg.length != 3) {
                return null;
            }
            int id = Integer.parseInt(splittedMsg[0], 16);
            byte length = Byte.parseByte(splittedMsg[1]);
            byte[] data = new byte[length];
            String[] splittedData = splittedMsg[2].split(" ");
            for (int i = 0; i < length; i++) {
                data[i] = (byte) (Short.parseShort(splittedData[i], 16) & 0xFF);
            }
            return new CanbusMessage.CanbusMessageBuilder()
                    .id(id & CAN_ERR_MASK)
                    .rtr((id & CAN_RTR_FLAG) == CAN_RTR_FLAG)
                    .extended((id & CAN_EFF_FLAG) == CAN_EFF_FLAG)
                    .error((id & CAN_ERR_FLAG) == CAN_ERR_FLAG)
                    .data(data)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    public static String toString(CanbusMessage msg) {
        StringBuilder b = new StringBuilder();

        int id = msg.getId();
        if (msg.isError()) {
            id |= CAN_ERR_FLAG;
        }
        if (msg.isExtended()) {
            id |= CAN_EFF_FLAG;
        }
        if (msg.isRtr()) {
            id |= CAN_RTR_FLAG;
        }
        b.append(Integer.toHexString(id).toUpperCase());
        b.append(',');
        b.append(msg.getData().length);
        b.append(',');
        b.append(dumpCanbusData(msg));
        return b.toString();
    }

    public static String dumpCanbusData(CanbusMessage msg) {
        StringBuilder b = new StringBuilder();
        byte[] data = msg.getData();
        for (int i = 0; i < data.length; i++) {
            b.append(Integer.toHexString(data[i] & 0xFF).toUpperCase());
            if (i < data.length - 1) {
                b.append(' ');
            }
        }
        return b.toString();
    }
}
