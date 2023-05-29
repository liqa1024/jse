package com.jtool.math.vector;


import org.jetbrains.annotations.ApiStatus;

/**
 * 矩阵一些额外运算的默认实现，原则上应该使用抽象类来实现，
 * 但是由于多继承的问题，为了避免重复代码，依旧放在接口中
 * @author liqa
 */
public interface IDefaultVectorOperation<V extends IVectorFull<?>, VS extends IVectorFull<V>> extends IVectorOperation<V> {
    /**/
    
    /** stuff to override */
    @ApiStatus.Internal VS thisVector_();
}
