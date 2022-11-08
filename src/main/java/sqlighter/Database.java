package sqlighter;

import sqlighter.page.Page;
import sqlighter.page.PageCacheFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

import static sqlighter.SQLiteConstants.*;

/**
 * Limited to one table. As this is the main use case, this will probably not change.
 * Please note that you can put whatever in it (does not have to reflect the actual source database structure),
 * including for example the result of a complex join
 * major #1 TODO find a way to handle sizes that don't fit into memory, like overflow to file or sth
 */
public class Database {

    public static short PAGE_SIZE = 8192;

    private final SchemaRecord schema;
    public final int pageSize;

    final List<Page> leafPages;

    private int pageCounter = 3;

    /*
     * assumes 1 schema record ie 1 table. This might not change
     */
    public Database(int pageSize, SchemaRecord schemaRecord, List<Page> leafPages) {
        this.pageSize = pageSize;
        this.schema = schemaRecord;
        this.leafPages = leafPages;
    }

    public void write(String filename) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(filename)) {
            write(outputStream);
        }
    }

    public void write(WritableByteChannel channel) throws IOException {
        List<? extends Page> currentTopLayer = this.leafPages;
        int nPages = currentTopLayer.size();
        while (currentTopLayer.size() > 1) { // interior page needed?
            currentTopLayer = createInteriorPages(currentTopLayer);
            nPages += currentTopLayer.size();
        }
        assert !currentTopLayer.isEmpty();
        Page tableRootPage = currentTopLayer.get(0); //
        channel.write(createHeaderPage(nPages + 1).getDataBuffer());
        setChildReferencesAndWrite(tableRootPage, channel);
        channel.close();
    }

    public void write(OutputStream outputStream) throws IOException {
        List<? extends Page> currentTopLayer = this.leafPages;
        int nPages = currentTopLayer.size();
        while (currentTopLayer.size() > 1) { // interior page needed?
            currentTopLayer = createInteriorPages(currentTopLayer);
            nPages += currentTopLayer.size();
        }
        assert !currentTopLayer.isEmpty();
        Page tableRootPage = currentTopLayer.get(0); //
        outputStream.write(createHeaderPage(nPages + 1).getData());
        setChildReferencesAndWrite(tableRootPage, outputStream);
        outputStream.close();
    }

    private void setChildReferencesAndWrite(Page page, OutputStream outputStream) {
        if (page.isInterior()) {
            setChildReferences(page);
        }
        write(page, outputStream);
        PageCacheFactory.getPageCache().release(page);
        //recurse
        page.getChildren().forEach(child -> setChildReferencesAndWrite(child, outputStream));
    }

    private void setChildReferencesAndWrite(Page page, WritableByteChannel channel) {
        if (page.isInterior()) {
            setChildReferences(page);
        }
        write(page, channel);
        PageCacheFactory.getPageCache().release(page);
        //recurse
        page.getChildren().forEach(child -> setChildReferencesAndWrite(child, channel));
    }

    private void write(Page page, OutputStream outputStream) {
        try {
            outputStream.write(page.getData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(Page page, WritableByteChannel channel) {
        try {
            channel.write(page.getDataBuffer());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void setChildReferences(Page page) {
        page.setForwardPosition(Page.POSITION_CELL_COUNT);
        page.putU16(page.getChildren().size() - 1);

        for (int i = 0; i < page.getChildren().size() - 1; i++) { // except right-most pointer
            page.setForwardPosition(Page.START + i * 2);
            int position = page.getU16(); // read the position that was written in an earlier pass
            page.setForwardPosition(position); // go to the cell at that location
            page.putU32(pageCounter++); // add page reference
        }
        page.setForwardPosition(Page.POSITION_RIGHTMOST_POINTER);
        page.putU32(pageCounter++);
    }

    private Page createHeaderPage(int nPages) {
        Page headerPage = Page.newHeader(pageSize);
        writeHeader(headerPage, nPages);
        int payloadLocationWriteLocation = headerPage.getForwardPosition(); // mark current position

        int payloadLocation = writeSchema(headerPage, schema); //write schema payload from the end
        headerPage.setForwardPosition(payloadLocationWriteLocation); // go back to marked position
        headerPage.putU16(payloadLocation); //payload start
        headerPage.skipForward(1); // the number of fragmented free bytes within the cell content area
        headerPage.putU16(payloadLocation); // first cell
        return headerPage;
    }

    private int writeSchema(Page rootPage, SchemaRecord schemaRecord) {
        rootPage.putBackward(schemaRecord.toRecord().toBytes());
        return rootPage.getBackwardPosition();
    }

    private List<Page> createInteriorPages(List<? extends Page> childPages) {
        List<Page> interiorPages = new ArrayList<>();
        Page interiorPage = PageCacheFactory.getPageCache().getInteriorPage();
        interiorPage.setKey(childPages.stream().mapToLong(Page::getKey).max().orElse(-1));
        interiorPage.setForwardPosition(Page.START);
        int pageIndex;
        for (pageIndex = 0; pageIndex < childPages.size() - 1; pageIndex++) {
            Page leafPage = childPages.get(pageIndex);
            if (interiorPage.getBackwardPosition() < interiorPage.getForwardPosition() + 10) {
                interiorPage.setForwardPosition(Page.START_OF_CONTENT_AREA);
                interiorPage.putU16(interiorPage.getBackwardPosition());
                interiorPage.skipForward(5);

                interiorPages.add(interiorPage);

                interiorPage = PageCacheFactory.getPageCache().getInteriorPage();
                interiorPage.setForwardPosition(Page.START);
            }
            addCellWithPageRef(interiorPage, leafPage);
            interiorPage.addChild(leafPage);
        }

        // write start of payload
        interiorPage.setForwardPosition(Page.START_OF_CONTENT_AREA);
        interiorPage.putU16(interiorPage.getBackwardPosition());
        interiorPage.skipForward(5);
        interiorPage.addChild(childPages.get(pageIndex));
        interiorPages.add(interiorPage);
        return interiorPages;
    }

    private void addCellWithPageRef(Page interiorPage, Page leafPage) {
        byte[] keyAsBytes = Varint.write(leafPage.getKey());
        ByteBuffer cell = ByteBuffer.allocate(6 + keyAsBytes.length);
        cell.position(5);
        cell.put(keyAsBytes);

        // write cell to page, starting at the end
        interiorPage.putBackward(cell.array());
        interiorPage.putU16(interiorPage.getBackwardPosition());
    }

    private void writeHeader(Page rootpage, int nPages) {
        rootpage.putU8(MAGIC_HEADER);
        rootpage.putU16(rootpage.size());
        rootpage.putU8(FILE_FORMAT_WRITE_VERSION);
        rootpage.putU8(FILE_FORMAT_READ_VERSION);
        rootpage.putU8(RESERVED_SIZE);
        rootpage.putU8(MAX_EMBED_PAYLOAD_FRACTION);
        rootpage.putU8(MIN_EMBED_PAYLOAD_FRACTION);
        rootpage.putU8(LEAF_PAYLOAD_FRACTION);
        rootpage.putU32(FILECHANGE_COUNTER);
        rootpage.putU32(nPages);// file size in pages
        rootpage.putU32(FREELIST_TRUNK_PAGE_HUMBER);// Page number of the first freelist trunk page.
        rootpage.putU32(TOTAL_N_FREELIST_PAGES);
        rootpage.putU32(SCHEMA_COOKIE);
        rootpage.putU32(SQLITE_SCHEMAVERSION);
        rootpage.putU32(SUGGESTED_CACHESIZE);
        rootpage.putU32(LARGEST_ROOT_BTREE_PAGE);
        rootpage.putU32(ENCODING_UTF8);
        rootpage.putU32(USER_VERSION);
        rootpage.putU32(VACUUM_MODE_OFF);// True (non-zero) for incremental-vacuum mode. False (zero) otherwise.
        rootpage.putU32(APP_ID);// Application ID
        rootpage.putU8(FILLER);// Reserved for expansion. Must be zero.
        rootpage.putU8(VERSION_VALID_FOR);// The version-valid-for number
        rootpage.putU8(SQLITE_VERSION);// SQLITE_VERSION_NUMBER
        rootpage.putU8(TABLE_LEAF_PAGE); // leaf table b-tree page for schema
        rootpage.putU16(NO_FREE_BLOCKS); // zero if there are no freeblocks
        rootpage.putU16(1); // the number of cells on the page
    }

}
