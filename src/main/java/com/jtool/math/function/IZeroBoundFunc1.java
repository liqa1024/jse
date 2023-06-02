package com.jtool.math.function;

/**
 * @author liqa
 * <p> 一维数值函数的扩展，超出界限外使用 0 值，可以加速一些运算 </p>
 */
public interface IZeroBoundFunc1 extends IFunc1 {
    /** 提供额外的接口用于检测两端 */
    double zeroBoundL();
    double zeroBoundR();
}
