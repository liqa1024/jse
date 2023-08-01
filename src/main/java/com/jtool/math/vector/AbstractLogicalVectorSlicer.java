package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import com.jtool.code.filter.IIndexFilter;

import java.util.List;

public abstract class AbstractLogicalVectorSlicer implements ILogicalVectorSlicer {
    @Override public final ILogicalVector get(int[]         aIndices) {return getL(UT.Code.asList(aIndices));}
    @Override public final ILogicalVector get(List<Integer> aIndices) {return getL(aIndices);}
    @Override public final ILogicalVector get(SliceType     aIndices) {if (aIndices != SliceType.ALL) throw new IllegalArgumentException(MSG); return getA();}
    
    final static String MSG = "Slice Indices Must be a Filter or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final ILogicalVector get(IIndexFilter aIndices) {return get(IIndexFilter.filter(thisSize_(), aIndices));}
    
    
    /** stuff to override */
    protected abstract ILogicalVector getL(List<Integer> aIndices);
    protected abstract ILogicalVector getA();
    
    protected abstract int thisSize_();
}
