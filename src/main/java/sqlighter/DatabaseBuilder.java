package sqlighter;

import sqlighter.data.Record;
import sqlighter.page.Page;
import sqlighter.page.PageCacheFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The database builder is the main interface to create a database.
 */
public class DatabaseBuilder {

    private int pageSize = Database.PAGE_SIZE;
    private final List<Page> leafPages = new ArrayList<>();
    private Page currentPage;

    private SchemaRecord schemaRecord;

    private int nRecordsOnCurrentPage;

    public DatabaseBuilder() {
        createPage();
    }

    public DatabaseBuilder withPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public void addRecord(final Record record) {
        if (currentPageIsFull(record)) {
            finishCurrentPage();
            createPage();
        }
        currentPage.setKey(record.getRowId()); //gets updated until page is finished
        currentPage.putBackward(record.toBytes());
        currentPage.putU16(currentPage.getBackwardPosition());
        nRecordsOnCurrentPage += 1;
    }

    public void addSchema(String tableName, String ddl) {
        this.schemaRecord = new SchemaRecord(1, tableName, 2, ddl);
    }

    public Database build() {
        currentPage.setForwardPosition(Page.POSITION_CELL_COUNT);
        currentPage.putU16(nRecordsOnCurrentPage);

        if (nRecordsOnCurrentPage > 0) {
            currentPage.putU16(currentPage.getBackwardPosition());
        } else {
            currentPage.putU16(currentPage.getBackwardPosition() - 1);
        }

        return new Database(pageSize, schemaRecord, leafPages);
    }

    private boolean currentPageIsFull(Record record) {
        return currentPage.getBackwardPosition() - record.getDataLength() < currentPage.getForwardPosition() + 5;
    }

    private void finishCurrentPage() {
        currentPage.setForwardPosition(Page.POSITION_CELL_COUNT);
        currentPage.putU16(nRecordsOnCurrentPage);
        currentPage.putU16(currentPage.getBackwardPosition());
    }

    private void createPage() {
        currentPage = PageCacheFactory.getPageCache().getLeafPage();
        currentPage.setForwardPosition(8);
        leafPages.add(currentPage);
        nRecordsOnCurrentPage = 0;
    }
}
