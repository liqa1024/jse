package jse.math.function;

import jse.math.vector.IVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * @author liqa
 * <p> 通用的数值函数接口，二维输入（f(x,y)）</p>
 */
public interface IFunc2 extends IFunc2Subs {
    /** 获取所有数据方便外部使用或者进行运算 */
    IVector x();
    IVector y();
    IVector f();
    
    /** 拷贝的接口 */
    IFunc2 copy();
    
    /** 获取结果，支持按照索引查找和按照 x, y 的值来查找 */
    double subs(double aX, double aY);
    double get(int aI, int aJ);
    /** 设置结果，简单起见只允许按照索引来设置 */
    void set(int aI, int aJ, double aV);
    
    /** 索引和 x, y 相互转换的接口 */
    int Nx();
    int Ny();
    double x0();
    double y0();
    double dx(int aI);
    double dy(int aJ);
    double getX(int aI);
    double getY(int aJ);
    void setX0(double aNewX0);
    void setY0(double aNewY0);
    
//    /** 还提供一个给函数专用的运算 */
//    IFunc1Operation operation();
//    @VisibleForTesting default IFunc1Operation opt() {return operation();}
    
    
    /** Groovy 的部分，重载一些运算符方便操作；圆括号为 x, y 值查找，方括号为索引查找 */
    @VisibleForTesting default double call(double aX, double aY) {return subs(aX, aY);}
    @VisibleForTesting default IFunc2Y_ getAt(final int aI) {
        return new IFunc2Y_() {
            @Override public double getAt(int aJ) {return get(aI, aJ);}
            @Override public void putAt(int aJ, double aV) {set(aI, aJ, aV);}
        };
    }
    
    /** 用来实现矩阵双重方括号索引，并且约束只能使用两个括号 */
    @ApiStatus.Internal interface IFunc2Y_ {
        @VisibleForTesting double getAt(int aJ);
        @VisibleForTesting void putAt(int aJ, double aV);
    }
}
