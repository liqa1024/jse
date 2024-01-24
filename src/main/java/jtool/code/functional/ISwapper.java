package jtool.code.functional;

import com.mastfrog.util.sort.Swapper;
import jtool.math.IDataShell;

/**
 * 还是需要包装一下添加实用接口，
 * 并且第三方的 functional 还是包装一下增加可替换性
 * @author liqa
 */
@FunctionalInterface
public interface ISwapper extends Swapper {
    void swap(int aIdx1, int aIdx2);
    
    default ISwapper merge(final ISwapper aAfter) {
        return (i, j) -> {
            this.swap(i, j);
            aAfter.swap(i, j);
        };
    }
    default ISwapper merge(final ISwapper aFirst, final ISwapper... aElse) {
        return (i, j) -> {
            this.swap(i, j);
            aFirst.swap(i, j);
            for (ISwapper tElse : aElse) tElse.swap(i, j);
        };
    }
    
    default ISwapper unshift(final int aShift) {
        return aShift==0 ? this : (i, j) -> swap(i-aShift, j-aShift);
    }
    default ISwapper unreverse(int aSize) {
        final int tSizeMM = aSize - 1;
        return (i, j) -> swap(tSizeMM-i, tSizeMM-j);
    }
    default ISwapper undata(IDataShell<?> aData) {
        if (aData.isReverse()) {
            return unshift(aData.internalDataShift()).unreverse(aData.internalDataSize());
        } else {
            return unshift(aData.internalDataShift());
        }
    }
}
