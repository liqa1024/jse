package jse.lmp;

import jse.atom.IAtomData;
import jse.atom.MFPC;
import jse.atom.MultiFrameParameterCalculator;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.NewCollections;
import jse.math.table.ITable;
import jse.math.vector.ILongVector;
import jse.math.vector.ILongVectorGetter;
import jse.math.vector.RefLongVector;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * @author liqa
 * <p> lammps 使用 dump 输出的数据格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 支持拥有多个时间帧的输出（可以当作 List 来使用），
 * 也可直接获取结果（则会直接获取第一个时间帧的结果） </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 * <p> 现在不再继承 {@link List}，因为 List 的接口太脏了 </p>
 * <p> 并且现在不再继承 {@link IAtomData}，如果需要使用单个 dump 直接使用 {@link SubLammpstrj} </p>
 */
public class Lammpstrj extends AbstractListWrapper<SubLammpstrj, IAtomData, SubLammpstrj> {
    Lammpstrj() {super(NewCollections.zl());}
    Lammpstrj(SubLammpstrj... aData) {super(NewCollections.from(aData));}
    Lammpstrj(List<SubLammpstrj> aData) {super(aData);}
    
    /** AbstractListWrapper stuffs */
    @Override protected final SubLammpstrj toInternal_(IAtomData aAtomData) {return SubLammpstrj.fromAtomData(aAtomData, SubLammpstrj.getTimeStep(aAtomData, size()));}
    @Override protected final SubLammpstrj toOutput_(SubLammpstrj aSubLammpstrj) {return aSubLammpstrj;}
    
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    @Override public Lammpstrj append(IAtomData aAtomData) {return (Lammpstrj)super.append(aAtomData);}
    @Override public Lammpstrj appendAll(Collection<? extends IAtomData> aAtomDataList) {return (Lammpstrj)super.appendAll(aAtomDataList);}
    public Lammpstrj append(IAtomData aAtomData, long aTimeStep) {
        mList.add(SubLammpstrj.fromAtomData(aAtomData, aTimeStep));
        return this;
    }
    public Lammpstrj appendFile(String aFilePath) throws IOException {
        mList.addAll(read(aFilePath).mList);
        return this;
    }
    /** groovy stuffs */
    @Override public Lammpstrj leftShift(IAtomData aAtomData) {return (Lammpstrj)super.leftShift(aAtomData);}
    @Override public Lammpstrj leftShift(Collection<? extends IAtomData> aAtomDataList) {return (Lammpstrj)super.leftShift(aAtomDataList);}
    
    
    // dump 额外的属性
    public ILongVector allTimeStep() {
        return new RefLongVector() {
            @Override public long get(int aIdx) {return mList.get(aIdx).timeStep();}
            @Override public int size() {return mList.size();}
        };
    }
    public List<String[]> allBoxBounds() {return AbstractCollections.map(mList, SubLammpstrj::boxBounds);}
    public List<LmpBox> allBox() {return AbstractCollections.map(mList, SubLammpstrj::box);}
    /** @deprecated use {@link #allBox} */ @Deprecated public List<LmpBox> allLmpBox() {return allBox();}
    public Lammpstrj setAllTimeStep(ILongVectorGetter aTimeStepGetter) {
        int i = 0;
        for (SubLammpstrj tSubLammpstrj : mList) {
            tSubLammpstrj.setTimeStep(aTimeStepGetter.get(i));
            ++i;
        }
        return this;
    }
    /** Groovy stuffs */
    @VisibleForTesting public ILongVector getAllTimeStep() {return allTimeStep();}
    
    /** 提供直接转为表格的接口 */
    public List<ITable> asTables() {
        return AbstractCollections.map(mList, SubLammpstrj::asTable);
    }
    
