package com.jtool.math.table;

import com.jtool.math.vector.IVector;

import java.util.AbstractList;
import java.util.Collection;

/**
 * @author liqa
 * <p> 抽象的拥有多个帧的列表的类，方便子类实现接口 </p>
 * <p> 注意这里每一帧的 Table 都是完全独立的，继承 ITable 只是为了方便只有一帧的情况直接使用 </p>
 */
public abstract class AbstractMultiFrameTable<T extends ITable> extends AbstractList<T> implements ITable {
    /** ITable stuffs */
    @Override public boolean noHead() {return defaultFrame().noHead();}
    @Override public Collection<String> heads() {return defaultFrame().heads();}
    @Override public IVector get(String aHead) {return defaultFrame().get(aHead);}
    @Override public boolean containsHead(String aHead) {return defaultFrame().containsHead(aHead);}
    @Override public boolean setHead(String aOldHead, String aNewHead) {return defaultFrame().setHead(aOldHead, aNewHead);}
    
    /** stuff to override */
    public T defaultFrame() {return get(0);}
    
    /** AbstractList stuffs */
    public abstract T get(int index);
    public abstract int size();
}
