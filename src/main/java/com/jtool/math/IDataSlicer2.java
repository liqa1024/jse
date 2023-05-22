package com.jtool.math;

import com.jtool.code.CS.SliceType;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 任意的通用的数据切片器，统一使用 List 方便抽象的切片
 * @author liqa
 */
public interface IDataSlicer2<M> {
    /** 一般来说，? 可以是 Integer 或者 Boolean */
    @SuppressWarnings("unchecked")
    default M get(List<?> aSelectedRows, List<?> aSelectedCols) {
        Object tSR0 = aSelectedRows.get(0);
        Object tSC0 = aSelectedCols.get(0);
        if (tSR0 instanceof Integer) {
            if (tSC0 instanceof Integer) return getII((List<Integer>)aSelectedRows, (List<Integer>)aSelectedCols);
            else if (tSC0 instanceof Boolean) return getIB((List<Integer>)aSelectedRows, (List<Boolean>)aSelectedCols);
            else throw new IllegalArgumentException("SelectedCols Must be List<Integer> or List<Boolean> or ALL");
        } else
        if (tSR0 instanceof Boolean) {
            if (tSC0 instanceof Integer) return getBI((List<Boolean>)aSelectedRows, (List<Integer>)aSelectedCols);
            else if (tSC0 instanceof Boolean) return getBB((List<Boolean>)aSelectedRows, (List<Boolean>)aSelectedCols);
            else throw new IllegalArgumentException("SelectedCols Must be List<Integer> or List<Boolean>");
        }
        else throw new IllegalArgumentException("SelectedRows Must be List<Integer> or List<Boolean> or ALL");
    }
    @SuppressWarnings("unchecked")
    default M get(SliceType aSelectedRows, List<?> aSelectedCols) {
        if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException("SelectedRows Must be List<Integer> or List<Boolean> or ALL");
        Object tSC0 = aSelectedCols.get(0);
        if (tSC0 instanceof Integer) return getAI((List<Integer>)aSelectedCols);
        else if (tSC0 instanceof Boolean) return getAB((List<Boolean>)aSelectedCols);
        else throw new IllegalArgumentException("SelectedCols Must be List<Integer> or List<Boolean> or ALL");
    }
    @SuppressWarnings("unchecked")
    default M get(List<?> aSelectedRows, SliceType aSelectedCols) {
        if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException("SelectedCols Must be List<Integer> or List<Boolean> or ALL");
        Object tSR0 = aSelectedRows.get(0);
        if (tSR0 instanceof Integer) return getIA((List<Integer>)aSelectedRows);
        else if (tSR0 instanceof Boolean) return getBA((List<Boolean>)aSelectedRows);
        else throw new IllegalArgumentException("SelectedRows Must be List<Integer> or List<Boolean> or ALL");
    }
    default M get(SliceType aSelectedRows, SliceType aSelectedCols) {
        if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException("SelectedRows Must be List<Integer> or List<Boolean> or ALL");
        if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException("SelectedCols Must be List<Integer> or List<Boolean> or ALL");
        return getAA();
    }
    
    /** stuff to override */
    @ApiStatus.Internal M getII(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    @ApiStatus.Internal M getIB(List<Integer> aSelectedRows, List<Boolean> aSelectedCols);
    @ApiStatus.Internal M getBI(List<Boolean> aSelectedRows, List<Integer> aSelectedCols);
    @ApiStatus.Internal M getBB(List<Boolean> aSelectedRows, List<Boolean> aSelectedCols);
    @ApiStatus.Internal M getAI(List<Integer> aSelectedCols);
    @ApiStatus.Internal M getAB(List<Boolean> aSelectedCols);
    @ApiStatus.Internal M getIA(List<Integer> aSelectedRows);
    @ApiStatus.Internal M getBA(List<Boolean> aSelectedRows);
    @ApiStatus.Internal M getAA();
}