    /** 修改模拟盒类型 */
    public Lammpstrj setBoxNormal() {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.setBoxNormal();
        return this;
    }
    public Lammpstrj setBoxPrism() {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.setBoxPrism();
        return this;
    }
    public Lammpstrj setBoxPrism(double aXY, double aXZ, double aYZ) {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.setBoxPrism(aXY, aXZ, aYZ);
        return this;
    }
    /** 调整模拟盒的 xyz 长度 */
    public Lammpstrj setBoxXYZ(double aX, double aY, double aZ) {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.setBoxXYZ(aX, aY, aZ);
        return this;
    }
    /** 设置缩放 */
    public Lammpstrj setBoxScale(double aScale) {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.setBoxScale(aScale);
        return this;
    }
    /** 密度归一化, 返回自身来支持链式调用 */
    public Lammpstrj setDenseNormalized() {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.setDenseNormalized();
        return this;
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
    
    /** 拷贝一份 Lammpstrj */
    public Lammpstrj copy() {
        List<SubLammpstrj> rData = new ArrayList<>(mList.size());
        for (SubLammpstrj subData : mList) rData.add(subData.copy());
        return new Lammpstrj(rData);
    }
    
    /// 创建 Lammpstrj
    /** 从 IAtomData 来创建，对于 Lammpstrj 可以支持容器的 aAtomData */
    public static Lammpstrj fromAtomData(IAtomData aAtomData) {
        return new Lammpstrj(SubLammpstrj.fromAtomData(aAtomData));
    }
    public static Lammpstrj fromAtomData(IAtomData aAtomData, long aTimeStep) {
        return new Lammpstrj(SubLammpstrj.fromAtomData(aAtomData, aTimeStep));
    }
    public static Lammpstrj fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.fromAtomData(subAtomData, SubLammpstrj.getTimeStep(subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    public static Lammpstrj fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, ILongVectorGetter aTimeStepGetter) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.fromAtomData(subAtomData, aTimeStepGetter.get(i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    public static Lammpstrj fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>(aAtomDataList.size());
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.fromAtomData(subAtomData, SubLammpstrj.getTimeStep(subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    public static Lammpstrj fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, ILongVectorGetter aTimeStepGetter) {
        if (aAtomDataList == null) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>(aAtomDataList.size());
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            rLammpstrj.add(SubLammpstrj.fromAtomData(subAtomData, aTimeStepGetter.get(i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    /** 对于 matlab 调用的兼容 */
    public static Lammpstrj fromAtomData_compat(Object[] aAtomDataArray) {
        if (aAtomDataArray==null || aAtomDataArray.length==0) return new Lammpstrj();
        
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        int i = 0;
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rLammpstrj.add(SubLammpstrj.fromAtomData((IAtomData)subAtomData, SubLammpstrj.getTimeStep((IAtomData)subAtomData, i)));
            ++i;
        }
        return new Lammpstrj(rLammpstrj);
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static Lammpstrj zl() {return new Lammpstrj();}
    public static Lammpstrj of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static Lammpstrj of(IAtomData aAtomData, long aTimeStep) {return fromAtomData(aAtomData, aTimeStep);}
    public static Lammpstrj of(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static Lammpstrj of(Iterable<? extends IAtomData> aAtomDataList, ILongVectorGetter aTimeStepGetter) {return fromAtomDataList(aAtomDataList, aTimeStepGetter);}
    public static Lammpstrj of(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static Lammpstrj of(Collection<? extends IAtomData> aAtomDataList, ILongVectorGetter aTimeStepGetter) {return fromAtomDataList(aAtomDataList, aTimeStepGetter);}
    /** 再提供一个 IListWrapper 的接口保证 Lammpstrj 也能输入 */
    public static Lammpstrj of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList) {return fromAtomDataList(aAtomDataList.asList());}
    public static Lammpstrj of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, ILongVectorGetter aTimeStepGetter) {return fromAtomDataList(aAtomDataList.asList(), aTimeStepGetter);}
    /** matlab stuffs */
    public static Lammpstrj of_compat(Object[] aAtomDataArray) {return fromAtomData_compat(aAtomDataArray);}
    
    
    /// 文件读写
    /**
     * 从文件 lammps 输出的 dump 文件中读取来实现初始化
     * @author liqa
     * @param aFilePath lammps 输出的 dump 文件路径
     * @return 读取得到的 Lammpstrj 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     */
    public static Lammpstrj read(String aFilePath) throws IOException {try (BufferedReader tReader = UT.IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static Lammpstrj read_(BufferedReader aReader) throws IOException {
        List<SubLammpstrj> rLammpstrj = new ArrayList<>();
        SubLammpstrj tSubLammpstrj;
        while ((tSubLammpstrj = SubLammpstrj.read_(aReader)) != null) {
            rLammpstrj.add(tSubLammpstrj);
        }
        return new Lammpstrj(rLammpstrj);
    }
    
    /**
     * 输出成 lammps 格式的 dump 文件，可以供 OVITO 等软件读取
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {write_(tWriteln);}}
    /** 改为 {@link UT.IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用 */
    void write_(UT.IO.IWriteln aWriteln) throws IOException {
        for (SubLammpstrj tSubLammpstrj : mList) tSubLammpstrj.write_(aWriteln);
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
    
    
    /// 实用功能，这里依旧保留这种写法
    /**
     * 获取多帧原子参数的计算器，支持使用 MFPC 的简写来调用
     * @param aTimestep 每一个 step 之间的时间步长，实际每帧之间的时间步长会考虑 SubLammpstrj 的时间步；只考虑第一帧和第二帧之间的间距
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MFPC 的线程数目
     * @return 获取到的 MFPC
     * @deprecated use {@link MultiFrameParameterCalculator#of} or {@link MFPC#of}
     */
    @Deprecated public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType, int aThreadNum) {return MultiFrameParameterCalculator.of(AbstractCollections.map(mList, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
    /** @deprecated use {@link MultiFrameParameterCalculator#of}*/ @Deprecated public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep                           ) {return MultiFrameParameterCalculator.of(mList                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    /** @deprecated use {@link MultiFrameParameterCalculator#of}*/ @Deprecated public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep,            int aThreadNum) {return MultiFrameParameterCalculator.of(mList                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
    /** @deprecated use {@link MultiFrameParameterCalculator#of}*/ @Deprecated public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType                ) {return MultiFrameParameterCalculator.of(AbstractCollections.map(mList, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getMFPC                             (double aTimestep                           ) {return MFPC.of(mList                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getMFPC                             (double aTimestep,            int aThreadNum) {return MFPC.of(mList                                                                             , size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getTypeMFPC                         (double aTimestep, int aType                ) {return MFPC.of(AbstractCollections.map(mList, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep            );}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getTypeMFPC                         (double aTimestep, int aType, int aThreadNum) {return MFPC.of(AbstractCollections.map(mList, atomData -> atomData.operation().filterType(aType)), size()>1 ? aTimestep*(get(1).timeStep()-get(0).timeStep()) : aTimestep, aThreadNum);}
}
