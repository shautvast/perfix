package sqlighter.data;

import sqlighter.Varint;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/*
 * NB Value classes derive their equality from their identity. I.e. no equals/hashcode
 */
public class Value {
    private static final byte FLOAT_TYPE = 7;

    protected final byte[] type;
    protected final byte[] value;
    protected final int length;

    protected Value(byte[] type, byte[] value) {
        this.type = type;
        this.value = value;
        this.length = type.length + value.length;
    }

    /**
     * Returns the length of serialType + the length of the value
     */
    public int getLength() {
        return length;
    }

    public void writeType(ByteBuffer buffer) {
        buffer.put(type);
    }

    public byte[] getDataType() {
        return type;
    }

    public void writeValue(ByteBuffer buffer) {
        buffer.put(value);
    }

    public byte[] getValue() {
        return value;
    }


    public static Value of(String value) {
        return new Value(Varint.write(value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length * 2L + 13),
                value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    public static Value of(long value) {
        byte[] valueAsBytes = getValueAsBytes(value);
        return new Value(getIntegerType(value, valueAsBytes.length), valueAsBytes);
    }

    public static Value of(double value) {
        return new Value(new byte[]{FLOAT_TYPE}, ByteBuffer.wrap(new byte[8]).putDouble(0, value).array());
    }

    public static Value of(byte[] value) {
        return new Value(Varint.write(value.length * 2L + 12), value);
    }

    public static byte[] getIntegerType(long value, int bytesLength) {
        if (value == 0) {
            return new byte[]{8};
        } else if (value == 1) {
            return new byte[]{9};
        } else {
            if (bytesLength < 5) {
                return Varint.write(bytesLength);
            } else if (bytesLength < 7) {
                return Varint.write(5);
            } else return Varint.write(6);
        }
    }

    /*
     * static because it's used in the constructor
     */
    public static byte[] getValueAsBytes(long value) {
        if (value == 0) {
            return new byte[0];
        } else if (value == 1) {
            return new byte[0];
        } else {
            return longToBytes(value, getLengthOfByteEncoding(value));
        }
    }

    public static int getLengthOfByteEncoding(long value) {
        long u;
        if (value < 0) {
            u = ~value;
        } else {
            u = value;
        }
        if (u <= 127) {
            return 1;
        } else if (u <= 32767) {
            return 2;
        } else if (u <= 8388607) {
            return 3;
        } else if (u <= 2147483647) {
            return 4;
        } else if (u <= 140737488355327L) {
            return 6;
        } else {
            return 8;
        }
    }

    public static byte[] longToBytes(long n, int nbytes) {
        byte[] b = new byte[nbytes];
        for (int i = 0; i < nbytes; i++) {
            b[i] = (byte) ((n >> (nbytes - i - 1) * 8) & 0xFF);
        }

        return b;
    }
}
