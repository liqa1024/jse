package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.collection.AbstractCollections;
import com.jtool.code.collection.NewCollections;
import com.jtool.code.filter.IIndexFilter;

import java.util.List;

import static com.jtool.math.vector.AbstractVectorSlicer.MSG;

public abstract class AbstractComplexVectorSlicer implements IComplexVectorSlicer {
    @Override public final IComplexVector get(int[]         aIndices) {return getL(AbstractCollections.from(aIndices));}
    @Override public final IComplexVector get(List<Integer> aIndices) {return getL(aIndices);}
    @Override public final IComplexVector get(SliceType     aIndices) {if (aIndices != SliceType.ALL) throw new IllegalArgumentException(MSG); return getA();}
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final IComplexVector get(IIndexFilter aIndices) {return get(NewCollections.filterInteger(thisSize_(), aIndices));}
    
    
    /** stuff to override */
    protected abstract IComplexVector getL(List<Integer> aIndices);
    protected abstract IComplexVector getA();
    
    protected abstract int thisSize_();
}
