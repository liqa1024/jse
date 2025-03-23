package jse.code;

import jse.Main;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检测任意引用是否被垃圾回收的检测器，用于实现一些会内存泄漏对象的自动回收；这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
@ApiStatus.Internal
public class ReferenceChecker extends WeakReference<Object> {
    protected ReferenceChecker(Object aObj) {
        super(aObj, getReferenceQueue());
        sPointers.add(this);
    }
    
    private volatile boolean mDisposed = false;
    public final boolean disposed() {return mDisposed;}
    public final synchronized void dispose() {
        if (!mDisposed) {
            mDisposed = true;
            dispose_();
            sPointers.remove(this);
        }
    }
    protected void dispose_() {}
    
    private final static ReferenceQueue<Object> sRefQueue = new ReferenceQueue<>();
    private final static Set<ReferenceChecker> sPointers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static ReferenceQueue<Object> getReferenceQueue() {
        cleanupWeakReferences();
        return sRefQueue;
    }
    private static void cleanupWeakReferences() {
        ReferenceChecker p = (ReferenceChecker)sRefQueue.poll();
        while (p != null) {
            p.dispose();
            p = (ReferenceChecker)sRefQueue.poll();
        }
    }
    private static void disposeAll() {
        Iterator<ReferenceChecker> it = sPointers.iterator();
        while (it.hasNext()) {
            ReferenceChecker ptr = it.next();
            /*
             * ptr.dispose() will remove from the set, so we remove it here
             * first to avoid ConcurrentModificationException
             */
            it.remove();
            ptr.dispose();
        }
    }
    // 在程序结束时清空所有检测器
    static {Main.addGlobalAutoCloseable(ReferenceChecker::disposeAll);}
}
