package jse.math.vector;

import jse.code.CS.SliceType;
import jse.code.collection.ISlice;
import jse.code.collection.NewCollections;
import jse.code.functional.IIndexFilter;

import java.util.List;

public abstract class AbstractIntVectorSlicer implements IIntVectorSlicer {
    @Override public final IIntVector get(ISlice        aIndices) {return getL(aIndices);}
    @Override public final IIntVector get(int[]         aIndices) {return getL(ISlice.of(aIndices));}
    @Override public final IIntVector get(List<Integer> aIndices) {return getL(ISlice.of(aIndices));}
    @Override public final IIntVector get(SliceType     aIndices) {if (aIndices != SliceType.ALL) throw new IllegalArgumentException(MSG); return getA();}
    
    final static String MSG = "Slice Indices Must be a Filter or ISlice or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final IIntVector get(IIndexFilter aIndices) {return get(NewCollections.filterInteger(thisSize_(), aIndices));}
    
    
    /** stuff to override */
    protected abstract IIntVector getL(ISlice aIndices);
    protected abstract IIntVector getA();
    
    protected abstract int thisSize_();
}
