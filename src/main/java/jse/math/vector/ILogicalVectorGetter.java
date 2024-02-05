package jse.math.vector;


import jse.code.functional.IIndexFilter;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * 需要同时继承 {@link IIndexFilter} 来简单实现使用其来切片等，
 * 并且可以避免 lambda 表达式的重载造成的冲突
 * @author liqa
 */
@FunctionalInterface
public interface ILogicalVectorGetter extends IIndexFilter {
    boolean get(int aIdx);
    
    @VisibleForTesting default boolean accept(int aIdx) {return get(aIdx);}
}
