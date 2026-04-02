package jse.atom;

import jse.code.collection.AbstractRandomAccessList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * 通用的可能包含元素符号的接口，集成一些常用方法来方便根据内部的元素符号来进行种类映射
 * @see IAtomData
 * @see IPotential
 * @author liqa
 */
public interface IHasSymbol {
    /** @return 原子种类的总数 */
    int ntypes();
    
    /** @return 是否包含元素符号信息 */
    boolean hasSymbol();
    /**
     * 直接获取指定种类编号下的元素符号，可以避免一次创建匿名列表的过程；
     * 有关系 {@code symbol(type) == symbols()[type-1]}
     * @param aType 需要查询的种类编号，从 {@code 1} 开始
     * @return 特定种类编号对应的元素符号，如果不存在元素符号信息则返回 {@code null}
     */
    @Nullable String symbol(int aType);
    /**
     * @return 按照原子种类编号排序的元素符号列表，有
     * {@code symbols().size() == ntypes()}；
     * 如果不存在元素符号信息则会返回 {@code null}
     */
    default @Nullable List<@Nullable String> symbols() {
        if (!hasSymbol()) return null;
        return new AbstractRandomAccessList<@Nullable String>() {
            @Override public @Nullable String get(int index) {return symbol(index+1);}
            @Override public int size() {return ntypes();}
        };
    }
    
    /// utils
    /**
     * 通过内部的 {@link #symbols()} 来获取关于另一个 {@link IAtomData} 的种类编号映射
     * @param aAtomData 需要获取种类编号映射的原子数据
     * @return 一个种类编号映射，输入原始的 aAtomData 的原子种类编号，返回对应 this 的种类编号
     * @throws UnsupportedOperationException 当不存在 {@link #symbols()} 数据
     */
    default IntUnaryOperator typeMap(IAtomData aAtomData) {
        if (!hasSymbol()) throw new UnsupportedOperationException("`typeMap` for IHasSymbol without symbols");
        if (!aAtomData.hasSymbol()) {
            int tTypeNum = ntypes();
            if (tTypeNum>0 && tTypeNum<aAtomData.ntypes()) throw new IllegalArgumentException("Invalid atom type number of AtomData: " + aAtomData.ntypes() + ", target: " + tTypeNum);
            return type->type;
        }
        return typeMap_(Objects.requireNonNull(symbols()), aAtomData);
    }
    /**
     * 通过内部的 {@link #symbols()} 来判断输入的 aSymbolsIn 是否是有着相同的顺序
     * @param aSymbolsIn 需要进行判断的输入元素符号列表
     * @return 是否和输入的符号列表有着相同的顺序，this 的列表可以长于输入的列表
     * @throws UnsupportedOperationException 当不存在 {@link #symbols()} 数据
     */
    default boolean sameSymbolOrder(Collection<? extends CharSequence> aSymbolsIn) {
        if (!hasSymbol()) throw new UnsupportedOperationException("`sameSymbolOrder` for IHasSymbol without symbols");
        return sameSymbolOrder_(Objects.requireNonNull(symbols()), aSymbolsIn);
    }
    /**
     * 获取输入的元素符号对应的种类编号
     * @param aSymbol 需要查询种类编号的元素符号
     * @return 元素符号对应的种类编号，如果不存在返回 {@code -1}
     * @throws UnsupportedOperationException 当不存在 {@link #symbols()} 数据
     */
    default int typeOf(String aSymbol) {
        if (!hasSymbol()) throw new UnsupportedOperationException("`typeOf` for IHasSymbol without symbols");
        return typeOf_(Objects.requireNonNull(symbols()), aSymbol);
    }
    /**
     * 验证输入的 aTypeMap 是否超过了自身的种类数目，
     * 当种类数小于等于 {@code 0} 时不会进行检测
     * @param aTypeNum aTypeMap 总共的种类数
     * @param aTypeMap 需要检测的种类映射
     */
    default void typeMapCheck(int aTypeNum, IntUnaryOperator aTypeMap) {
        int tTypeNum = ntypes();
        if (tTypeNum <= 0) return;
        for (int tType = 1; tType <= aTypeNum; ++tType) {
            if (tTypeNum < aTypeMap.applyAsInt(tType)) throw new IllegalArgumentException("Invalid atom type number of TypeMap: " + aTypeMap.applyAsInt(tType) + ", limit: " + tTypeNum);
        }
    }
    
    @ApiStatus.Internal
    static IntUnaryOperator typeMap_(List<String> aSymbols, IAtomData aAtomData) {
        List<String> tAtomDataSymbols = Objects.requireNonNull(aAtomData.symbols());
        if (sameSymbolOrder_(aSymbols, tAtomDataSymbols)) return type->type;
        final int[] tAtomDataType2newType = new int[tAtomDataSymbols.size()+1];
        for (int i = 0; i < tAtomDataSymbols.size(); ++i) {
            String tElem = tAtomDataSymbols.get(i);
            int type = typeOf_(aSymbols, tElem);
            if (type <= 0) throw new IllegalArgumentException("Invalid element ("+tElem+") in AtomData");
            tAtomDataType2newType[i+1] = type;
        }
        return type -> tAtomDataType2newType[type];
    }
    @ApiStatus.Internal
    static boolean sameSymbolOrder_(List<String> aSymbols, Collection<? extends CharSequence> aSymbolsIn) {
        if (aSymbols.size() < aSymbolsIn.size()) return false;
        int tIdx = 0;
        for (CharSequence aSymbol : aSymbolsIn) {
            if (!aSymbol.equals(aSymbols.get(tIdx))) {
                return false;
            }
            ++tIdx;
        }
        return true;
    }
    @ApiStatus.Internal
    static int typeOf_(List<String> aSymbols, String aSymbol) {
        for (int i = 0; i < aSymbols.size(); ++i) {
            if (aSymbol.equals(aSymbols.get(i))) {
                return i+1;
            }
        }
        return -1;
    }
}
