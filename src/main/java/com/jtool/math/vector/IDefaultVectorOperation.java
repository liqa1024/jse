package com.jtool.math.vector;


import org.jetbrains.annotations.ApiStatus;

/**
 * 矩阵一些额外运算的默认实现，原则上应该使用抽象类来实现，
 * 但是由于多继承的问题，为了避免重复代码，依旧放在接口中
 * @author liqa
 */
public interface IDefaultVectorOperation<V extends IVectorAny<?>, VS extends IVectorAny<V>> extends IVectorOperation<V> {
    /** 向量的一些额外的运算 */
    default V reverse() {return thisVector_().generator().from(refReverse());}
    default IVectorAny<?> refReverse() {
        return new AbstractVector() {
            private final VS mThis = thisVector_();
            @Override public double get_(int aIdx) {return mThis.get_(mThis.size()-1-aIdx);}
            @Override public void set_(int aIdx, double aValue) {mThis.set_(mThis.size()-1-aIdx, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return mThis.getAndSet_(mThis.size()-1-aIdx, aValue);}
            @Override public int size() {return mThis.size();}
        };
    }
    
    /** stuff to override */
    @ApiStatus.Internal VS thisVector_();
}
