package com.guan.atom;

/**
 * @author liqa
 * <p> 拥有正交的原子 XYZ 坐标信息的类型继承此接口，获取原子坐标信息以及正交模拟盒的信息，主要用于方便计算类来计算 </p>
 * <p> 约定数据使用矩阵存储，每行为不同的原子，每列为 xyz 坐标，即 orthogonalXYZ[atomIDX][xyz] </p>
 */
public interface IHasOrthogonalXYZ {
    double[][] orthogonalXYZ();
    double[] boxLo();
    double[] boxHi();
}
