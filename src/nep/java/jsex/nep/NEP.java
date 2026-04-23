package jsex.nep;

import jse.atom.IPairPotential;
import jse.clib.Compiler;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.cptr.*;
import jse.gpu.*;
import jse.jit.IJITEngine;
import jse.jit.IJITMethod;
import jse.jit.SimpleJIT;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.CS.ZL_STR;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.JAVA_HOME;
import static jse.cptr.CPointer.NULL;

/**
 * jse 实现的 nep，具体定义可以参考：
 * <a href="https://pubs.aip.org/aip/jcp/article-abstract/157/11/114801/2841888/GPUMD-A-package-for-constructing-accurate-machine">
 * GPUMD: A package for constructing accurate machine-learned potentials and performing highly efficient atomistic simulations </a>
 * <p>
 * 这里照搬了 c++ 版本的 nep 实现：
 * <a href="https://github.com/brucefan1983/NEP_CPU">
 * brucefan1983/NEP_CPU: CPU version of NEP </a>
 * 因此同样也基于 GPL-3.0 协议
 * <p>
 * 所有命令照搬 c++ 中的实现，并且命名格式保留原本格式，
 * 现在改为运行时编译并大幅增加模板使用来进一步提高效率
 * <p>
 * 更新日期：2025-05-04 (目前最新版本不够稳定)
 * <p>
 * commit SHA: 0155fd78cdf107a2c5a8dd31da9a29f3489d203e
 *
 * @author Zheyong Fan, Junjie Wang, Eric Lindgren，liqa
 */
public class NEP implements IPairPotential {
    
    public final static class Conf {
        /**
         * 自定义 nep cuda 中使用的 block_size 值，这可能会影响速度；
         * 默认为 {@code 256}
         */
        public static int CUDA_BLOCKSIZE = OS.envI("JSE_NEP_CUDA_BLOCKSIZE", 256);
        
        /**
         * nep 内部实现中使用查表方式计算径向函数，
         * 应当会有更高的速度，默认关闭
         */
        public static boolean USE_TABLE_FOR_RADIAL_FUNCTIONS = OS.envZ("JSE_NEP_USE_TABLE_FOR_RADIAL_FUNCTIONS", false);
        
        /**
         * 自定义构建 nep 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_NEP");
        /**
         * 自定义构建 nep 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NEP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NEP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        public static @Nullable String CMAKE_CUDA_COMPILER = OS.env("JSE_CMAKE_CUDA_COMPILER_NEP");
        public static @Nullable String CMAKE_CUDA_FLAGS    = OS.env("JSE_CMAKE_CUDA_FLAGS_NEP");
        /**
         * 自定义构建 nep 时的优化等级，
         * 默认会使用 BASE 优化
         */
        public static int OPTIM_LEVEL = OS.envI("JSE_NEP_OPTIM_LEVEL", IJITEngine.OPTIM_BASE);
        /**
         * 设置 nep 内部计算的默认精度，默认为 double
         */
        public static String PRECISION = OS.env("JSE_NEP_PRECISION", "double");
    }
    final static String VERSION_SHA = "0155fd78cdf107a2c5a8dd31da9a29f3489d203e";
    
    private static final String JIT_NAME = "nepjit";
    private final static String INTERFACE_NAME = "nep_interface.cpp", INTERFACE_HEAD_NAME = "nep_interface.h";
    private final static String INTERFACE_NAME_CUDA = "nep_interface_cuda.cu", INTERFACE_HEAD_NAME_CUDA = "nep_interface_cuda.h";
    private final static String SRC_NAME = "nep_core.hpp";
    
    NEP() {}
    public NEP(String aPotentialFileName) throws Exception {
        init_from_file(aPotentialFileName, "cpu");
    }
    
