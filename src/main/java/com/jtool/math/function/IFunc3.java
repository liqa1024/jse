package com.jtool.math.function;

import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;


/**
 * @author liqa
 * <p> 通用的数值函数接口，三维输入（f(x,y,z)）</p>
 */
public interface IFunc3 extends IFunc3Subs {
    /** 获取所有数据方便外部使用或者进行运算 */
    IVector x();
    IVector y();
    IVector z();
    IVector f();
    
    /** 拷贝的接口 */
    IFunc3 copy();
    
    /** 获取结果，支持按照索引查找和按照 x, y, z 的值来查找 */
    double subs(double aX, double aY, double aZ);
    double get(int aI, int aJ, int aK);
    /** 设置结果，简单起见只允许按照索引来设置 */
    void set(int aI, int aJ, int aK, double aV);
    
    /** 不进行边界检测的版本，带入 x 的情况永远不会超过边界（周期边界或者固定值），因此只提供索引的情况 */
    double get_(int aI, int aJ, int aK);
    void set_(int aI, int aJ, int aK, double aV);
    
    /** 索引和 x, y, z 相互转换的接口 */
    int Nx();
    int Ny();
    int Nz();
    double x0();
    double y0();
    double z0();
    double dx();
    double dy();
    double dz();
    double getX(int aI);
    double getY(int aJ);
    double getZ(int aK);
    void setX0(double aNewX0);
    void setY0(double aNewY0);
    void setZ0(double aNewZ0);
    
//    /** 还提供一个给函数专用的运算 */
//    IFunc1Operation operation();
//    @VisibleForTesting default IFunc1Operation opt() {return operation();}
    
    
    /** Groovy 的部分，重载一些运算符方便操作；圆括号为 x, y 值查找，方括号为索引查找 */
    @VisibleForTesting default double call(double aX, double aY, double aZ) {return subs(aX, aY, aZ);}
    @VisibleForTesting default IFunc3YZ_ getAt(final int aI) {
        return aJ -> new IFunc3Z_() {
            @Override public double getAt(int aK) {return get(aI, aJ, aK);}
            @Override public void putAt(int aK, double aV) {set(aI, aJ, aK, aV);}
        };
    }
    
    /** 用来实现矩阵双重方括号索引，并且约束只能使用两个括号 */
    @ApiStatus.Internal interface IFunc3YZ_ {
        @VisibleForTesting IFunc3Z_ getAt(int aJ);
    }
    @ApiStatus.Internal interface IFunc3Z_ {
        @VisibleForTesting double getAt(int aK);
        @VisibleForTesting void putAt(int aK, double aV);
    }
}
