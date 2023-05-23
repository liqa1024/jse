package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 通用的矩阵切片器，接受 List 方便抽象的切片
 * @author liqa
 * @param <M> 被切片的矩阵类型
 * @param <V> 切片成一维的返回向量类型
 */
public interface IMatrixSlicer<M, V> {
    /**
     * 为了代码简洁（因为 List 的内容被擦除不能重载），因此只支持 Integer 来切片，
     * 并且实际切片过程也会将 Boolean 转成 Integer。
     * 还是使用多种输入排列组合方式来重载，可能会让实现比较复杂，但是是值得的
     */
    default M get(int[]         aSelectedRows, int[]         aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), UT.Code.asList(aSelectedCols));}
    default M get(List<Integer> aSelectedRows, int[]         aSelectedCols) {return getLL(aSelectedRows, UT.Code.asList(aSelectedCols));}
    default M get(int[]         aSelectedRows, List<Integer> aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), aSelectedCols);}
    default M get(List<Integer> aSelectedRows, List<Integer> aSelectedCols) {return getLL(aSelectedRows, aSelectedCols);}
    default M get(SliceType     aSelectedRows, int[]         aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(UT.Code.asList(aSelectedCols));}
    default M get(SliceType     aSelectedRows, List<Integer> aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(aSelectedCols);}
    default M get(int[]         aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(UT.Code.asList(aSelectedRows));}
    default M get(List<Integer> aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(aSelectedRows);}
    default M get(SliceType     aSelectedRows, SliceType     aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getAA();}
    default V get(int           aSelectedRow , int[]         aSelectedCols) {return getIL(aSelectedRow, UT.Code.asList(aSelectedCols));}
    default V get(int           aSelectedRow , List<Integer> aSelectedCols) {return getIL(aSelectedRow, aSelectedCols);}
    default V get(int           aSelectedRow , SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getIA(aSelectedRow);}
    default V get(int[]         aSelectedRows, int           aSelectedCol ) {return getLI(UT.Code.asList(aSelectedRows), aSelectedCol);}
    default V get(List<Integer> aSelectedRows, int           aSelectedCol ) {return getLI(aSelectedRows, aSelectedCol);}
    default V get(SliceType     aSelectedRows, int           aSelectedCol ) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAI(aSelectedCol);}
    
    
    
    String COL_MSG = "SelectedCols Must be int[] or List<Integer> or ALL";
    String ROL_MSG = "SelectedRows Must be int[] or List<Integer> or ALL";
    
    /** stuff to override */
    @ApiStatus.Internal V getIL(int aSelectedRow, List<Integer> aSelectedCols);
    @ApiStatus.Internal V getLI(List<Integer> aSelectedRows, int aSelectedCol);
    @ApiStatus.Internal V getIA(int aSelectedRow);
    @ApiStatus.Internal V getAI(int aSelectedCol);
    @ApiStatus.Internal M getLL(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    @ApiStatus.Internal M getLA(List<Integer> aSelectedRows);
    @ApiStatus.Internal M getAL(List<Integer> aSelectedCols);
    @ApiStatus.Internal M getAA();
}
