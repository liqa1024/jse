package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import com.jtool.code.iterator.IDoubleIterator;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractVectorSlicer implements IVectorSlicer {
    @Override public final IVector get(int[]         aIndices) {return getL(UT.Code.asList(aIndices));}
    @Override public final IVector get(List<Integer> aIndices) {return getL(aIndices);}
    @Override public final IVector get(SliceType     aIndices) {if (aIndices != SliceType.ALL) throw new IllegalArgumentException(MSG); return getA();}
    
    final static String MSG = "Slice Indices Must be a Filter or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final IVector get(IFilter          aIndices) {return get(F2L(aIndices));}
    @Override public final IVector get(IFilterWithIndex aIndices) {return get(F2L(aIndices));}
    
    List<Integer> F2L(IFilter aIndices) {
        List<Integer> rIndices = new ArrayList<>();
        IDoubleIterator it = thisIterator_();
        int idx = 0;
        while (it.hasNext()) {
            if (aIndices.accept(it.next())) rIndices.add(idx);
            ++idx;
        }
        return rIndices;
    }
    List<Integer> F2L(IFilterWithIndex aIndices) {
        List<Integer> rIndices = new ArrayList<>();
        IDoubleIterator it = thisIterator_();
        int idx = 0;
        while (it.hasNext()) {
            if (aIndices.accept(it.next(), idx)) rIndices.add(idx);
            ++idx;
        }
        return rIndices;
    }
    
    
    /** stuff to override */
    protected abstract IVector getL(List<Integer> aIndices);
    protected abstract IVector getA();
    
    protected abstract IDoubleIterator thisIterator_();
}
