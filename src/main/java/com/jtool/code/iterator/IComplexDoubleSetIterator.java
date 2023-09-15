package com.jtool.code.iterator;

import com.jtool.atom.IAtom;
import com.jtool.atom.IXYZ;
import com.jtool.math.ComplexDouble;

import java.util.function.Consumer;


/**
 * 支持使用两个 double 输入的复数迭代器，用来减少外套类型的使用
 * <p>
 * 和 {@link IXYZ} 或者 {@link IAtom} 部分的用法思路不同，
 * 这里直接返回 {@link ComplexDouble} 让其和基本类型一样的使用，不会有引用的属性在里面
 * @author liqa
 */
public interface IComplexDoubleSetIterator extends IComplexDoubleIterator, IComplexDoubleSetOnlyIterator {
    void nextOnly();
    
    /** 同样采用 toSetIterator() 的方法转为 {@link ISetIterator} 而不是继承 */
    default ISetIterator<ComplexDouble> toSetIterator() {
        return new ISetIterator<ComplexDouble>() {
            @Override public boolean hasNext() {return IComplexDoubleSetIterator.this.hasNext();}
            @Override public ComplexDouble next() {return IComplexDoubleSetIterator.this.next();}
            
            @Override public void remove() {IComplexDoubleSetIterator.this.remove();}
            @Override public void forEachRemaining(Consumer<? super ComplexDouble> action) {IComplexDoubleSetIterator.this.forEachRemaining(action::accept);}
            
            @Override public void nextOnly() {IComplexDoubleSetIterator.this.nextOnly();}
            @Override public void set(ComplexDouble aValue) {IComplexDoubleSetIterator.this.set(aValue);}
            @Override public void nextAndSet(ComplexDouble aValue) {IComplexDoubleSetIterator.this.nextAndSet(aValue);}
        };
    }
}
