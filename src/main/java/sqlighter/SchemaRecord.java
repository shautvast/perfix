package sqlighter;

import sqlighter.data.Record;
import sqlighter.data.Value;

/*
 * Is a record in the sqlites_schema table
 * and a special case of a Record
 * class is being used for both reading and writing
 *
 */
public class SchemaRecord {

    private final long rowid;
    private final String tableName;
    private long rootpage;
    private final String sql;

    public SchemaRecord(long rowid, String tableName, long rootpage, String sql) {
        this.rowid = rowid;
        this.tableName = tableName;
        this.rootpage = rootpage;
        this.sql = sql;
    }

    public SchemaRecord(long rowid, String tableName, long rootpage) {
       this(rowid, tableName, rootpage, null);
    }

    public String getTableName() {
        return tableName;
    }

    public long getRowid() {
        return rowid;
    }

    public long getRootpage() {
        return rootpage;
    }

    public String getSql() {
        return sql;
    }

    public void setRootpage(long rootpage) {
        this.rootpage = rootpage;
    }

    public Record toRecord(){
        Record record = new Record(rowid);
        record.addValue(Value.of("table"));
        record.addValue(Value.of(getTableName().toLowerCase()));
        record.addValue(Value.of(getTableName().toLowerCase()));
        record.addValue(Value.of(getRootpage()));
        record.addValue(Value.of(getSql()));
        return record;
    }
}
