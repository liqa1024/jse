package jse.code.collection;

import groovy.lang.EmptyRange;
import groovy.lang.Range;
import jse.code.functional.IUnaryFullOperator;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 原本的 MultiFrameXXX 的抽象类，
 * 现在不再继承 {@link List}，因为 List 的接口太脏了，
 * 但是提供了 List 基本一致的接口；
 * 并且提供了 groovy 相关接口来方便在 groovy 中依旧按照 List 一样的使用
 * @param <R> 返回的类型
 * @param <T> 输入的类型
 * @param <E> 内部 List 存储的类型
 * @author liqa
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class AbstractListWrapper<R, T, E> {
    protected List<E> mList;
    protected AbstractListWrapper(List<E> aList) {mList = aList;}
    
    /// stuff to override
    protected abstract E toInternal_(T aValue);
    protected abstract R toOutput_(E aValue);
    
    /// List stuffs
    public final R get(int aIdx) {return toOutput_(mList.get(aIdx));}
    public final int size() {return mList.size();}
    
    public final boolean add(T aValue) {return mList.add(toInternal_(aValue));}
    public final void add(int aIdx, T aValue) {mList.add(aIdx, toInternal_(aValue));}
    public final boolean addAll(Collection<? extends T> aList) {return mList.addAll(AbstractCollections.map(aList, this::toInternal_));}
    public final boolean addAll(int aIdx, Collection<? extends T> aList) {return mList.addAll(aIdx, AbstractCollections.map(aList, this::toInternal_));}
    public final R set(int aIdx, T aValue) {return toOutput_(mList.set(aIdx, toInternal_(aValue)));}
    public final R remove(int aIdx) {return toOutput_(mList.remove(aIdx));}
    public final void clear() {mList.clear();}
    
    public final List<R> asList() {return AbstractCollections.map(mList, this::toOutput_);}
    public final Iterator<R> iterator() {return AbstractCollections.map(mList.iterator(), this::toOutput_);}
    
    /// useful stuffs
    public final R removeLast() {return toOutput_(DefaultGroovyMethods.removeLast(mList));}
    public final R first() {return toOutput_(DefaultGroovyMethods.first(mList));}
    public final R last() {return toOutput_(DefaultGroovyMethods.last(mList));}
    public AbstractListWrapper<R, T, E> append(T aValue) {add(aValue); return this;}
    public AbstractListWrapper<R, T, E> appendAll(Collection<? extends T> aList) {addAll(aList); return this;}
    
    /// groovy stuffs
    @VisibleForTesting public final R getAt(int aIdx) {return toOutput_(DefaultGroovyMethods.getAt(mList, aIdx));}
    @VisibleForTesting public final void putAt(int aIdx, T aValue) {DefaultGroovyMethods.putAt(mList, aIdx, toInternal_(aValue));}
    @VisibleForTesting @SuppressWarnings("rawtypes") public final List<R> getAt(Range aRange) {return DefaultGroovyMethods.getAt(asList(), aRange);}
    @VisibleForTesting @SuppressWarnings("rawtypes") public final List<R> getAt(EmptyRange aRange) {return DefaultGroovyMethods.getAt(asList(), aRange);}
    @VisibleForTesting @SuppressWarnings("rawtypes") public final List<R> getAt(Collection aIndices) {return DefaultGroovyMethods.getAt(asList(), aIndices);}
    @VisibleForTesting public final List<R> collect() {return NewCollections.map(mList, this::toOutput_);}
    @VisibleForTesting public final <RR> List<RR> collect(final IUnaryFullOperator<RR, R> aTransform) {return NewCollections.map(mList, v -> aTransform.apply(toOutput_(v)));}
    @VisibleForTesting public AbstractListWrapper<R, T, E> leftShift(T aValue) {return append(aValue);}
    @VisibleForTesting public AbstractListWrapper<R, T, E> leftShift(Collection<? extends T> aList) {return appendAll(aList);}
}
