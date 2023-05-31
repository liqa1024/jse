package com.jtool.math.matrix;

import com.jtool.math.vector.Vector;

/**
 * @author liqa
 * <p> 一般的矩阵接口，返回矩阵类型为 {@link ColumnMatrix}，向量为 {@link Vector}，方便一般的使用 </p>
 */
public interface IMatrix extends IMatrixAny<ColumnMatrix, Vector> {

}
