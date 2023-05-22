package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.math.IDataGenerator2;
import com.jtool.math.IDataSlicer2;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 在原本的矩阵接口上扩展更多高级功能 </p>
 */
public interface IMatrixFull<T extends Number, M extends IMatrix<T>> extends IMatrix<T> {
    /** 获得基于自身的矩阵生成器，方便构造相同大小的同样的矩阵 */
    IDataGenerator2<M> generator();
    @VisibleForTesting default IDataGenerator2<M> gen() {return generator();}
    
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果，TODO 以后需要支持向量切片 */
    IDataSlicer2<M> slicer();
    IDataSlicer2<IMatrix<T>> refSlicer();
    
    /** Groovy 的部分，重载一些运算符方便操作；为了让代码简洁，这里只增加使用 call 来进行切片的操作 */
    @VisibleForTesting default M call(List<?>   aSelectedRows, List<?>   aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting default M call(SliceType aSelectedRows, List<?>   aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting default M call(List<?>   aSelectedRows, SliceType aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting default M call(SliceType aSelectedRows, SliceType aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
}
