package sqlighter.page;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class PageCache {

    protected final Queue<Page> leafPages = new LinkedBlockingQueue<>();
    protected final Queue<Page> interiorPages = new LinkedBlockingQueue<>();

    public Page getInteriorPage() {
        Page page = interiorPages.poll();
        if (page == null) {
            page = Page.newInterior();
        } else {
            page.reset();
        }
        return page;
    }

    public Page getLeafPage() {
        Page page = leafPages.poll();
        if (page == null) {
            page = Page.newLeaf();
        } else {
            page.reset();
        }
        return page;
    }

    public void release(Page page) {
        if (page.getType() == PageType.TABLE_INTERIOR) {
            interiorPages.add(page);
        } else if (page.getType() == PageType.TABLE_LEAF) {
            leafPages.add(page);
        }
    }
}
