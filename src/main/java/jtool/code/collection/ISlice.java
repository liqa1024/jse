package jtool.code.collection;

import java.util.List;

/**
 * 用来切片输入的通用类
 * @author liqa
 */
public interface ISlice {
    ISlice ZL_SLICE = new ISlice() {
        @Override public int get(int aIdx) {throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));}
        @Override public int size() {return 0;}
    };
    
    int get(int aIdx);
    int size();
    
    /** 直接使用静态方法来通过其他类型转为 ISlice */
    static ISlice of(final int[] aIndices) {
        return new ISlice() {
            @Override public int get(int aIdx) {return aIndices[aIdx];}
            @Override public int size() {return aIndices.length;}
        };
    }
    static ISlice of(final List<Integer> aIndices) {
        return new ISlice() {
            @Override public int get(int aIdx) {return aIndices.get(aIdx);}
            @Override public int size() {return aIndices.size();}
        };
    }
    static ISlice of(final int aIndex) {
        return new ISlice() {
            @Override public int get(int aIdx) {
                if (aIdx != 0) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
                else return aIndex;
            }
            @Override public int size() {return 1;}
        };
    }
    static ISlice zl() {return ZL_SLICE;}
}
