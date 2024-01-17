package jtool.math.vector;

import jtool.code.CS.SliceType;
import jtool.code.collection.ISlice;
import jtool.code.collection.NewCollections;
import jtool.code.functional.IIndexFilter;

import java.util.List;

import static jtool.math.vector.AbstractVectorSlicer.MSG;

public abstract class AbstractLogicalVectorSlicer implements ILogicalVectorSlicer {
    @Override public final ILogicalVector get(ISlice        aIndices) {return getL(aIndices);}
    @Override public final ILogicalVector get(int[]         aIndices) {return getL(ISlice.of(aIndices));}
    @Override public final ILogicalVector get(List<Integer> aIndices) {return getL(ISlice.of(aIndices));}
    @Override public final ILogicalVector get(SliceType     aIndices) {if (aIndices != SliceType.ALL) throw new IllegalArgumentException(MSG); return getA();}
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final ILogicalVector get(IIndexFilter aIndices) {return get(NewCollections.filterInteger(thisSize_(), aIndices));}
    
    
    /** stuff to override */
    protected abstract ILogicalVector getL(ISlice aIndices);
    protected abstract ILogicalVector getA();
    
    protected abstract int thisSize_();
}
