package jse.lmp;

import jse.atom.IAtomData;
import jse.code.FileEndException;
import jse.code.IO;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.NewCollections;
import jse.math.table.ITable;
import jse.math.vector.ILongVector;
import jse.math.vector.ILongVectorGetter;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * <a href="https://docs.lammps.org/dump.html">
 * lammps dump </a> 格式支持，此类为专门针对其多帧数据的支持。
 * <p>
 * 多帧的 {@link SubLammpstrj}，通过：
 * <pre> {@code
 * def data = dump[i]
 * } </pre>
 * 来直接获取某一帧的 {@link SubLammpstrj} 数据，从而可以进行原子数据的相关操作，
 * 或者通过：
 * <pre> {@code
 * for (data in dump.asList()) {
 *     //...
 * }
 * } </pre>
 * 来将多帧数据转成 {@link List} 后进行遍历
 * <p>
 * 别称为 {@link Dump}
 *
 * @see IAtomData IAtomData: 原子数据类型通用接口
 * @see SubLammpstrj SubLammpstrj: 内部存储的单帧 lammps dump 原子数据类型
 * @see #read(String) read(String): 读取指定路径的 lammps dump 的所有帧的数据
 * @see #write(String) write(String): 将此 lammps dump 原子数据写入指定路径
 * @see #of(IAtomData) of(IAtomData): 将任意的原子数据转换成多帧 lammps dump 原子数据
 * @author liqa
 */
public class Lammpstrj extends AbstractListWrapper<SubLammpstrj, IAtomData, SubLammpstrj> {
    Lammpstrj() {super(NewCollections.zl());}
    Lammpstrj(SubLammpstrj... aData) {super(NewCollections.from(aData));}
    Lammpstrj(List<SubLammpstrj> aData) {super(aData);}
    
    /** AbstractListWrapper stuffs */
    @Override protected final SubLammpstrj toInternal_(IAtomData aAtomData) {return SubLammpstrj.of(aAtomData, SubLammpstrj.getTimeStep(aAtomData, size()));}
    @Override protected final SubLammpstrj toOutput_(SubLammpstrj aSubLammpstrj) {return aSubLammpstrj;}
    
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    @Override public Lammpstrj append(IAtomData aAtomData) {return (Lammpstrj)super.append(aAtomData);}
    @Override public Lammpstrj appendAll(Collection<? extends IAtomData> aAtomDataList) {return (Lammpstrj)super.appendAll(aAtomDataList);}
    public Lammpstrj append(IAtomData aAtomData, long aTimeStep) {
        mList.add(SubLammpstrj.of(aAtomData, aTimeStep));
        return this;
    }
    public Lammpstrj appendFile(String aFilePath) throws IOException {
        mList.addAll(read(aFilePath).mList);
        return this;
    }
    /** groovy stuffs */
    @Override public Lammpstrj leftShift(IAtomData aAtomData) {return (Lammpstrj)super.leftShift(aAtomData);}
    @Override public Lammpstrj leftShift(Collection<? extends IAtomData> aAtomDataList) {return (Lammpstrj)super.leftShift(aAtomDataList);}
    
    /** 提供直接转为表格的接口 */
    public List<ITable> asTables() {
        return AbstractCollections.map(mList, SubLammpstrj::asTable);
    }
    
