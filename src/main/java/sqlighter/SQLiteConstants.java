package sqlighter;

/**
 * special values for SQLite.
 *
 * See <a href="https://sqlite.org/fileformat2.html">Database File Format </a>
 */
public class SQLiteConstants {
    public static final byte[] MAGIC_HEADER = new byte[]{0x53, 0x51, 0x4c, 0x69, 0x74, 0x65, 0x20, 0x66, 0x6f, 0x72, 0x6d, 0x61, 0x74, 0x20, 0x33, 0x00};

    public static final byte FILE_FORMAT_WRITE_VERSION = 1; // legacy
    public static final byte FILE_FORMAT_READ_VERSION = 1; // legacy
    public static final byte RESERVED_SIZE = 0;
    public static final byte MAX_EMBED_PAYLOAD_FRACTION = 0x40;
    public static final byte MIN_EMBED_PAYLOAD_FRACTION = 0x20;
    public static final byte LEAF_PAYLOAD_FRACTION = 0x20;
    public static final int FILECHANGE_COUNTER = 1;
    public static final int FREELIST_TRUNK_PAGE_HUMBER = 0;
    public static final int TOTAL_N_FREELIST_PAGES = 0;
    public static final int SCHEMA_COOKIE = 1;
    public static final int SQLITE_SCHEMAVERSION = 4;
    public static final int SUGGESTED_CACHESIZE = 0;
    public static final int LARGEST_ROOT_BTREE_PAGE = 0; // zero when not in auto-vacuum mode

    public static final int ENCODING_UTF8 = 1; // The database text encoding. A value of 1 means UTF-8. A value of 2 means UTF-16le. A value of 3 means UTF-16be.

    public static final int USER_VERSION = 0;
    public static final int VACUUM_MODE_OFF = 0; // not used
    public static final int APP_ID = 0;
    public static final byte[] FILLER = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    }; // 20 bytes for some future use
    public static final byte[] VERSION_VALID_FOR = {0, 0, 0x03, -123};
    public static final byte[] SQLITE_VERSION = {0x00, 0x2e, 0x5F, 0x1A};

    public static final short NO_FREE_BLOCKS = 0;


    public static final byte TABLE_LEAF_PAGE = 0x0d; //TODO enum?
    public static final byte TABLE_INTERIOR_PAGE = 0x05;
    public static final byte INDEX_LEAF_PAGE = 0x0a;
    public static final byte INDEX_INTERIOR_PAGE = 0x02;
}