package jse.code.iterator;

import jse.atom.IAtom;
import jse.atom.IXYZ;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;

/**
 * 支持使用两个 double 输入的复数迭代器，用来减少外套类型的使用
 * <p>
 * 和 {@link IXYZ} 或者 {@link IAtom} 部分的用法思路不同，
 * 这里直接返回 {@link ComplexDouble} 让其和基本类型一样的使用，不会有引用的属性在里面
 * @author liqa
 */
public interface IComplexDoubleSetOnlyIterator {
    /** 额外添加使用两个 double 输入的设置接口 */
    void setReal(double aReal);
    void setImag(double aImag);
    /** 高性能接口，一次完成下一步和设置过程 */
    default void nextAndSetReal(double aReal) {
        nextOnly();
        setReal(aReal);
    }
    default void nextAndSetImag(double aImag) {
        nextOnly();
        setImag(aImag);
    }
    
    /** ISetOnlyIterator stuffs */
    boolean hasNext();
    void nextOnly();
    default void set(double aReal, double aImag) {
        setReal(aReal);
        setImag(aImag);
    }
    default void set(IComplexDouble aValue) {
        set(aValue.real(), aValue.imag());
    }
    default void set(ComplexDouble aValue) {
        set(aValue.mReal, aValue.mImag);
    }
    default void nextAndSet(double aReal, double aImag) {
        nextOnly();
        set(aReal, aImag);
    }
    default void nextAndSet(IComplexDouble aValue) {
        nextOnly();
        set(aValue.real(), aValue.imag());
    }
    default void nextAndSet(ComplexDouble aValue) {
        nextOnly();
        set(aValue.mReal, aValue.mImag);
    }
    /** IDoubleSetOnlyIterator like stuffs */
    default void set(double aValue) {
        set(aValue, 0.0);
    }
    default void nextAndSet(double aValue) {
        nextAndSet(aValue, 0.0);
    }
    
    /** 同样采用 toSetOnlyIterator() 的方法转为 {@link ISetOnlyIterator} 而不是继承 */
    default ISetOnlyIterator<ComplexDouble> toSetOnlyIterator() {
        return new ISetOnlyIterator<ComplexDouble>() {
            @Override public boolean hasNext() {return IComplexDoubleSetOnlyIterator.this.hasNext();}
            @Override public void nextOnly() {IComplexDoubleSetOnlyIterator.this.nextOnly();}
            @Override public void set(ComplexDouble aValue) {IComplexDoubleSetOnlyIterator.this.set(aValue);}
            @Override public void nextAndSet(ComplexDouble aValue) {IComplexDoubleSetOnlyIterator.this.nextAndSet(aValue);}
        };
    }
}