    /** 截断开头一部分, 返回自身来支持链式调用 */
    public Lammpstrj cutFront(@Range(from=0, to=Integer.MAX_VALUE) int aLength) {
        if (aLength == 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        List<SubLammpstrj> oList = mList;
        mList = new ArrayList<>(oList.size() - aLength);
        Iterator<SubLammpstrj> it = oList.listIterator(aLength);
        while (it.hasNext()) mList.add(it.next());
        return this;
    }
    /** 截断结尾一部分, 返回自身来支持链式调用 */
    public Lammpstrj cutBack(@Range(from=0, to=Integer.MAX_VALUE) int aLength) {
        if (aLength == 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        for (int i = 0; i < aLength; ++i) removeLast();
        return this;
    }
    /** 等间距截取，返回自身来支持链式调用 */
    public Lammpstrj step(@Range(from=1, to=Integer.MAX_VALUE) int aStep) {
        if (aStep == 1) return this;
        List<SubLammpstrj> oList = mList;
        final int tSize = oList.size();
        mList = new ArrayList<>(tSize / aStep);
        for (int i = 0; i < tSize; i+=aStep) {
            mList.add(oList.get(i));
        }
        return this;
    }
    
    /** 拷贝一份 Lammpstrj */
    public Lammpstrj copy() {
        List<SubLammpstrj> rData = new ArrayList<>(mList.size());
        for (SubLammpstrj subData : mList) rData.add(subData.copy());
        return new Lammpstrj(rData);
    }
    
    /// 创建 Lammpstrj
    /**
     * 创建一个空的多帧 lammps dump 数据
     * @return 新创建的空的 {@link Lammpstrj}
     */
    public static Lammpstrj zl() {
        return new Lammpstrj();
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个多帧的 lammps dump 数据（内部只有一帧）
     * <p>
     * 使用 {@link #of(IAtomData, long)} 来手动指定其时间步
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的多帧的 lammps dump 数据
     * @see SubLammpstrj#of(IAtomData)
     * @see #of(IAtomData, long)
     */
    public static Lammpstrj of(IAtomData aAtomData) {
        return new Lammpstrj(SubLammpstrj.of(aAtomData));
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个多帧的 lammps dump 数据（内部只有一帧）
     * @param aAtomData 输入的原子数据
     * @param aTimeStep 可选的时间步
     * @return 创建的多帧的 lammps dump 数据
     * @see SubLammpstrj#of(IAtomData, long)
     * @see #of(IAtomData)
     */
    public static Lammpstrj of(IAtomData aAtomData, long aTimeStep) {
        return new Lammpstrj(SubLammpstrj.of(aAtomData, aTimeStep));
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 lammps dump 数据
     * <p>
     * 使用 {@link #of(Iterable, ILongVectorGetter)} 来手动指定每帧的时间步
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的多帧的 lammps dump 数据
     * @see #of(Iterable, ILongVectorGetter)
     */
    public static Lammpstrj of(Iterable<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.of(subAtomData, SubLammpstrj.getTimeStep(subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 lammps dump 数据
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTimeStepGetter 可选的时间步获取器，可以使用 lammbda 表达式或者直接传入 {@link ILongVector}
     * @return 创建的多帧的 lammps dump 数据
     * @see #of(Iterable)
     * @see ILongVectorGetter
     */
    public static Lammpstrj of(Iterable<? extends IAtomData> aAtomDataList, ILongVectorGetter aTimeStepGetter) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.of(subAtomData, aTimeStepGetter.get(i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 lammps dump 数据
     * <p>
     * 使用 {@link #of(Collection, ILongVectorGetter)} 来手动指定每帧的时间步
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的多帧的 lammps dump 数据
     * @see #of(Collection, ILongVectorGetter)
     */
    public static Lammpstrj of(Collection<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>(aAtomDataList.size());
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.of(subAtomData, SubLammpstrj.getTimeStep(subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 lammps dump 数据
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTimeStepGetter 可选的时间步获取器，可以使用 lammbda 表达式或者直接传入 {@link ILongVector}
     * @return 创建的多帧的 lammps dump 数据
     * @see #of(Collection)
     * @see ILongVectorGetter
     */
    public static Lammpstrj of(Collection<? extends IAtomData> aAtomDataList, ILongVectorGetter aTimeStepGetter) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>(aAtomDataList.size());
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.of(subAtomData, aTimeStepGetter.get(i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link Lammpstrj} 也能直接输入
     * @see #of(Collection)
     * @see AbstractListWrapper
     */
    public static Lammpstrj of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList) {
        return of(aAtomDataList.asList());
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link Lammpstrj} 也能直接输入
     * @see #of(Collection, ILongVectorGetter)
     * @see AbstractListWrapper
     */
    public static Lammpstrj of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, ILongVectorGetter aTimeStepGetter) {
        return of(aAtomDataList.asList(), aTimeStepGetter);
    }
    /// matlab stuffs
    /**
     * 对于 matlab 调用的兼容方法
     * @see #of(Collection)
     */
    public static Lammpstrj of_compat(Object[] aAtomDataArray) {
        if (aAtomDataArray==null || aAtomDataArray.length==0) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rLammpstrj.add(SubLammpstrj.of((IAtomData)subAtomData, SubLammpstrj.getTimeStep((IAtomData)subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    
    
    /// 文件读写
    /**
     * 从文件 lammps 输出的 dump 文件中读取来实现初始化
     * <p>
     * 注意和 {@link SubLammpstrj#read(String)} 不同的是，
     * 在遇到文件不完整的情况不会报错而是直接截断最后不完整的帧
     *
     * @param aFilePath lammps 输出的 dump 文件路径
     * @return 读取得到的 {@link Lammpstrj} 对象
     * @throws IOException 如果读取失败
     */
    public static Lammpstrj read(String aFilePath) throws IOException {
        try (BufferedReader tReader = IO.toReader(aFilePath)) {return read(tReader);}
    }
    /**
     * 提供使用 {@link BufferedReader} 的流式接口
     * @param aReader 需要的读取流
     * @return 读取得到的 {@link Lammpstrj} 对象
     * @throws IOException 如果读取失败
     */
    public static Lammpstrj read(BufferedReader aReader) throws IOException {
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        while (true) {
            try {
                SubLammpstrj tSubLammpstrj = SubLammpstrj.read(aReader);
                rLammpstrj.add(tSubLammpstrj);
            } catch (FileEndException any) {
                break;
            }
        }
        return new Lammpstrj(rLammpstrj);
    }
    
    /**
     * 输出成 lammps 格式的 dump 文件，可以供 OVITO 等软件读取
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {
        try (IO.IWriteln tWriteln = IO.toWriteln(aFilePath)) {write(tWriteln);}
    }
    /**
     * 提供使用 {@link IO.IWriteln} 的流式接口
     * @param aWriteln 需要写入的流
     * @throws IOException 如果写入文件失败
     */
    public void write(IO.IWriteln aWriteln) throws IOException {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.write(aWriteln);
    }
    
    
    /// MPI stuffs
    /** 用于 MPI 收发信息的 tags */
    final static int
          LAMMPSTRJ_INFO = 210
        , DATA_KEY = 212
        , DATA = 211
        , LAMMPSTRJ_SIZE = 219
        ;
    /** send recv bcast 做简单实现，将 Lammpstrj 看作一个整体 */
    public static void send(Lammpstrj aLammpstrj, int aDest, MPI.Comm aComm) throws MPIException {
        aComm.sendI(aLammpstrj.size(), aDest, LAMMPSTRJ_SIZE);
        for (SubLammpstrj tSubLammpstrj : aLammpstrj.mList) SubLammpstrj.send(tSubLammpstrj, aDest, aComm);
    }
    public static Lammpstrj recv(int aSource, MPI.Comm aComm) throws MPIException {
        final int tSize = aComm.recvI(aSource, LAMMPSTRJ_SIZE);
        List<SubLammpstrj> rLammpstrj = new ArrayList<>(tSize);
        for (int i = 0; i < tSize; ++i) rLammpstrj.add(SubLammpstrj.recv(aSource, aComm));
        return new Lammpstrj(rLammpstrj);
    }
    public static Lammpstrj bcast(Lammpstrj aLammpstrj, int aRoot, MPI.Comm aComm) throws MPIException {
        if (aComm.rank() == aRoot) {
            aComm.bcastI(aLammpstrj.size(), aRoot);
            for (SubLammpstrj tSubLammpstrj : aLammpstrj.mList) SubLammpstrj.bcast(tSubLammpstrj, aRoot, aComm);
            return aLammpstrj;
        } else {
            final int tSize = aComm.bcastI(-1, aRoot);
            List<SubLammpstrj> rLammpstrj = new ArrayList<>(tSize);
            for (int i = 0; i < tSize; ++i) rLammpstrj.add(SubLammpstrj.bcast(null, aRoot, aComm));
            return new Lammpstrj(rLammpstrj);
        }
    }
    /** 对于整个 Lammpstrj 还提供 gather 和 scatter 方法 */
    public static Lammpstrj gather(Lammpstrj aLammpstrj, int aRoot, MPI.Comm aComm) throws MPIException {
        if (aComm.rank() != aRoot) {
            send(aLammpstrj, aRoot, aComm);
            return aLammpstrj;
        } else {
            final int tNP = aComm.size();
            List<SubLammpstrj> rLammpstrj = new ArrayList<>(aLammpstrj.size() * tNP);
            for (int i = 0; i < tNP; ++i) {
                if (i != aRoot) {
                    int tSize = aComm.recvI(i, LAMMPSTRJ_SIZE);
                    for (int j = 0; j < tSize; ++j) rLammpstrj.add(SubLammpstrj.recv(i, aComm));
                } else {
                    rLammpstrj.addAll(aLammpstrj.mList);
                }
            }
            return new Lammpstrj(rLammpstrj);
        }
    }
    public static Lammpstrj allgather(Lammpstrj aLammpstrj, MPI.Comm aComm) throws MPIException {
        final int tMe = aComm.rank();
        final int tNP = aComm.size();
        List<SubLammpstrj> rLammpstrj = new ArrayList<>(aLammpstrj.size() * tNP);
        for (int i = 0; i < tNP; ++i) {
            if (i == tMe) {
                int tSize = aComm.bcastI(aLammpstrj.size(), i);
                for (int j = 0; j < tSize; ++j) SubLammpstrj.bcast(aLammpstrj.get(j), i, aComm);
                rLammpstrj.addAll(aLammpstrj.mList);
            } else {
                int tSize = aComm.bcastI(-1, i);
                for (int j = 0; j < tSize; ++j) rLammpstrj.add(SubLammpstrj.bcast(null, i, aComm));
            }
        }
        return new Lammpstrj(rLammpstrj);
    }
    public static Lammpstrj scatter(Lammpstrj aLammpstrj, int aRoot, MPI.Comm aComm) throws MPIException {
        if (aComm.rank() == aRoot) {
            List<SubLammpstrj> rLammpstrj = null;
            final int tNP = aComm.size();
            final int tSize = aLammpstrj.size();
            final int subSize = tSize / tNP;
            final int tRest = tSize % tNP;
            final Iterator<SubLammpstrj> it = aLammpstrj.iterator();
            for (int i = 0; i < tNP; ++i) {
                int tScatterSize = (i<tRest) ? subSize+1 : subSize;
                if (i != aRoot) {
                    aComm.sendI(tScatterSize, i, LAMMPSTRJ_SIZE);
                    for (int j = 0; j < tScatterSize; ++j) SubLammpstrj.send(it.next(), i, aComm);
                } else {
                    rLammpstrj = new ArrayList<>(tScatterSize);
                    for (int j = 0; j < tScatterSize; ++j) rLammpstrj.add(it.next());
                }
            }
            return new Lammpstrj(rLammpstrj);
        } else {
            int tSize = aComm.recvI(aRoot, LAMMPSTRJ_SIZE);
            List<SubLammpstrj> rLammpstrj = new ArrayList<>(tSize);
            for (int j = 0; j < tSize; ++j) rLammpstrj.add(SubLammpstrj.recv(aRoot, aComm));
            return new Lammpstrj(rLammpstrj);
        }
    }
}
