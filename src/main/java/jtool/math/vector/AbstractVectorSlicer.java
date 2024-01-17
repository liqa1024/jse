package jtool.math.vector;

import jtool.code.CS.SliceType;
import jtool.code.collection.ISlice;
import jtool.code.collection.NewCollections;
import jtool.code.functional.IIndexFilter;

import java.util.List;

public abstract class AbstractVectorSlicer implements IVectorSlicer {
    @Override public final IVector get(ISlice        aIndices) {return getL(aIndices);}
    @Override public final IVector get(int[]         aIndices) {return getL(ISlice.of(aIndices));}
    @Override public final IVector get(List<Integer> aIndices) {return getL(ISlice.of(aIndices));}
    @Override public final IVector get(SliceType     aIndices) {if (aIndices != SliceType.ALL) throw new IllegalArgumentException(MSG); return getA();}
    
    final static String MSG = "Slice Indices Must be a Filter or ISlice or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final IVector get(IIndexFilter aIndices) {return get(NewCollections.filterInteger(thisSize_(), aIndices));}
    
    
    /** stuff to override */
    protected abstract IVector getL(ISlice aIndices);
    protected abstract IVector getA();
    
    protected abstract int thisSize_();
}
