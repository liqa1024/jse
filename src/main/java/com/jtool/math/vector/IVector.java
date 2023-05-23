package com.jtool.math.vector;


import com.jtool.math.operator.IOperator1Full;

import java.util.List;

/**
 * @author liqa
 * <p> 通用的向量接口，使用泛型来方便实现任意数据类型的向量 </p>
 * <p> 直接继承 List 方便使用 </p>
 */
public interface IVector<T extends Number> extends List<T>, IVectorGetter<T> {
    /** 转为兼容性更好的 double[] */
    double[] vec();
    
    /** 批量修改的接口 */
    void fill(Number aValue);
    void fillWith(Iterable<? extends Number> aList);
    void fillWith(IVectorGetter<? extends Number> aVectorGetter);
    
    /** 访问和修改部分，自带的接口；注意 set 已经被 List 占用，这里为了逻辑统一依旧提供一个自己的重载版本 */
    T get_(int aIdx);
    void set_(int aIdx, Number aValue);
    T getAndSet_(int aIdx, Number aValue); // 返回修改前的值
    int size();
    
    T get(int aIdx);
    T getAndSet(int aIdx, Number aValue);
    /** List 的 set 在这里是 getAndSet 的逻辑 */
    T set(int aIdx, Number aValue);
    /** 专门提供一个仅 set 的接口 */
    void setOnly(int aIdx, Number aValue);
}
