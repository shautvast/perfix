package sqlighter.data;

import sqlighter.Varint;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Record in sqlite database.
 * Used for reading and writing.
 */
public final class Record {

    /**
     * Suppresses use of the rowIDSequence, to facilitate unittests
     */
    public static boolean useDefaultRowId = false;

    // start at 1

    private final long rowId;

    private final List<Value> values = new ArrayList<>(10);

    public Record(long rowId) {
        this.rowId = rowId;
    }

    public void addValues(Value... values) {
        this.values.addAll(Arrays.asList(values));
    }

    public void addValue(Value value) {
        this.values.add(value);
    }

    /**
     * write the record to an array of bytes
     */
    public byte[] toBytes() {
        int dataLength = getDataLength();
        byte[] lengthBytes = Varint.write(dataLength);
        byte[] rowIdBytes = Varint.write(rowId);

        ByteBuffer buffer = ByteBuffer.allocate(lengthBytes.length + rowIdBytes.length + dataLength);
        buffer.put(lengthBytes);
        buffer.put(rowIdBytes);

        // 'The initial portion of the payload that does not spill to overflow pages.'
        int lengthOfEncodedColumnTypes = values.stream().map(Value::getDataType).mapToInt(ar -> ar.length).sum() + 1;
        buffer.put(Varint.write(lengthOfEncodedColumnTypes));

        //types
        for (Value value : values) {
            value.writeType(buffer);
        }

        //values
        for (Value value : values) {
            value.writeValue(buffer);
        }

        return buffer.array();
    }

    public int getDataLength() {
        return values.stream().mapToInt(Value::getLength).sum() + 1;
    }

    public long getRowId() {
        return rowId;
    }

    @SuppressWarnings("unused")
    public List<Value> getValues() {
        return values;
    }

    /**
     * returns the value at the specified column index (0 based)
     */
    public Value getValue(int column) {
        return values.get(column);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return rowId == record.rowId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowId);
    }
}
