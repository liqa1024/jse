package com.jtool.math.table;

import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;

/**
 * 通用的列表类，仅实现列表的额外功能
 * @author liqa
 */
public interface ITable {
    boolean noHead();
    Collection<String> heads();
    
    /** Map like stuffs */
    IVector get(String aHead);
    boolean containsHead(String aHead);
    @SuppressWarnings("UnusedReturnValue")
    boolean setHead(String aOldHead, String aNewHead);
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting default IVector getAt(String aHead) {return get(aHead);}
    @VisibleForTesting default void putAt(String aOldHead, String aNewHead) {setHead(aOldHead, aNewHead);}
}
