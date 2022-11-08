package sqlighter.page;

import java.lang.ref.SoftReference;
import java.util.Optional;

public class PageCacheFactory {

    private static final ThreadLocal<SoftReference<PageCache>> threadlocalPageCache = new ThreadLocal<>();


    public static PageCache getPageCache() {
        return Optional.ofNullable(threadlocalPageCache.get())
                .map(SoftReference::get)
                .orElseGet(() -> {
                    PageCache pageCache = new PageCache();
                    threadlocalPageCache.set(new SoftReference<>(pageCache));
                    return pageCache;
                });
    }
}