    @Override public int ntypes() {return element_list.size();}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return element_list.get(aType-1);}
    @Override public double rcutMax() {return Math.max(paramb.rc_radial, paramb.rc_angular);}
    
    
    private final DoubleList mNlDxBuf = new DoubleList(16), mNlDyBuf = new DoubleList(16), mNlDzBuf = new DoubleList(16);
    private final IntList mNlTypeBuf = new IntList(16), mNlIdxBuf = new IntList(16);
    
    private int buildNL_(IDxyzTypeIdxIterable aNL, double aRCut) {
        final int tTypeNum = this.ntypes();
        // 缓存情况需要先清空这些
        mNlDxBuf.clear(); mNlDyBuf.clear(); mNlDzBuf.clear();
        mNlTypeBuf.clear(); mNlIdxBuf.clear();
        aNL.forEachDxyzTypeIdx(aRCut, (dx, dy, dz, type, idx) -> {
            // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            mNlDxBuf.add(dx); mNlDyBuf.add(dy); mNlDzBuf.add(dz);
            mNlTypeBuf.add(type-1); mNlIdxBuf.add(idx);
        });
        int tNeiNum = mNlIdxBuf.size();
        mNlDx.ensureCapacity(tNeiNum); mNlDx.fillD(mNlDxBuf);
        mNlDy.ensureCapacity(tNeiNum); mNlDy.fillD(mNlDyBuf);
        mNlDz.ensureCapacity(tNeiNum); mNlDz.fillD(mNlDzBuf);
        mNlType.ensureCapacity(tNeiNum);
        mNlType.fill(mNlTypeBuf);
        mNlFx.ensureCapacity(tNeiNum);
        mNlFy.ensureCapacity(tNeiNum);
        mNlFz.ensureCapacity(tNeiNum);
        return tNeiNum;
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     */
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) throws Exception {
        if (mDead) throw new IllegalStateException("This NEP is dead");
        if (!mInited) throw new IllegalStateException();
        if (mCuda) throw new UnsupportedOperationException();
        Fp.ensureCapacity(annmb.dim);
        sum_fxyz.ensureCapacity((long) (paramb.n_max_angular + 1) * NUM_OF_ABC);
        aNeighborListGetter.forEachNLWithException(null, null, (threadID, cIdx, cType, nl) -> {
            // 近邻列表构建以及相关值设置
            int tNeiNum = buildNL_(nl, rcutMax());
            mInNums.putAt(0, tNeiNum);
            mInNums.putAt(1, cType-1);
            // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
            mDataIn.putAt(0, mInNums);
            mDataIn.putAt(1, mNlDx);
            mDataIn.putAt(2, mNlDy);
            mDataIn.putAt(3, mNlDz);
            mDataIn.putAt(4, mNlType);
            mDataIn.putAt(5, paramb.atomic_numbers);
            mDataIn.putAt(6, paramb.q_scaler);
            mDataIn.putAt(7, annmb.w0);
            mDataIn.putAt(8, annmb.b0);
            mDataIn.putAt(9, annmb.w1);
            mDataIn.putAt(10, annmb.b1);
            mDataIn.putAt(11, annmb.c);
            mDataIn.putAt(12, zbl.para);
            mDataIn.putAt(13, gn_radial);
            mDataIn.putAt(14, gn_angular);
            mDataOut.putAt(0, mOutEng);
            mDataOut.putAt(1, mNlFx);
            mDataOut.putAt(2, mNlFy);
            mDataOut.putAt(3, mNlFz);
            mDataOut.putAt(4, Fp);
            mDataOut.putAt(5, sum_fxyz);
            // 调用 jit 方法获取结果
            mCalEnergy.invoke(mDataIn, mDataOut);
            double tEng = mOutEng.getD();
            rEnergyAccumulator.add(threadID, cIdx, -1, tEng);
        });
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     * @param rForceAccumulator {@inheritDoc}
     * @param rVirialAccumulator {@inheritDoc}
     */
    @Override public void calEnergyForceVirial(int aAtomNumber, INeighborListGetter aNeighborListGetter, @Nullable IEnergyAccumulator rEnergyAccumulator, @Nullable IForceAccumulator rForceAccumulator, @Nullable IVirialAccumulator rVirialAccumulator) throws Exception {
        if (mDead) throw new IllegalStateException("This NEP is dead");
        if (!mInited) throw new IllegalStateException();
        if (mCuda) throw new UnsupportedOperationException();
        Fp.ensureCapacity(annmb.dim);
        sum_fxyz.ensureCapacity((long) (paramb.n_max_angular + 1) * NUM_OF_ABC);
        aNeighborListGetter.forEachNLWithException(null, null, (threadID, cIdx, cType, nl) -> {
            // 近邻列表构建以及相关值设置
            int tNeiNum = buildNL_(nl, rcutMax());
            mInNums.putAt(0, tNeiNum);
            mInNums.putAt(1, cType-1);
            // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
            mDataIn.putAt(0, mInNums);
            mDataIn.putAt(1, mNlDx);
            mDataIn.putAt(2, mNlDy);
            mDataIn.putAt(3, mNlDz);
            mDataIn.putAt(4, mNlType);
            mDataIn.putAt(5, paramb.atomic_numbers);
            mDataIn.putAt(6, paramb.q_scaler);
            mDataIn.putAt(7, annmb.w0);
            mDataIn.putAt(8, annmb.b0);
            mDataIn.putAt(9, annmb.w1);
            mDataIn.putAt(10, annmb.b1);
            mDataIn.putAt(11, annmb.c);
            mDataIn.putAt(12, zbl.para);
            mDataIn.putAt(13, gn_radial);
            mDataIn.putAt(14, gn_angular);
            mDataIn.putAt(15, gnp_radial);
            mDataIn.putAt(16, gnp_angular);
            mDataOut.putAt(0, mOutEng);
            mDataOut.putAt(1, mNlFx);
            mDataOut.putAt(2, mNlFy);
            mDataOut.putAt(3, mNlFz);
            mDataOut.putAt(4, Fp);
            mDataOut.putAt(5, sum_fxyz);
            // 调用 jit 方法获取结果
            mCalEnergyForce.invoke(mDataIn, mDataOut);
            double tEng = mOutEng.getD();
            if (rEnergyAccumulator != null) {
                rEnergyAccumulator.add(threadID, cIdx, -1, tEng);
            }
            // 累加交叉项到近邻
            for (int j = 0; j < tNeiNum; ++j) {
                double dx = mNlDxBuf.get(j);
                double dy = mNlDyBuf.get(j);
                double dz = mNlDzBuf.get(j);
                int idx = mNlIdxBuf.get(j);
                // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查；
                // 直接遍历查询不走合并了，实测专门合并还会影响效率
                double fx = mNlFx.getAtD(j);
                double fy = mNlFy.getAtD(j);
                double fz = mNlFz.getAtD(j);
                if (rForceAccumulator != null) {
                    rForceAccumulator.add(threadID, cIdx, idx, fx, fy, fz);
                }
                if (rVirialAccumulator != null) {
                    // GPUMD 给出的更具对称性的形式要求累加到近邻的 index 上
                    rVirialAccumulator.add(threadID, -1, idx, fx, fy, fz, dx, dy, dz);
                }
            }
        });
    }
    
    
    private void validNlLammps_(int aNeiNum) {
        mNlDx.ensureCapacity(aNeiNum);
        mNlDy.ensureCapacity(aNeiNum);
        mNlDz.ensureCapacity(aNeiNum);
        mNlType.ensureCapacity(aNeiNum);
        mNlIdx.ensureCapacity(aNeiNum);
        mNlFx.ensureCapacity(aNeiNum);
        mNlFy.ensureCapacity(aNeiNum);
        mNlFz.ensureCapacity(aNeiNum);
    }
    void computeLammps(PairNEP aPair) {
        if (mDead) throw new IllegalStateException("This NEP is dead");
        if (!mInited) throw new IllegalStateException();
        if (mCuda) throw new IllegalStateException();
        // 近邻列表大小获取和缓存合理化
        int inum = aPair.listInum();
        IntCPointer ilist = aPair.listIlist();
        IntCPointer numneigh = aPair.listNumneigh();
        mInNums.putAt(0, inum);
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, ilist);
        mDataIn.putAt(2, numneigh);
        mStatNeiNumLammps.invoke(mDataIn, mOutNums);
        validNlLammps_(mOutNums.getAt(0));
        
        Fp.ensureCapacity(annmb.dim);
        sum_fxyz.ensureCapacity((long) (paramb.n_max_angular + 1) * NUM_OF_ABC);
        // compute 开始，参数设置
        mInNums.putAt(0, inum);
        mInNums.putAt(1, aPair.eflagEither()?1:0);
        mInNums.putAt(2, aPair.vflagEither()?1:0);
        mInNums.putAt(3, aPair.eflagAtom()?1:0);
        mInNums.putAt(4, aPair.vflagAtom()?1:0);
        mInNums.putAt(5, aPair.cvflagAtom()?1:0);
        
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, mNlDx);
        mDataIn.putAt(2, mNlDy);
        mDataIn.putAt(3, mNlDz);
        mDataIn.putAt(4, mNlType);
        mDataIn.putAt(5, mNlIdx);
        mDataIn.putAt(6, paramb.atomic_numbers);
        mDataIn.putAt(7, paramb.q_scaler);
        mDataIn.putAt(8, annmb.w0);
        mDataIn.putAt(9, annmb.b0);
        mDataIn.putAt(10, annmb.w1);
        mDataIn.putAt(11, annmb.b1);
        mDataIn.putAt(12, annmb.c);
        mDataIn.putAt(13, zbl.para);
        mDataIn.putAt(14, gn_radial);
        mDataIn.putAt(15, gn_angular);
        mDataIn.putAt(16, gnp_radial);
        mDataIn.putAt(17, gnp_angular);
        mDataIn.putAt(18, aPair.atomX());
        mDataIn.putAt(19, aPair.atomType());
        mDataIn.putAt(20, ilist);
        mDataIn.putAt(21, numneigh);
        mDataIn.putAt(22, aPair.listFirstneigh());
        mDataIn.putAt(23, aPair.mCutoffsq);
        mDataIn.putAt(24, aPair.mTypeMap);
        
        mDataOut.putAt(0, aPair.atomF());
        mDataOut.putAt(1, mNlFx);
        mDataOut.putAt(2, mNlFy);
        mDataOut.putAt(3, mNlFz);
        mDataOut.putAt(4, Fp);
        mDataOut.putAt(5, sum_fxyz);
        mDataOut.putAt(6, aPair.engVdwl());
        mDataOut.putAt(7, aPair.eatom());
        mDataOut.putAt(8, aPair.virial());
        mDataOut.putAt(9, aPair.vatom());
        mDataOut.putAt(10, aPair.cvatom());
        
        // 调用 jit 方法计算
        mComputeLammps.invoke(mDataIn, mDataOut);
    }
    
    
    private boolean mCudaParaInited = false;
    private void initLmpParamCuda_(PairNEP aPair) throws CudaException {
        if (mCudaParaInited) return;
        mCudaParaInited = true;
        mCudaTypeMap = IntCudaPointer.malloc(aPair.mTypeNum+1);
        mCudaTypeMap.fill(aPair.mTypeMap, aPair.mTypeNum+1);
    }
    void computeLammpsCuda(PairNEP aPair) throws CudaException {
        if (mDead) throw new IllegalStateException("This NEP is dead");
        if (!mInited) throw new IllegalStateException();
        if (!mCuda) throw new IllegalStateException();
        initLmpParamCuda_(aPair);
        // 常规缓存向量长度规范
        final boolean nlflag = mNumneighMax<0 || aPair.neighborAgo()==0;
        final boolean cvflagAtom = aPair.cvflagAtom();
        final int inum = aPair.listInum();
        final int nlocal = aPair.atomNlocal();
        final int nghost = aPair.atomNghost();
        final int nlocalghost = nlocal + nghost;
        mFltBuf.ensureCapacity((long)nlocalghost*9);
        mCudaX.ensureCapacity((long)nlocalghost*3);
        mCudaF0.ensureCapacity((long)inum*3);
        mCudaF1.ensureCapacity((long)nlocalghost*3);
        mCudaEatom0.ensureCapacity((long)inum);
        mCudaVatom0.ensureCapacity((long)inum*6);
        mCudaVatom1.ensureCapacity((long)nlocalghost*(cvflagAtom?9:6));
        mCudaType.ensureCapacity(nlocalghost);
        if (nlflag) {
            mCudaIlist.ensureCapacity(inum);
            mCudaNumneigh.ensureCapacity(inum);
        }
        mCudaGNeiNum.ensureCapacity(inum);
        mCudaGCType.ensureCapacity(inum);
        cuda_Fp.ensureCapacity((long)inum*annmb.dim);
        cuda_sum_fxyz.ensureCapacity((long)inum*(paramb.n_max_angular+1)*NUM_OF_ABC);
        // 近邻列表大小获取和缓存合理化
        IPointer ilist = NULL;
        IPointer numneigh = NULL;
        IPointer firstneigh = NULL;
        if (nlflag) {
            ilist = aPair.listIlist();
            numneigh = aPair.listNumneigh();
            firstneigh = aPair.listFirstneigh();
            mInNums.putAt(0, inum);
            mDataIn.putAt(0, mInNums);
            mDataIn.putAt(1, ilist);
            mDataIn.putAt(2, numneigh);
            mStatNeiNumLammps.invoke(mDataIn, mOutNums);
            mNumneighMax = mOutNums.getAt(0);
        }
        // 近邻列表缓存向量长度规范
        if (nlflag) {
            int tTotNeiNum = inum*mNumneighMax;
            mIntBuf.ensureCapacity(tTotNeiNum);
            mCudaFirstneigh.ensureCapacity(tTotNeiNum);
            mCudaGNlType.ensureCapacity(tTotNeiNum);
            mCudaGNlIdx.ensureCapacity(tTotNeiNum);
            mCudaGNlDx.ensureCapacity(tTotNeiNum);
            mCudaGNlDy.ensureCapacity(tTotNeiNum);
            mCudaGNlDz.ensureCapacity(tTotNeiNum);
            mCudaGNlFx.ensureCapacity(tTotNeiNum);
            mCudaGNlFy.ensureCapacity(tTotNeiNum);
            mCudaGNlFz.ensureCapacity(tTotNeiNum);
        }
        
        // lammps -> cuda
        mInNums.putAt(0, inum);
        mInNums.putAt(1, nlocalghost);
        mInNums.putAt(2, nlflag?1:0);
        mInNums.putAt(3, mNumneighMax);
        
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, aPair.atomX());
        mDataIn.putAt(2, aPair.atomType());
        if (nlflag) {
            mDataIn.putAt(3, ilist);
            mDataIn.putAt(4, numneigh);
            mDataIn.putAt(5, firstneigh);
        }
        
        mDataOut.putAt(0, mFltBuf);
        mDataOut.putAt(1, mIntBuf);
        mDataOut.putAt(2, mCudaX);
        mDataOut.putAt(3, mCudaType);
        if (nlflag) {
            mDataOut.putAt(4, mCudaIlist);
            mDataOut.putAt(5, mCudaNumneigh);
            mDataOut.putAt(6, mCudaFirstneigh);
        }
        
        int tCode = mLammps2Cuda.invoke(mDataIn, mDataOut);
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda compute
        mInNums.putAt(0, inum);
        mInNums.putAt(1, nlocalghost);
        mInNums.putAt(2, aPair.eflagEither()?1:0);
        mInNums.putAt(3, aPair.vflagEither()?1:0);
        mInNums.putAt(4, aPair.eflagAtom()?1:0);
        mInNums.putAt(5, aPair.vflagAtom()?1:0);
        mInNums.putAt(6, cvflagAtom?1:0);
        
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, mCudaX);
        mDataIn.putAt(2, mCudaType);
        mDataIn.putAt(3, mCudaIlist);
        mDataIn.putAt(4, mCudaNumneigh);
        mDataIn.putAt(5, mCudaFirstneigh);
        mDataIn.putAt(6, aPair.mCutoffsq);
        mDataIn.putAt(7, mCudaTypeMap);
        mDataIn.putAt(8, paramb.cuda_atomic_numbers);
        mDataIn.putAt(9, paramb.cuda_q_scaler);
        mDataIn.putAt(10, annmb.cuda_w0);
        mDataIn.putAt(11, annmb.cuda_b0);
        mDataIn.putAt(12, annmb.cuda_w1);
        mDataIn.putAt(13, annmb.cuda_b1);
        mDataIn.putAt(14, annmb.cuda_c);
        mDataIn.putAt(15, zbl.cuda_para);
        mDataIn.putAt(16, cuda_gn_radial);
        mDataIn.putAt(17, cuda_gn_angular);
        mDataIn.putAt(18, cuda_gnp_radial);
        mDataIn.putAt(19, cuda_gnp_angular);
        
        mDataOut.putAt(0, mCudaF0);
        mDataOut.putAt(1, mCudaF1);
        mDataOut.putAt(2, mCudaEatom0);
        mDataOut.putAt(3, mCudaVatom0);
        mDataOut.putAt(4, mCudaVatom1);
        mDataOut.putAt(5, mCudaGNlDx);
        mDataOut.putAt(6, mCudaGNlDy);
        mDataOut.putAt(7, mCudaGNlDz);
        mDataOut.putAt(8, mCudaGNlType);
        mDataOut.putAt(9, mCudaGNlIdx);
        mDataOut.putAt(10, mCudaGNeiNum);
        mDataOut.putAt(11, mCudaGCType);
        mDataOut.putAt(12, mCudaGNlFx);
        mDataOut.putAt(13, mCudaGNlFy);
        mDataOut.putAt(14, mCudaGNlFz);
        mDataOut.putAt(15, cuda_Fp);
        mDataOut.putAt(16, cuda_sum_fxyz);
        
        tCode = mComputeLammpsCuda.invoke(mDataIn, mDataOut);
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda -> lammps
        mInNums.putAt(0, inum);
        mInNums.putAt(1, nlocalghost);
        mInNums.putAt(2, aPair.eflagEither()?1:0);
        mInNums.putAt(3, aPair.vflagEither()?1:0);
        mInNums.putAt(4, aPair.eflagAtom()?1:0);
        mInNums.putAt(5, aPair.vflagAtom()?1:0);
        mInNums.putAt(6, cvflagAtom?1:0);
        
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, mFltBuf);
        mDataIn.putAt(2, ilist);
        mDataIn.putAt(3, mCudaF1);
        mDataIn.putAt(4, mCudaEatom0);
        mDataIn.putAt(5, mCudaVatom0);
        mDataIn.putAt(6, mCudaVatom1);
        
        mDataOut.putAt(0, aPair.atomF());
        mDataOut.putAt(1, aPair.engVdwl());
        mDataOut.putAt(2, aPair.eatom());
        mDataOut.putAt(3, aPair.virial());
        mDataOut.putAt(4, aPair.vatom());
        mDataOut.putAt(5, aPair.cvatom());
        
        tCode = mCuda2Lammps.invoke(mDataIn, mDataOut);
        CudaCore.cudaExceptionCheck(tCode);
    }
    
    
    static final int NUM_OF_ABC = 80; // 3 + 5 + 7 + 9 + 11 + 13 + 15 + 17 for L_max = 8
    static final int NUM_ELEMENTS = 94;
    static final String[] ELEMENTS = {
        "H",  "He", "Li", "Be", "B",  "C",  "N",  "O",  "F",  "Ne", "Na", "Mg", "Al", "Si", "P",  "S",
        "Cl", "Ar", "K",  "Ca", "Sc", "Ti", "V",  "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge",
        "As", "Se", "Br", "Kr", "Rb", "Sr", "Y",  "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd",
        "In", "Sn", "Sb", "Te", "I",  "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd",
        "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta", "W",  "Re", "Os", "Ir", "Pt", "Au", "Hg",
        "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th", "Pa", "U",  "Np", "Pu"
    };
    static final int table_length = 2001;
    static final int table_segments = table_length - 1;
    static final double table_resolution = 0.0005;
    
    static class ParaMB {
        boolean use_typewise_cutoff = false;
        boolean use_typewise_cutoff_zbl = false;
        double typewise_cutoff_radial_factor = 2.5;
        double typewise_cutoff_angular_factor = 2.0;
        double typewise_cutoff_zbl_factor = 0.65;
        
        int model_type = 0; // 0=potential, 1=dipole, 2=polarizability
        int version = 4;
        double rc_radial = 0.0;
        double rc_angular = 0.0;
        double rcinv_radial = 0.0;
        double rcinv_angular = 0.0;
        int n_max_radial = 0;
        int n_max_angular = 0;
        int L_max = 0;
        int dim_angular;
        int num_L;
        int basis_size_radial = 8;
        int basis_size_angular = 8;
        int num_types_sq = 0;
        int num_c_radial = 0;
        int num_types = 0;
        final IDoubleOrFloatCPointer q_scaler;
        final IntCPointer atomic_numbers = IntCPointer.malloc(NUM_ELEMENTS);
        final boolean cuda;
        final FloatCudaPointer cuda_q_scaler;
        final IntCudaPointer cuda_atomic_numbers;
        ParaMB(boolean single, boolean cuda) throws CudaException {
            q_scaler = single ? FloatCPointer.malloc(140) : DoubleCPointer.malloc(140);
            this.cuda = cuda;
            cuda_q_scaler = cuda ? FloatCudaPointer.malloc(140) : null;
            cuda_atomic_numbers = cuda ? IntCudaPointer.malloc(NUM_ELEMENTS) : null;
        }
        void parse2cuda() throws CudaException {
            if (!cuda) throw new IllegalStateException();
            cuda_q_scaler.fill((FloatCPointer)q_scaler, 140);
            cuda_atomic_numbers.fill(atomic_numbers, NUM_ELEMENTS);
        }
        
        private boolean mFree = false;
        void free() {
            if (mFree) throw new IllegalStateException();
            mFree = true;
            q_scaler.free();
            atomic_numbers.free();
            if (cuda) {
                try {
                    cuda_q_scaler.free();
                    cuda_atomic_numbers.free();
                } catch (CudaException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    static class ANN {
        private boolean inited = false;
        int dim = 0;
        int num_neurons1 = 0;
        int num_para = 0;
        int num_para_ann = 0;
        final AnyCPointer w0 = AnyCPointer.calloc(NUM_ELEMENTS);
        final AnyCPointer b0 = AnyCPointer.calloc(NUM_ELEMENTS);
        final AnyCPointer w1 = AnyCPointer.calloc(NUM_ELEMENTS);
        IDoubleOrFloatCPointer b1 = null;
        IDoubleOrFloatCPointer c = null;
        // for the scalar part of polarizability
        final AnyCPointer w0_pol = AnyCPointer.calloc(NUM_ELEMENTS);
        final AnyCPointer b0_pol = AnyCPointer.calloc(NUM_ELEMENTS);
        final AnyCPointer w1_pol = AnyCPointer.calloc(NUM_ELEMENTS);
        IDoubleOrFloatCPointer b1_pol = null;
        
        final boolean cuda;
        final AnyCPointer cuda0_w0, cuda0_b0, cuda0_w1;
        final CudaPointer cuda_w0, cuda_b0, cuda_w1;
        FloatCudaPointer cuda_b1 = null;
        FloatCudaPointer cuda_c = null;
        // for the scalar part of polarizability
        final AnyCPointer cuda0_w0_pol, cuda0_b0_pol, cuda0_w1_pol;
        final CudaPointer cuda_w0_pol, cuda_b0_pol, cuda_w1_pol;
        FloatCudaPointer cuda_b1_pol = null;
        ANN(boolean single, boolean cuda) throws CudaException {
            this.cuda = cuda;
            cuda0_w0 = cuda ? AnyCPointer.calloc(NUM_ELEMENTS) : null;
            cuda0_b0 = cuda ? AnyCPointer.calloc(NUM_ELEMENTS) : null;
            cuda0_w1 = cuda ? AnyCPointer.calloc(NUM_ELEMENTS) : null;
            cuda0_w0_pol = cuda ? AnyCPointer.calloc(NUM_ELEMENTS) : null;
            cuda0_b0_pol = cuda ? AnyCPointer.calloc(NUM_ELEMENTS) : null;
            cuda0_w1_pol = cuda ? AnyCPointer.calloc(NUM_ELEMENTS) : null;
            cuda_w0 = cuda ? CudaPointer.malloc(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_b0 = cuda ? CudaPointer.malloc(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_w1 = cuda ? CudaPointer.malloc(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_w0_pol = cuda ? CudaPointer.malloc(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_b0_pol = cuda ? CudaPointer.malloc(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_w1_pol = cuda ? CudaPointer.malloc(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
        }
        
        void clear() {
            if (!inited) return;
            inited = false;
            for (int t = 0; t < NUM_ELEMENTS; ++t) {
                getAndFree_(t, w0);
                getAndFree_(t, b0);
                getAndFree_(t, w1);
                getAndFree_(t, w0_pol);
                getAndFree_(t, b0_pol);
                getAndFree_(t, w1_pol);
            }
            b1.free(); b1 = null;
            c.free(); c = null;
            if (b1_pol!=null) {
                b1_pol.free();
                b1_pol = null;
            }
            if (cuda) {
                try {
                    for (int t = 0; t < NUM_ELEMENTS; ++t) {
                        getAndFreeCuda_(t, cuda0_w0);
                        getAndFreeCuda_(t, cuda0_b0);
                        getAndFreeCuda_(t, cuda0_w1);
                        getAndFreeCuda_(t, cuda0_w0_pol);
                        getAndFreeCuda_(t, cuda0_b0_pol);
                        getAndFreeCuda_(t, cuda0_w1_pol);
                    }
                    cuda_w0.memset(0, NUM_ELEMENTS*AnyCPointer.TYPE_SIZE);
                    cuda_b0.memset(0, NUM_ELEMENTS*AnyCPointer.TYPE_SIZE);
                    cuda_w1.memset(0, NUM_ELEMENTS*AnyCPointer.TYPE_SIZE);
                    cuda_w0_pol.memset(0, NUM_ELEMENTS*AnyCPointer.TYPE_SIZE);
                    cuda_b0_pol.memset(0, NUM_ELEMENTS*AnyCPointer.TYPE_SIZE);
                    cuda_w1_pol.memset(0, NUM_ELEMENTS*AnyCPointer.TYPE_SIZE);
                    cuda_b1.free(); cuda_b1 = null;
                    cuda_c.free(); c = null;
                    if (cuda_b1_pol!=null) {
                        cuda_b1_pol.free();
                        cuda_b1_pol = null;
                    }
                } catch (CudaException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        private void getAndFree_(int i, AnyCPointer aNestedPtr) {
            CPointer tPtr = aNestedPtr.getAsCPointerAt(i);
            if (!tPtr.isNull()) {
                tPtr.free();
                aNestedPtr.putAt(i, NULL);
            }
        }
        private void getAndFreeCuda_(int i, AnyCPointer aNestedPtr) throws CudaException {
            CudaPointer tPtr = aNestedPtr.getAsCudaPointerAt(i);
            if (!tPtr.isNull()) {
                tPtr.free();
                aNestedPtr.putAt(i, NULL);
            }
        }
        
        private boolean mFree = false;
        void free() {
            if (mFree) throw new IllegalStateException();
            mFree = true;
            clear();
            w0.free(); b0.free(); w1.free();
            w0_pol.free(); b0_pol.free(); w1_pol.free();
            if (cuda) {
                cuda0_w0.free(); cuda0_b0.free(); cuda0_w1.free();
                cuda0_w0_pol.free(); cuda0_b0_pol.free(); cuda0_w1_pol.free();
                try {
                    cuda_w0.free(); cuda_b0.free(); cuda_w1.free();
                    cuda_w0_pol.free(); cuda_b0_pol.free(); cuda_w1_pol.free();
                } catch (CudaException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    static class ZBL {
        boolean enabled = false;
        boolean flexibled = false;
        int num_types;
        double rc_inner = 1.0;
        double rc_outer = 2.0;
        final IDoubleOrFloatCPointer para;
        final boolean cuda;
        final FloatCudaPointer cuda_para;
        ZBL(boolean single, boolean cuda) throws CudaException {
            para = single ? FloatCPointer.malloc(550) : DoubleCPointer.calloc(550);
            this.cuda = cuda;
            cuda_para = cuda ? FloatCudaPointer.malloc(550) : null;
        }
        void parse2cuda() throws CudaException {
            if (!cuda) throw new IllegalStateException();
            cuda_para.fill((FloatCPointer)para, 550);
        }
        
        private boolean mFree = false;
        void free() {
            if (mFree) throw new IllegalStateException();
            mFree = true;
            para.free();
            if (cuda) {
                try {
                    cuda_para.free();
                } catch (CudaException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    
    ParaMB paramb = null;
    ANN annmb = null;
    ZBL zbl = null;
    IGrowableDoubleOrFloatCPointer Fp = null, sum_fxyz = null;
    ArrayList<String> element_list = new ArrayList<>();
    
    IGrowableDoubleOrFloatCPointer gn_radial = null;   // tabulated gn_radial functions
    IGrowableDoubleOrFloatCPointer gnp_radial = null;  // tabulated gnp_radial functions
    IGrowableDoubleOrFloatCPointer gn_angular = null;  // tabulated gn_angular functions
    IGrowableDoubleOrFloatCPointer gnp_angular = null; // tabulated gnp_angular functions
    GrowableFloatCudaPointer cuda_gn_radial = null, cuda_gnp_radial = null, cuda_gn_angular = null, cuda_gnp_angular = null;
    
    boolean mInited = false, mSingle = false, mCuda = true;
    AnyCPointer mDataIn = AnyCPointer.calloc(32), mDataOut = AnyCPointer.calloc(32);
    IntCPointer mInNums = IntCPointer.malloc(32), mOutNums = IntCPointer.malloc(32);
    IDoubleOrFloatCPointer mOutEng = null;
    IGrowableDoubleOrFloatCPointer mNlDx = null, mNlDy = null, mNlDz = null;
    IGrowableDoubleOrFloatCPointer mNlFx = null, mNlFy = null, mNlFz = null;
    GrowableIntCPointer mNlType = null, mNlIdx = null;
    
    /// gpu stuffs
    // cpu 数据
    private int mNumneighMax = -1;
    private GrowableFloatCPointer mFltBuf = null;
    private GrowableIntCPointer mIntBuf = null;
    // cuda 数据
    private GrowableFloatCudaPointer mCudaX = null, mCudaF0 = null, mCudaF1 = null, mCudaEatom0 = null, mCudaVatom0 = null, mCudaVatom1 = null;
    private GrowableIntCudaPointer mCudaType = null, mCudaIlist = null, mCudaNumneigh = null, mCudaGNeiNum = null, mCudaGCType = null;
    private GrowableIntCudaPointer mCudaFirstneigh = null, mCudaGNlType = null, mCudaGNlIdx = null;
    private GrowableFloatCudaPointer mCudaGNlDx = null, mCudaGNlDy = null, mCudaGNlDz = null, mCudaGNlFx = null, mCudaGNlFy = null, mCudaGNlFz = null;
    private IntCudaPointer mCudaTypeMap = null;
    private GrowableFloatCudaPointer cuda_Fp = null, cuda_sum_fxyz = null;
    
    /// jit stuffs
    IJITEngine mJITEngine = null;
    private static final String NAME_CAL_ENERGY = "jse_nep_calEnergy", NAME_CAL_ENERGYFORCE = "jse_nep_calEnergyForce";
    private static final String NAME_CONSTRUCT_TABLE = "jse_nep_constructTable";
    private static final String NAME_STAT_NEINUM_LAMMPS = "jse_nep_statNeiNumLammps", NAME_COMPUTE_LAMMPS = "jse_nep_computeLammps";
    private static final String NAME_LAMMPS2CUDA = "jse_nep_lammps2cuda", NAME_CUDA2LAMMPS = "jse_nep_cuda2lammps", NAME_COMPUTE_LAMMPS_CUDA = "jse_nep_computeLammpsCuda";
    private IJITMethod mCalEnergy = null, mCalEnergyForce = null;
    private IJITMethod mConstructTable = null;
    private IJITMethod mStatNeiNumLammps = null, mComputeLammps = null;
    private IJITMethod mLammps2Cuda = null, mCuda2Lammps = null, mComputeLammpsCuda = null;
    private String mLibDir = OS.WORKING_DIR, mProjectName = JIT_NAME;
    
    private final static Pattern PROJECT_INVALID_NAME = Pattern.compile("[^a-zA-Z0-9_\\-]");
    
    private static final String MARKER_REMOVE_START = "// >>> NEPGEN REMOVE";
    private static final String MARKER_REMOVE_END = "// <<< NEPGEN REMOVE";
    private static final String MARKER_PICK_START = "// >>> NEPGEN PICK";
    private static final String MARKER_PICK_CASE = "// --- NEPGEN PICK:";
    private static final String MARKER_PICK_END = "// <<< NEPGEN PICK";
    private static final int STATE_NORMAL = 0, STATE_REMOVE = 1, STATE_PICK = 4;
    
    boolean mDead = false;
    @Override public boolean isClosed() {return mDead;}
    @Override public void close() throws Exception {
        if (mDead) return;
        mDead = true;
        
        mDataIn.free(); mDataOut.free();
        mInNums.free();
        if (mInited) {
            mOutEng.free();
            Fp.free(); sum_fxyz.free();
            gn_radial.free(); gnp_radial.free();
            gn_angular.free(); gnp_angular.free();
            mNlType.free(); mNlIdx.free();
            mNlDx.free(); mNlDy.free(); mNlDz.free();
            mNlFx.free(); mNlFy.free(); mNlFz.free();
            mJITEngine.close();
            paramb.free(); annmb.free(); zbl.free();
        }
        if (mCuda) {
            cuda_Fp.free(); cuda_sum_fxyz.free();
            cuda_gn_radial.free(); cuda_gnp_radial.free();
            cuda_gn_angular.free(); cuda_gnp_angular.free();
            mCudaX.free();
            mCudaF0.free();
            mCudaF1.free();
            mCudaEatom0.free();
            mCudaVatom0.free();
            mCudaVatom1.free();
            mCudaType.free();
            mCudaIlist.free();
            mCudaNumneigh.free();
            mCudaGNeiNum.free();
            mCudaGCType.free();
            mCudaFirstneigh.free();
            mCudaGNlType.free();
            mCudaGNlIdx.free();
            mCudaGNlDx.free();
            mCudaGNlDy.free();
            mCudaGNlDz.free();
            mCudaGNlFx.free();
            mCudaGNlFy.free();
            mCudaGNlFz.free();
        }
        if (mCudaTypeMap != null) {
            mCudaTypeMap.free();
            mCudaTypeMap = null;
        }
    }
    
    void compileJIT() throws Exception {
        Map<String, Object> rGenMap = initGenMap_();
        if (mCuda) {
            rGenMap.put("NEPGEN_CUDA_BLOCKSIZE", Conf.CUDA_BLOCKSIZE);
        }
        rGenMap.put("[PRECISION]", mSingle ? "single" : "double");
        rGenMap.put("[ARCH]", mCuda ? "cuda" : "cpu");
        String tUniqueID = UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, NEP.VERSION_SHA,
                                            rGenMap, Conf.OPTIM_LEVEL, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_CUDA_COMPILER, Conf.CMAKE_CUDA_FLAGS, Conf.CMAKE_SETTING);
        if (mCuda) {
            mJITEngine = CudaJIT.engine()
                .setCmakeCudaCompiler(Conf.CMAKE_CUDA_COMPILER).setCmakeCudaFlags(Conf.CMAKE_CUDA_FLAGS)
                .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
                .setCmakeSettings(Conf.CMAKE_SETTING).setOptimLevel(Conf.OPTIM_LEVEL)
                .setLibDir(mLibDir).setProjectName(mProjectName+"_"+tUniqueID)
                .setSrcDirIniter((wd, engine) -> {
                    codeGen_(IO.getResource("nep/src/"+SRC_NAME), wd+SRC_NAME, rGenMap);
                    codeGen_(IO.getResource("nep/src/"+INTERFACE_NAME_CUDA), wd+INTERFACE_NAME_CUDA, rGenMap);
                    codeGen_(IO.getResource("nep/src/"+INTERFACE_HEAD_NAME_CUDA), wd+INTERFACE_HEAD_NAME_CUDA, rGenMap);
                    // 注意这里需要使用 jit 中的通用 CMakeLists，确保 project name 同步
                    engine.writeCmakeFile(wd, INTERFACE_NAME_CUDA);
                    return wd;
                });
            mJITEngine.setMethodNames(NAME_CONSTRUCT_TABLE, NAME_STAT_NEINUM_LAMMPS, NAME_LAMMPS2CUDA, NAME_CUDA2LAMMPS, NAME_COMPUTE_LAMMPS_CUDA).compile();
            mLammps2Cuda = mJITEngine.findMethod(NAME_LAMMPS2CUDA);
            mCuda2Lammps = mJITEngine.findMethod(NAME_CUDA2LAMMPS);
            mComputeLammpsCuda = mJITEngine.findMethod(NAME_COMPUTE_LAMMPS_CUDA);
        } else {
            mJITEngine = SimpleJIT.engine()
                .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
                .setCmakeSettings(Conf.CMAKE_SETTING).setOptimLevel(Conf.OPTIM_LEVEL)
                .setLibDir(mLibDir).setProjectName(mProjectName+"_"+tUniqueID)
                .setSrcDirIniter((wd, engine) -> {
                    codeGen_(IO.getResource("nep/src/"+SRC_NAME), wd+SRC_NAME, rGenMap);
                    codeGen_(IO.getResource("nep/src/"+INTERFACE_NAME), wd+INTERFACE_NAME, rGenMap);
                    codeGen_(IO.getResource("nep/src/"+INTERFACE_HEAD_NAME), wd+INTERFACE_HEAD_NAME, rGenMap);
                    // 注意这里需要使用 jit 中的通用 CMakeLists，确保 project name 同步
                    engine.writeCmakeFile(wd, INTERFACE_NAME);
                    return wd;
                });
            mJITEngine.setMethodNames(NAME_CAL_ENERGY, NAME_CAL_ENERGYFORCE, NAME_CONSTRUCT_TABLE, NAME_STAT_NEINUM_LAMMPS, NAME_COMPUTE_LAMMPS).compile();
            mCalEnergy = mJITEngine.findMethod(NAME_CAL_ENERGY);
            mCalEnergyForce = mJITEngine.findMethod(NAME_CAL_ENERGYFORCE);
            mComputeLammps = mJITEngine.findMethod(NAME_COMPUTE_LAMMPS);
        }
        mConstructTable = mJITEngine.findMethod(NAME_CONSTRUCT_TABLE);
        mStatNeiNumLammps = mJITEngine.findMethod(NAME_STAT_NEINUM_LAMMPS);
    }
    private Map<String, Object> initGenMap_() {
        Map<String, Object> rGenMap = new LinkedHashMap<>();
        rGenMap.put("NEPGEN_USE_TABLE", Conf.USE_TABLE_FOR_RADIAL_FUNCTIONS?1:0);
        rGenMap.put("NEPGEN_VERSION", paramb.version);
        rGenMap.put("NEPGEN_NTYPES", paramb.num_types);
        rGenMap.put("NEPGEN_USE_TYPEWISE_CUTOFF", paramb.use_typewise_cutoff?1:0);
        rGenMap.put("NEPGEN_USE_TYPEWISE_CUTOFF_ZBL", paramb.use_typewise_cutoff_zbl?1:0);
        rGenMap.put("NEPGEN_TYPEWISE_CUTOFF_FACTOR_R", paramb.typewise_cutoff_radial_factor);
        rGenMap.put("NEPGEN_TYPEWISE_CUTOFF_FACTOR_A", paramb.typewise_cutoff_angular_factor);
        rGenMap.put("NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL", paramb.typewise_cutoff_zbl_factor);
        rGenMap.put("NEPGEN_RCUT_R", paramb.rc_radial);
        rGenMap.put("NEPGEN_RCUT_A", paramb.rc_angular);
        rGenMap.put("NEPGEN_RCUT_INNER_ZBL", zbl.rc_inner);
        rGenMap.put("NEPGEN_RCUT_OUTER_ZBL", zbl.rc_outer);
        rGenMap.put("NEPGEN_NMAX_R", paramb.n_max_radial);
        rGenMap.put("NEPGEN_NMAX_A", paramb.n_max_angular);
        rGenMap.put("NEPGEN_BSIZE_R", paramb.basis_size_radial);
        rGenMap.put("NEPGEN_BSIZE_A", paramb.basis_size_angular);
        rGenMap.put("NEPGEN_NUMC_R", paramb.num_c_radial);
        rGenMap.put("NEPGEN_LMAX", paramb.L_max);
        rGenMap.put("NEPGEN_NUML", paramb.num_L);
        rGenMap.put("NEPGEN_ANN_DIM_A", paramb.dim_angular);
        rGenMap.put("NEPGEN_ANN_DIM", annmb.dim);
        rGenMap.put("NEPGEN_NUM_NEURONS1", annmb.num_neurons1);
        rGenMap.put("NEPGEN_NUM_PARA", annmb.num_para);
        rGenMap.put("NEPGEN_NUM_PARA_ANN", annmb.num_para_ann);
        rGenMap.put("NEPGEN_ZBL", zbl.enabled?1:0);
        rGenMap.put("NEPGEN_ZBL_FLEXIBLED", zbl.flexibled?1:0);
        return rGenMap;
    }
    private static void codeGen_(URL aSourceURL, String aTargetPath, Map<String, Object> aGenMap) throws Exception {
        List<String> tLines;
        try (BufferedReader tReader = jse.code.IO.toReader(aSourceURL)) {
            tLines = jse.code.IO.readAllLines(tReader);
        }
        IO.write(aTargetPath, processLines_(tLines, aGenMap));
    }
    private static List<String> processLines_(List<String> aLines, Map<String, Object> aGenMap) {
        int tState = STATE_NORMAL;
        List<String> rBuf0 = new ArrayList<>(), rBuf1 = new ArrayList<>();
        List<String> rOutLines = new ArrayList<>(aLines.size());
        for (String tLine : aLines) {
            switch(tState) {
            case STATE_NORMAL: {
                switch(tLine.trim()) {
                case MARKER_REMOVE_START: {
                    tState = STATE_REMOVE;
                    break;
                }
                case MARKER_PICK_START: {
                    tState = STATE_PICK;
                    break;
                }
                default: {
                    rOutLines.add(baseReplace_(tLine, aGenMap));
                    break;
                }}
                break;
            }
            case STATE_REMOVE: {
                if (tLine.trim().equals(MARKER_REMOVE_END)) {
                    tState = STATE_NORMAL;
                }
                break;
            }
            case STATE_PICK: {
                if (tLine.trim().startsWith(MARKER_PICK_END)) {
                    tState = STATE_NORMAL;
                    String tKey = tLine.trim().substring(MARKER_PICK_END.length()).trim();
                    Object tValue = aGenMap.get(tKey);
                    if (tValue==null) throw new IllegalStateException("Missing pick key: "+tKey);
                    rBuf1.clear();
                    boolean tInCase = false;
                    for (String tBufLine : rBuf0) {
                        if (tInCase) {
                            if (tBufLine.trim().startsWith(MARKER_PICK_CASE)) {
                                break;
                            } else {
                                rBuf1.add(tBufLine.replace("NEPGENO", "NEPGEN"));
                            }
                        } else {
                            if (tBufLine.trim().startsWith(MARKER_PICK_CASE)) {
                                String tCase = tBufLine.trim().substring(MARKER_PICK_CASE.length()).trim();
                                if (tCase.equals(tValue)) tInCase = true;
                            }
                        }
                    }
                    rBuf0.clear();
                    // 内部只核心逻辑，全部完成后递归处理后续
                    rOutLines.addAll(processLines_(rBuf1, aGenMap));
                } else {
                    rBuf0.add(tLine);
                }
                break;
            }
            default: {
                throw new IllegalStateException();
            }}
        }
        if (tState!=STATE_NORMAL) throw new IllegalStateException();
        return rOutLines;
    }
    private static String baseReplace_(String aLine, Map<String, Object> aGenMap) {
        // 简单串联，在没有遇到性能问题前都就这样做好了
        for (Map.Entry<String, Object> tEntry : aGenMap.entrySet()) {
            String tKey = tEntry.getKey();
            if (tKey.startsWith("[") && tKey.endsWith("]")) continue;
            aLine = aLine.replace("__"+tKey+"__", tEntry.getValue().toString());
        }
        return aLine;
    }
    
    void init_from_file(String potential_filename, String architecture) throws Exception {
        mInited = true;
        if (architecture.equals("cpu")) {
            mCuda = false;
        } else
        if (architecture.equals("cuda")) {
            mCuda = true;
        } else {
            throw new IllegalArgumentException("NEP architecture MUST be 'cpu' or 'cuda', input: " + architecture);
        }
        if (mCuda) {
            mSingle = true;
        } else {
            if (Conf.PRECISION.equals("single")) {
                mSingle = true;
            } else
            if (Conf.PRECISION.equals("double")) {
                mSingle = false;
            } else {
                throw new IllegalArgumentException("NEP precision MUST be 'double' or 'single', input: " + Conf.PRECISION);
            }
        }
        paramb = new ParaMB(mSingle, mCuda);
        annmb = new ANN(mSingle, mCuda);
        zbl = new ZBL(mSingle, mCuda);
        
        DoubleList parameters = new DoubleList();
        int num_para_descriptor;
        try (BufferedReader input = IO.toReader(potential_filename)) {
            // nep3 1 C
            String[] tokens = get_tokens(input);
            if (tokens.length < 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("The first line of nep.txt should have at least 3 items.");
            }
            switch(tokens[0]) {
            case "nep3": {
                paramb.model_type = 0;
                paramb.version = 3;
                zbl.enabled = false;
                break;
            }
            case "nep3_zbl": {
                paramb.model_type = 0;
                paramb.version = 3;
                zbl.enabled = true;
                break;
            }
            case "nep3_dipole": {
                paramb.model_type = 1;
                paramb.version = 3;
                zbl.enabled = false;
                break;
            }
            case "nep3_polarizability": {
                paramb.model_type = 2;
                paramb.version = 3;
                zbl.enabled = false;
                break;
            }
            case "nep4": {
                paramb.model_type = 0;
                paramb.version = 4;
                zbl.enabled = false;
                break;
            }
            case "nep4_zbl": {
                paramb.model_type = 0;
                paramb.version = 4;
                zbl.enabled = true;
                break;
            }
            case "nep4_dipole": {
                paramb.model_type = 1;
                paramb.version = 4;
                zbl.enabled = false;
                break;
            }
            case "nep4_polarizability": {
                paramb.model_type = 2;
                paramb.version = 4;
                zbl.enabled = false;
                break;
            }
            case "nep5": {
                paramb.model_type = 0;
                paramb.version = 5;
                zbl.enabled = false;
                break;
            }
            case "nep5_zbl": {
                paramb.model_type = 0;
                paramb.version = 5;
                zbl.enabled = true;
                break;
            }
            }
            
            paramb.num_types = Integer.parseInt(tokens[1]);
            if (tokens.length != 2 + paramb.num_types) {
                print_tokens(tokens);
                throw new IllegalArgumentException("The first line of nep.txt should have " + paramb.num_types + " atom symbols.");
            }
            
            element_list.clear();
            element_list.ensureCapacity(paramb.num_types);
            for (int n = 0; n < paramb.num_types; ++n) {
                int atomic_number = 0;
                element_list.add(tokens[2 + n]);
                for (int m = 0; m < NUM_ELEMENTS; ++m) {
                    if (ELEMENTS[m].equals(tokens[2 + n])) {
                        atomic_number = m;
                        break;
                    }
                }
                paramb.atomic_numbers.putAt(n, atomic_number);
            }
            
            // zbl 0.7 1.4
            if (zbl.enabled) {
                tokens = get_tokens(input);
                if (tokens.length != 3) {
                    print_tokens(tokens);
                    throw new IllegalArgumentException("This line should be zbl rc_inner rc_outer.");
                }
                zbl.rc_inner = Double.parseDouble(tokens[1]);
                zbl.rc_outer = Double.parseDouble(tokens[2]);
                if (zbl.rc_inner == 0 && zbl.rc_outer == 0) {
                    zbl.flexibled = true;
                }
            }
            
            // cutoff 4.2 3.7 80 47
            tokens = get_tokens(input);
            if (tokens.length != 5 && tokens.length != 8) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be cutoff rc_radial rc_angular MN_radial MN_angular [radial_factor] [angular_factor] [zbl_factor].");
            }
            paramb.rc_radial = Double.parseDouble(tokens[1]);
            paramb.rc_angular = Double.parseDouble(tokens[2]);
            int MN_radial = Integer.parseInt(tokens[3]);  // not used
            int MN_angular = Integer.parseInt(tokens[4]); // not used
            if (tokens.length == 8) {
                paramb.typewise_cutoff_radial_factor = Double.parseDouble(tokens[5]);
                paramb.typewise_cutoff_angular_factor = Double.parseDouble(tokens[6]);
                paramb.typewise_cutoff_zbl_factor = Double.parseDouble(tokens[7]);
                if (paramb.typewise_cutoff_radial_factor > 0.0) {
                    paramb.use_typewise_cutoff = true;
                }
                if (paramb.typewise_cutoff_zbl_factor > 0.0) {
                    paramb.use_typewise_cutoff_zbl = true;
                }
            }
            
            // n_max 10 8
            tokens = get_tokens(input);
            if (tokens.length != 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be n_max n_max_radial n_max_angular.");
            }
            paramb.n_max_radial = Integer.parseInt(tokens[1]);
            paramb.n_max_angular = Integer.parseInt(tokens[2]);
            
            // basis_size 10 8
            tokens = get_tokens(input);
            if (tokens.length != 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be basis_size basis_size_radial basis_size_angular.");
            }
            paramb.basis_size_radial = Integer.parseInt(tokens[1]);
            paramb.basis_size_angular = Integer.parseInt(tokens[2]);
            
            // l_max
            tokens = get_tokens(input);
            if (tokens.length != 4) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be l_max l_max_3body l_max_4body l_max_5body.");
            }
            paramb.L_max = Integer.parseInt(tokens[1]);
            paramb.num_L = paramb.L_max;
            
            int L_max_4body = Integer.parseInt(tokens[2]);
            int L_max_5body = Integer.parseInt(tokens[3]);
            if (L_max_4body == 2) {
                paramb.num_L += 1;
            }
            if (L_max_5body == 1) {
                paramb.num_L += 1;
            }
            paramb.dim_angular = (paramb.n_max_angular + 1) * paramb.num_L;
            
            // ANN
            tokens = get_tokens(input);
            if (tokens.length != 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be ANN num_neurons 0.");
            }
            annmb.num_neurons1 = Integer.parseInt(tokens[1]);
            annmb.dim = (paramb.n_max_radial + 1) + paramb.dim_angular;
            
            // calculated parameters:
            paramb.rcinv_radial = 1.0 / paramb.rc_radial;
            paramb.rcinv_angular = 1.0 / paramb.rc_angular;
            paramb.num_types_sq = paramb.num_types * paramb.num_types;
            if (paramb.version == 3) {
                annmb.num_para_ann = (annmb.dim + 2) * annmb.num_neurons1 + 1;
            } else if (paramb.version == 4) {
                annmb.num_para_ann = (annmb.dim + 2) * annmb.num_neurons1 * paramb.num_types + 1;
            } else {
                annmb.num_para_ann = ((annmb.dim + 2) * annmb.num_neurons1 + 1) * paramb.num_types + 1;
            }
            if (paramb.model_type == 2) {
                annmb.num_para_ann *= 2;
            }
            num_para_descriptor = paramb.num_types_sq * ((paramb.n_max_radial + 1) * (paramb.basis_size_radial + 1) +
                                                         (paramb.n_max_angular + 1) * (paramb.basis_size_angular + 1));
            annmb.num_para = annmb.num_para_ann + num_para_descriptor;
            paramb.num_c_radial = paramb.num_types_sq * (paramb.n_max_radial + 1) * (paramb.basis_size_radial + 1);
            
            // NN and descriptor parameters
            parameters.clear();
            parameters.ensureCapacity(annmb.num_para);
            for (int n = 0; n < annmb.num_para; ++n) {
                tokens = get_tokens(input);
                parameters.add(Double.parseDouble(tokens[0]));
            }
            for (int d = 0; d < annmb.dim; ++d) {
                tokens = get_tokens(input);
                paramb.q_scaler.putAtD(d, Double.parseDouble(tokens[0]));
            }
            
            // flexible zbl potential parameters if (zbl.flexibled)
            if (zbl.flexibled) {
                int num_type_zbl = (paramb.num_types * (paramb.num_types + 1)) / 2;
                for (int d = 0; d < 10 * num_type_zbl; ++d) {
                    tokens = get_tokens(input);
                    zbl.para.putAtD(d, Double.parseDouble(tokens[0]));
                }
                zbl.num_types = paramb.num_types;
            }
        }
        // init cpointer here
        Fp = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
        sum_fxyz = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
        
        gn_radial = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
        gnp_radial = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
        gn_angular = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
        gnp_angular = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
        
        mOutEng = mSingle ? FloatCPointer.malloc(1) : DoubleCPointer.malloc(1);
        mNlDx = mSingle ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlDy = mSingle ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlDz = mSingle ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlFx = mSingle ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlFy = mSingle ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlFz = mSingle ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlType = new GrowableIntCPointer(16);
        mNlIdx = new GrowableIntCPointer(16);
        
        // init cuda pointer here
        if (mCuda) {
            cuda_Fp = new GrowableFloatCudaPointer(1);
            cuda_sum_fxyz = new GrowableFloatCudaPointer(1);
            
            cuda_gn_radial = new GrowableFloatCudaPointer(1);
            cuda_gnp_radial = new GrowableFloatCudaPointer(1);
            cuda_gn_angular = new GrowableFloatCudaPointer(1);
            cuda_gnp_angular = new GrowableFloatCudaPointer(1);
            
            mFltBuf = new GrowableFloatCPointer(128);
            mIntBuf = new GrowableIntCPointer(1024);
            mCudaX = new GrowableFloatCudaPointer(128);
            mCudaF0 = new GrowableFloatCudaPointer(128);
            mCudaF1 = new GrowableFloatCudaPointer(128);
            mCudaEatom0 = new GrowableFloatCudaPointer(128);
            mCudaVatom0 = new GrowableFloatCudaPointer(128);
            mCudaVatom1 = new GrowableFloatCudaPointer(128);
            mCudaType = new GrowableIntCudaPointer(128);
            mCudaIlist = new GrowableIntCudaPointer(128);
            mCudaNumneigh = new GrowableIntCudaPointer(128);
            mCudaGNeiNum = new GrowableIntCudaPointer(128);
            mCudaGCType = new GrowableIntCudaPointer(128);
            mCudaFirstneigh = new GrowableIntCudaPointer(1024);
            mCudaGNlType = new GrowableIntCudaPointer(1024);
            mCudaGNlIdx = new GrowableIntCudaPointer(1024);
            mCudaGNlDx = new GrowableFloatCudaPointer(1024);
            mCudaGNlDy = new GrowableFloatCudaPointer(1024);
            mCudaGNlDz = new GrowableFloatCudaPointer(1024);
            mCudaGNlFx = new GrowableFloatCudaPointer(1024);
            mCudaGNlFy = new GrowableFloatCudaPointer(1024);
            mCudaGNlFz = new GrowableFloatCudaPointer(1024);
        }
        
        // compile nep here
        String tLibDir = IO.toParentPath(potential_filename);
        if (tLibDir!=null) mLibDir = tLibDir;
        String tProjectName = IO.toFileName(potential_filename).replace(".txt", "");
        tProjectName = PROJECT_INVALID_NAME.matcher(tProjectName).replaceAll("");
        if (!tProjectName.isEmpty()) mProjectName = tProjectName;
        compileJIT();
        
        DoubleCPointer parametersPtr = DoubleCPointer.malloc(parameters.size());
        parametersPtr.fill(parameters);
        update_potential(parametersPtr);

        if (Conf.USE_TABLE_FOR_RADIAL_FUNCTIONS) {
            if (paramb.use_typewise_cutoff) {
                throw new IllegalStateException("Cannot use tabulated radial functions with typewise cutoff.");
            }
            gn_radial.ensureCapacity((long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1));
            gnp_radial.ensureCapacity((long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1));
            gn_angular.ensureCapacity((long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1));
            gnp_angular.ensureCapacity((long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1));
            mDataOut.putAt(0, gn_radial);
            mDataOut.putAt(1, gn_angular);
            mDataOut.putAt(2, gnp_radial);
            mDataOut.putAt(3, gnp_angular);
            mConstructTable.invoke(parametersPtr, mDataOut);
            if (mCuda) {
                long count;
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1);
                cuda_gn_radial.ensureCapacity(count); cuda_gn_radial.fill((FloatCPointer)gn_radial, count);
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1);
                cuda_gnp_radial.ensureCapacity(count); cuda_gnp_radial.fill((FloatCPointer)gnp_radial, count);
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1);
                cuda_gn_angular.ensureCapacity(count); cuda_gn_angular.fill((FloatCPointer)gn_angular, count);
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1);
                cuda_gnp_angular.ensureCapacity(count); cuda_gnp_angular.fill((FloatCPointer)gnp_angular, count);
            }
        }
        parametersPtr.free();
        
        if (mCuda) {
            paramb.parse2cuda();
            zbl.parse2cuda();
        }
    }
    void update_potential(DoubleCPointer aPtr) throws CudaException {
        annmb.clear();
        int count;
        IDoubleOrFloatCPointer buf;
        DoubleCPointer ptr = aPtr.copy();
        for (int t = 0; t < paramb.num_types; ++t) {
            if (t > 0 && paramb.version == 3) { // Use the same set of NN parameters for NEP3
                ptr.leftShift((long) (annmb.dim + 2) * annmb.num_neurons1);
            }
            count = annmb.num_neurons1 * annmb.dim;
            buf = mSingle?FloatCPointer.malloc(count):DoubleCPointer.malloc(count); buf.fillD(ptr, count); ptr.rightShift(count);
            annmb.w0.putAt(t, buf);
            count = annmb.num_neurons1;
            buf = mSingle?FloatCPointer.malloc(count):DoubleCPointer.malloc(count); buf.fillD(ptr, count); ptr.rightShift(count);
            annmb.b0.putAt(t, buf);
            count = paramb.version==5 ? annmb.num_neurons1+1 : annmb.num_neurons1; // one extra bias for NEP5 stored in ann.w1[t]
            buf = mSingle?FloatCPointer.malloc(count):DoubleCPointer.malloc(count); buf.fillD(ptr, count); ptr.rightShift(count);
            annmb.w1.putAt(t, buf);
        }
        buf = mSingle?FloatCPointer.malloc(1):DoubleCPointer.malloc(1); buf.setD(ptr.get()); ptr.next();
        annmb.b1 = buf;
        
        if (paramb.model_type == 2) {
            for (int t = 0; t < paramb.num_types; ++t) {
                if (t > 0 && paramb.version == 3) { // Use the same set of NN parameters for NEP3
                    ptr.leftShift((long) (annmb.dim + 2) * annmb.num_neurons1);
                }
                count = annmb.num_neurons1 * annmb.dim;
                buf = mSingle?FloatCPointer.malloc(count):DoubleCPointer.malloc(count); buf.fillD(ptr, count); ptr.rightShift(count);
                annmb.w0_pol.putAt(t, buf);
                count = annmb.num_neurons1;
                buf = mSingle?FloatCPointer.malloc(count):DoubleCPointer.malloc(count); buf.fillD(ptr, count); ptr.rightShift(count);
                annmb.b0_pol.putAt(t, buf);
                count = annmb.num_neurons1;
                buf = mSingle ?FloatCPointer.malloc(count):DoubleCPointer.malloc(count); buf.fillD(ptr, count); ptr.rightShift(count);
                annmb.w1_pol.putAt(t, buf);
            }
            buf = mSingle?FloatCPointer.malloc(1):DoubleCPointer.malloc(1); buf.setD(ptr.get()); ptr.next();
            annmb.b1_pol = buf;
        }
        count = annmb.num_para - annmb.num_para_ann;
        buf = mSingle?FloatCPointer.malloc(count):DoubleCPointer.malloc(count); buf.fillD(ptr, count);
        annmb.c = buf;
        
        if (mCuda) {
            FloatCudaPointer bufCuda;
            for (int t = 0; t < paramb.num_types; ++t) {
                count = annmb.num_neurons1 * annmb.dim;
                bufCuda = FloatCudaPointer.malloc(count);
                bufCuda.fill(annmb.w0.getAsFloatCPointerAt(t), count);
                annmb.cuda0_w0.putAt(t, bufCuda);
                count = annmb.num_neurons1;
                bufCuda = FloatCudaPointer.malloc(count);
                bufCuda.fill(annmb.b0.getAsFloatCPointerAt(t), count);
                annmb.cuda0_b0.putAt(t, bufCuda);
                count = paramb.version==5 ? annmb.num_neurons1+1 : annmb.num_neurons1; // one extra bias for NEP5 stored in ann.w1[t]
                bufCuda = FloatCudaPointer.malloc(count);
                bufCuda.fill(annmb.w1.getAsFloatCPointerAt(t), count);
                annmb.cuda0_w1.putAt(t, bufCuda);
            }
            bufCuda = FloatCudaPointer.malloc(1);
            bufCuda.fill((FloatCPointer)annmb.b1, 1);
            annmb.cuda_b1 = bufCuda;
            annmb.cuda_w0.memcpy2this(annmb.cuda0_w0, paramb.num_types*AnyCPointer.TYPE_SIZE);
            annmb.cuda_b0.memcpy2this(annmb.cuda0_b0, paramb.num_types*AnyCPointer.TYPE_SIZE);
            annmb.cuda_w1.memcpy2this(annmb.cuda0_w1, paramb.num_types*AnyCPointer.TYPE_SIZE);
            
            if (paramb.model_type == 2) {
                for (int t = 0; t < paramb.num_types; ++t) {
                    count = annmb.num_neurons1 * annmb.dim;
                    bufCuda = FloatCudaPointer.malloc(count);
                    bufCuda.fill(annmb.w0_pol.getAsFloatCPointerAt(t), count);
                    annmb.cuda0_w0_pol.putAt(t, bufCuda);
                    count = annmb.num_neurons1;
                    bufCuda = FloatCudaPointer.malloc(count);
                    bufCuda.fill(annmb.b0_pol.getAsFloatCPointerAt(t), count);
                    annmb.cuda0_b0_pol.putAt(t, bufCuda);
                    count = annmb.num_neurons1;
                    bufCuda = FloatCudaPointer.malloc(count);
                    bufCuda.fill(annmb.w1_pol.getAsFloatCPointerAt(t), count);
                    annmb.cuda0_w1_pol.putAt(t, bufCuda);
                }
                bufCuda = FloatCudaPointer.malloc(1);
                bufCuda.fill((FloatCPointer)annmb.b1_pol, 1);
                annmb.cuda_b1_pol = bufCuda;
                annmb.cuda_w0_pol.memcpy2this(annmb.cuda0_w0_pol, paramb.num_types*AnyCPointer.TYPE_SIZE);
                annmb.cuda_b0_pol.memcpy2this(annmb.cuda0_b0_pol, paramb.num_types*AnyCPointer.TYPE_SIZE);
                annmb.cuda_w1_pol.memcpy2this(annmb.cuda0_w1_pol, paramb.num_types*AnyCPointer.TYPE_SIZE);
            }
            count = annmb.num_para - annmb.num_para_ann;
            bufCuda = FloatCudaPointer.malloc(count);
            bufCuda.fill((FloatCPointer)annmb.c, count);
            annmb.cuda_c = bufCuda;
        }
    }
    
    static String[] get_tokens(BufferedReader input) throws IOException {
        String line = input.readLine();
        if (line == null) return ZL_STR;
        return IO.Text.splitBlank(line);
    }
    static void print_tokens(String[] tokens) {
        System.err.print("Line:");
        for (String token : tokens) {
            System.err.print(" ");
            System.err.print(token);
        }
        System.err.println();
    }
}
