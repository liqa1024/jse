package com.jtool.io;

import java.util.AbstractList;

/**
 * @author liqa
 * <p> 完全抽象的文件路径列表类，用来指定使用的文件路径名称格式，所有属性使用函数实现来方便重写 </p>
 */
public abstract class AbstractFilePathList extends AbstractList<String> {
    public abstract int start();
    public abstract int end();
    public abstract String filePath();
    
    // 默认根据 end 来决定文件路径名称格式
    protected boolean isSinglePath() {return end() < 0;}
    
    /** List stuffs */
    @Override public String get(int index) {
        if (isSinglePath()) {
            if (index != 0) throw new IndexOutOfBoundsException("index: "+index);
            return filePath();
        } else {
            if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("index: "+index);
            return filePath()+"-"+(start()+index);
        }
    }
    @Override public int size() {return isSinglePath() ? 1 : end()-start();}
}
