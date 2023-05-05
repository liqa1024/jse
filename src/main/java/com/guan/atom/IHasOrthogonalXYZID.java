package com.guan.atom;

/**
 * @author liqa
 * <p> 拥有正交的原子 XYZ 坐标信息以及 ID 信息的类型继承此接口，用来获取原子坐标信息和 ID 以及正交模拟盒的信息，主要用于 MSD 之类的量来计算 </p>
 * <p> 约定数据使用矩阵存储，ID 不出现重复，每行为不同的原子，每列为 xyz 坐标和 id，即 orthogonalXYZID[atomIDX][x,y,z,id] </p>
 */
public interface IHasOrthogonalXYZID {
    double[][] orthogonalXYZID();
    double[] boxLo();
    double[] boxHi();
}
