package jse.parallel;


import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static jse.code.CS.RNGSUS;

/**
 * @author liqa
 * <p> 在随机位置开始的 List，用来在高并行的情况下打乱 List 的访问顺序，
 * 减少线程竞争同一个资源的概率 </p>
 * <p> 可以手动指定开始的位置，创建完成后起始位置都是确定的 </p>
 */
@Deprecated
public class RandomStartList<T> implements Iterable<T> {
    private final List<T> mList;
    private final int mStart;
    
    public RandomStartList(List<T> aList) {
        if (aList.isEmpty()) throw new IllegalArgumentException("input List cannot be empty");
        mList = aList; mStart = RNGSUS.nextInt(mList.size());
    }
    public RandomStartList(List<T> aList, long aStart) {
        if (aList.isEmpty()) throw new IllegalArgumentException("input List cannot be empty");
        mList = aList; mStart = (int) (aStart % aList.size()); // aStart 可以是任意正数
    }
    
    @Override @NotNull
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private ListIterator<T> mListIt = mList.listIterator(mStart);
            private T mNext = mListIt.next(); // List 一定不为空，因此这个一定合法，用于翻越结束点
            
            @Override public boolean hasNext() {
                if (mNext != null) return true;
                if (!mListIt.hasNext()) mListIt = mList.listIterator(0); // 周期边界条件，注意此时相当于没有执行 next
                if (mListIt.nextIndex() == mStart) return false;
                mNext = mListIt.next();
                return true;
            }
            @Override public T next() {
                if (hasNext()) {
                    T tNext = mNext;
                    mNext = null;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
