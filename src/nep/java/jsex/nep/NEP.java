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
        mPtrMng.ensureCapacity(mNlDx, tNeiNum); mNlDx.fillD(mNlDxBuf);
        mPtrMng.ensureCapacity(mNlDy, tNeiNum); mNlDy.fillD(mNlDyBuf);
        mPtrMng.ensureCapacity(mNlDz, tNeiNum); mNlDz.fillD(mNlDzBuf);
        mPtrMng.ensureCapacity(mNlType, tNeiNum); mNlType.fill(mNlTypeBuf);
        mPtrMng.ensureCapacity(mNlFx, tNeiNum);
        mPtrMng.ensureCapacity(mNlFy, tNeiNum);
        mPtrMng.ensureCapacity(mNlFz, tNeiNum);
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
        mPtrMng.ensureCapacity(Fp, annmb.dim);
        mPtrMng.ensureCapacity(sum_fxyz, (long) (paramb.n_max_angular + 1)*NUM_OF_ABC);
        aNeighborListGetter.forEachNLWithException(null, null, (threadID, cIdx, cType, nl) -> {
            // 近邻列表构建以及相关值设置
            int tNeiNum = buildNL_(nl, rcutMax());
            // 调用 jit 方法获取结果
            mCalEnergy.invoke(
                mNlDx, mNlDy, mNlDz, mNlType, tNeiNum, cType-1,
                paramb.atomic_numbers, paramb.q_scaler,
                annmb.w0, annmb.b0, annmb.w1, annmb.b1, annmb.c,
                zbl.para, gn_radial, gn_angular,
                mOutEng, mNlFx, mNlFy, mNlFz,
                Fp, sum_fxyz
            );
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
        mPtrMng.ensureCapacity(Fp, annmb.dim);
        mPtrMng.ensureCapacity(sum_fxyz, (long) (paramb.n_max_angular + 1)*NUM_OF_ABC);
        aNeighborListGetter.forEachNLWithException(null, null, (threadID, cIdx, cType, nl) -> {
            // 近邻列表构建以及相关值设置
            int tNeiNum = buildNL_(nl, rcutMax());
            // 调用 jit 方法获取结果
            mCalEnergyForce.invoke(
                mNlDx, mNlDy, mNlDz, mNlType, tNeiNum, cType-1,
                paramb.atomic_numbers, paramb.q_scaler,
                annmb.w0, annmb.b0, annmb.w1, annmb.b1, annmb.c,
                zbl.para, gn_radial, gn_angular, gnp_radial, gnp_angular,
                mOutEng, mNlFx, mNlFy, mNlFz,
                Fp, sum_fxyz
            );
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
        mPtrMng.ensureCapacity(mNlDx, aNeiNum);
        mPtrMng.ensureCapacity(mNlDy, aNeiNum);
        mPtrMng.ensureCapacity(mNlDz, aNeiNum);
        mPtrMng.ensureCapacity(mNlType, aNeiNum);
        mPtrMng.ensureCapacity(mNlIdx, aNeiNum);
        mPtrMng.ensureCapacity(mNlFx, aNeiNum);
        mPtrMng.ensureCapacity(mNlFy, aNeiNum);
        mPtrMng.ensureCapacity(mNlFz, aNeiNum);
    }
    void computeLammps(PairNEP aPair) {
        if (mDead) throw new IllegalStateException("This NEP is dead");
        if (!mInited) throw new IllegalStateException();
        if (mCuda) throw new IllegalStateException();
        // 近邻列表大小获取和缓存合理化
        int inum = aPair.listInum();
        IntCPointer ilist = aPair.listIlist();
        IntCPointer numneigh = aPair.listNumneigh();
        mStatNeiNumLammps.invoke(ilist, numneigh, inum, mOutNums);
        validNlLammps_(mOutNums.getAt(0));
        
        mPtrMng.ensureCapacity(Fp, annmb.dim);
        mPtrMng.ensureCapacity(sum_fxyz, (long) (paramb.n_max_angular + 1)*NUM_OF_ABC);
        // 调用 jit 方法计算
        mComputeLammps.invoke(
            inum, aPair.eflagEither()?1:0, aPair.vflagEither()?1:0, aPair.eflagAtom()?1:0, aPair.vflagAtom()?1:0, aPair.cvflagAtom()?1:0,
            aPair.atomX(), aPair.atomF(), aPair.atomType(),
            ilist, numneigh, aPair.listFirstneigh(), aPair.mCutoffsq, aPair.mTypeMap,
            aPair.engVdwl(), aPair.eatom(), aPair.virial(), aPair.vatom(), aPair.cvatom(),
            mNlDx, mNlDy, mNlDz, mNlType, mNlIdx,
            paramb.atomic_numbers, paramb.q_scaler,
            annmb.w0, annmb.b0, annmb.w1, annmb.b1, annmb.c,
            zbl.para, gn_radial, gn_angular, gnp_radial, gnp_angular,
            mNlFx, mNlFy, mNlFz,
            Fp, sum_fxyz
        );
    }
    
    
    private boolean mCudaParaInited = false;
    private void initLmpParamCuda_(PairNEP aPair) throws CudaException {
        if (mCudaParaInited) return;
        mCudaParaInited = true;
        mCudaTypeMap = mPtrMng.newIntCudaPointer(aPair.mTypeNum+1);
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
        mPtrMng.ensureCapacity(mFltBuf, (long)nlocalghost*9);
        mPtrMng.ensureCapacity(mCudaX, (long)nlocalghost*3);
        mPtrMng.ensureCapacity(mCudaF, (long)nlocalghost*3);
        mPtrMng.ensureCapacity(mCudaEatom0, (long)inum);
        mPtrMng.ensureCapacity(mCudaVatom0, (long)inum*6);
        mPtrMng.ensureCapacity(mCudaVatom1, (long)nlocalghost*(cvflagAtom?9:6));
        mPtrMng.ensureCapacity(mCudaType, nlocalghost);
        if (nlflag) {
            mPtrMng.ensureCapacity(mCudaIlist, inum);
            mPtrMng.ensureCapacity(mCudaNumneigh, inum);
        }
        mPtrMng.ensureCapacity(mCudaGNeiNum, inum);
        mPtrMng.ensureCapacity(mCudaGCType, inum);
        mPtrMng.ensureCapacity(cuda_Fp, (long)inum*annmb.dim);
        mPtrMng.ensureCapacity(cuda_sum_fxyz, (long)inum*(paramb.n_max_angular+1)*NUM_OF_ABC);
        // 近邻列表大小获取和缓存合理化
        IPointer ilist = NULL;
        IPointer numneigh = NULL;
        IPointer firstneigh = NULL;
        if (nlflag) {
            ilist = aPair.listIlist();
            numneigh = aPair.listNumneigh();
            firstneigh = aPair.listFirstneigh();
            mStatNeiNumLammps.invoke(ilist, numneigh, inum, mOutNums);
            mNumneighMax = mOutNums.getAt(0);
        }
        // 近邻列表缓存向量长度规范
        if (nlflag) {
            int tTotNeiNum = inum*mNumneighMax;
            mPtrMng.ensureCapacity(mIntBuf, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaFirstneigh, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlType, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlIdx, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlDx, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlDy, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlDz, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlFx, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlFy, tTotNeiNum);
            mPtrMng.ensureCapacity(mCudaGNlFz, tTotNeiNum);
        }
        
        // lammps -> cuda
        int tCode = mLammps2Cuda.invoke(
            inum, nlocalghost, nlflag?1:0, mNumneighMax,
            aPair.atomX(), aPair.atomType(),
            ilist, numneigh, firstneigh,
            mFltBuf, mIntBuf,
            mCudaX, mCudaType,
            nlflag?mCudaIlist:NULL, nlflag?mCudaNumneigh:NULL, nlflag?mCudaFirstneigh:NULL
        );
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda compute
        tCode = mComputeLammpsCuda.invoke(
            inum, nlocalghost, aPair.eflagEither()?1:0, aPair.vflagEither()?1:0, aPair.eflagAtom()?1:0, aPair.vflagAtom()?1:0, cvflagAtom?1:0,
            mCudaX, mCudaType, mCudaIlist, mCudaNumneigh, mCudaFirstneigh, aPair.mCutoffsq, mCudaTypeMap,
            paramb.cuda_atomic_numbers, paramb.cuda_q_scaler,
            annmb.cuda_w0, annmb.cuda_b0, annmb.cuda_w1, annmb.cuda_b1, annmb.cuda_c,
            zbl.cuda_para, cuda_gn_radial, cuda_gn_angular, cuda_gnp_radial, cuda_gnp_angular,
            mCudaF, mCudaEatom0, mCudaVatom0, mCudaVatom1,
            mCudaGNlDx, mCudaGNlDy, mCudaGNlDz, mCudaGNlType, mCudaGNlIdx, mCudaGNeiNum, mCudaGCType,
            mCudaGNlFx, mCudaGNlFy, mCudaGNlFz,
            cuda_Fp, cuda_sum_fxyz
        );
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda -> lammps
        tCode = mCuda2Lammps.invoke(
            inum, nlocalghost, aPair.eflagEither()?1:0, aPair.vflagEither()?1:0, aPair.eflagAtom()?1:0, aPair.vflagAtom()?1:0, cvflagAtom?1:0,
            aPair.atomF(), aPair.engVdwl(), aPair.eatom(), aPair.virial(), aPair.vatom(), aPair.cvatom(),
            mFltBuf, ilist,
            mCudaF, mCudaEatom0, mCudaVatom0, mCudaVatom1
        );
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
        final IntCPointer atomic_numbers;
        final boolean cuda;
        final FloatCudaPointer cuda_q_scaler;
        final IntCudaPointer cuda_atomic_numbers;
        ParaMB(PointerManager ptrMng, boolean single, boolean cuda) throws CudaException {
            atomic_numbers = ptrMng.newIntCPointer(NUM_ELEMENTS);
            q_scaler = ptrMng.newDoubleOrFloatCPointer(single, 140);
            this.cuda = cuda;
            cuda_q_scaler = cuda ? ptrMng.newFloatCudaPointer(140) : null;
            cuda_atomic_numbers = cuda ? ptrMng.newIntCudaPointer(NUM_ELEMENTS) : null;
        }
        void parse2cuda() throws CudaException {
            if (!cuda) throw new IllegalStateException();
            cuda_q_scaler.fill((FloatCPointer)q_scaler, 140);
            cuda_atomic_numbers.fill(atomic_numbers, NUM_ELEMENTS);
        }
    }
    static class ANN {
        private boolean inited = false;
        final PointerManager ptrMng;
        int dim = 0;
        int num_neurons1 = 0;
        int num_para = 0;
        int num_para_ann = 0;
        final AnyCPointer w0;
        final AnyCPointer b0;
        final AnyCPointer w1;
        IDoubleOrFloatCPointer b1 = null;
        IDoubleOrFloatCPointer c = null;
        // for the scalar part of polarizability
        final AnyCPointer w0_pol;
        final AnyCPointer b0_pol;
        final AnyCPointer w1_pol;
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
        ANN(PointerManager ptrMng, boolean single, boolean cuda) throws CudaException {
            this.ptrMng = ptrMng;
            w0 = ptrMng.newAnyCPointer(NUM_ELEMENTS);
            b0 = ptrMng.newAnyCPointer(NUM_ELEMENTS);
            w1 = ptrMng.newAnyCPointer(NUM_ELEMENTS);
            w0_pol = ptrMng.newAnyCPointer(NUM_ELEMENTS);
            b0_pol = ptrMng.newAnyCPointer(NUM_ELEMENTS);
            w1_pol = ptrMng.newAnyCPointer(NUM_ELEMENTS);
            this.cuda = cuda;
            cuda0_w0 = cuda ? ptrMng.newAnyCPointer(NUM_ELEMENTS) : null;
            cuda0_b0 = cuda ? ptrMng.newAnyCPointer(NUM_ELEMENTS) : null;
            cuda0_w1 = cuda ? ptrMng.newAnyCPointer(NUM_ELEMENTS) : null;
            cuda0_w0_pol = cuda ? ptrMng.newAnyCPointer(NUM_ELEMENTS) : null;
            cuda0_b0_pol = cuda ? ptrMng.newAnyCPointer(NUM_ELEMENTS) : null;
            cuda0_w1_pol = cuda ? ptrMng.newAnyCPointer(NUM_ELEMENTS) : null;
            cuda_w0 = cuda ? ptrMng.newCudaPointer(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_b0 = cuda ? ptrMng.newCudaPointer(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_w1 = cuda ? ptrMng.newCudaPointer(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_w0_pol = cuda ? ptrMng.newCudaPointer(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_b0_pol = cuda ? ptrMng.newCudaPointer(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
            cuda_w1_pol = cuda ? ptrMng.newCudaPointer(NUM_ELEMENTS*AnyCPointer.TYPE_SIZE) : null;
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
            ptrMng.free(b1); b1 = null;
            ptrMng.free(c); c = null;
            if (b1_pol!=null) {
                ptrMng.free(b1_pol);
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
                    ptrMng.free(cuda_b1); cuda_b1 = null;
                    ptrMng.free(cuda_c); c = null;
                    if (cuda_b1_pol!=null) {
                        ptrMng.free(cuda_b1_pol);
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
                ptrMng.free(tPtr);
                aNestedPtr.putAt(i, NULL);
            }
        }
        private void getAndFreeCuda_(int i, AnyCPointer aNestedPtr) throws CudaException {
            CudaPointer tPtr = aNestedPtr.getAsCudaPointerAt(i);
            if (!tPtr.isNull()) {
                ptrMng.free(tPtr);
                aNestedPtr.putAt(i, NULL);
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
        ZBL(PointerManager ptrMng, boolean single, boolean cuda) throws CudaException {
            para = ptrMng.newDoubleOrFloatCPointer(single, 550);
            this.cuda = cuda;
            cuda_para = cuda ? ptrMng.newFloatCudaPointer(550) : null;
        }
        void parse2cuda() throws CudaException {
            if (!cuda) throw new IllegalStateException();
            cuda_para.fill((FloatCPointer)para, 550);
        }
    }
    
    ParaMB paramb = null;
    ANN annmb = null;
    ZBL zbl = null;
    IDoubleOrFloatCPointer Fp = null, sum_fxyz = null;
    ArrayList<String> element_list = new ArrayList<>();
    
    IDoubleOrFloatCPointer gn_radial = null;   // tabulated gn_radial functions
    IDoubleOrFloatCPointer gnp_radial = null;  // tabulated gnp_radial functions
    IDoubleOrFloatCPointer gn_angular = null;  // tabulated gn_angular functions
    IDoubleOrFloatCPointer gnp_angular = null; // tabulated gnp_angular functions
    FloatCudaPointer cuda_gn_radial = null, cuda_gnp_radial = null, cuda_gn_angular = null, cuda_gnp_angular = null;
    
    final PointerManager mPtrMng = new PointerManager();
    boolean mInited = false, mSingle = false, mCuda = true;
    IntCPointer mOutNums = mPtrMng.newIntCPointer(16);
    IDoubleOrFloatCPointer mOutEng = null;
    IDoubleOrFloatCPointer mNlDx = null, mNlDy = null, mNlDz = null;
    IDoubleOrFloatCPointer mNlFx = null, mNlFy = null, mNlFz = null;
    IntCPointer mNlType = null, mNlIdx = null;
    
    /// gpu stuffs
    // cpu 数据
    private int mNumneighMax = -1;
    private FloatCPointer mFltBuf = null;
    private IntCPointer mIntBuf = null;
    // cuda 数据
    private FloatCudaPointer mCudaX = null, mCudaF = null, mCudaEatom0 = null, mCudaVatom0 = null, mCudaVatom1 = null;
    private IntCudaPointer mCudaType = null, mCudaIlist = null, mCudaNumneigh = null, mCudaGNeiNum = null, mCudaGCType = null;
    private IntCudaPointer mCudaFirstneigh = null, mCudaGNlType = null, mCudaGNlIdx = null;
    private FloatCudaPointer mCudaGNlDx = null, mCudaGNlDy = null, mCudaGNlDz = null, mCudaGNlFx = null, mCudaGNlFy = null, mCudaGNlFz = null;
    private IntCudaPointer mCudaTypeMap = null;
    private FloatCudaPointer cuda_Fp = null, cuda_sum_fxyz = null;
    
    /// jit stuffs
    IJITEngine mJITEngine = null;
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
        mPtrMng.close();
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
                .addTypeMap("JSE_NEP::flt_t", mSingle?"float":"double")
                .setLibDir(mLibDir).setProjectName(mProjectName+"_"+tUniqueID)
                .setSrc(codeGenStr_(IO.getResource("nep/src/"+INTERFACE_NAME_CUDA), rGenMap)).setNoExtern()
                .setSrcDirIniter((wd, engine) -> {
                    codeGen_(IO.getResource("nep/src/"+SRC_NAME), wd+SRC_NAME, rGenMap);
                    // 其余操作使用 jit 通用操作，确保 project name 同步
                    engine.writeCmakeFile(wd, INTERFACE_NAME_CUDA);
                    engine.writeHeadFile(wd, INTERFACE_HEAD_NAME_CUDA);
                    engine.writeSrcFile(wd, INTERFACE_NAME_CUDA, INTERFACE_HEAD_NAME_CUDA);
                    return wd;
                });
            mJITEngine.compile();
            mLammps2Cuda = mJITEngine.findMethod("jse_nep_lammps2cuda");
            mCuda2Lammps = mJITEngine.findMethod("jse_nep_cuda2lammps");
            mComputeLammpsCuda = mJITEngine.findMethod("jse_nep_computeLammpsCuda");
        } else {
            mJITEngine = SimpleJIT.engine()
                .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
                .setCmakeSettings(Conf.CMAKE_SETTING).setOptimLevel(Conf.OPTIM_LEVEL)
                .addTypeMap("JSE_NEP::flt_t", mSingle?"float":"double")
                .setLibDir(mLibDir).setProjectName(mProjectName+"_"+tUniqueID)
                .setSrc(codeGenStr_(IO.getResource("nep/src/"+INTERFACE_NAME), rGenMap)).setNoExtern()
                .setSrcDirIniter((wd, engine) -> {
                    codeGen_(IO.getResource("nep/src/"+SRC_NAME), wd+SRC_NAME, rGenMap);
                    // 其余操作使用 jit 通用操作，确保 project name 同步
                    engine.writeCmakeFile(wd, INTERFACE_NAME);
                    engine.writeHeadFile(wd, INTERFACE_HEAD_NAME);
                    engine.writeSrcFile(wd, INTERFACE_NAME, INTERFACE_HEAD_NAME);
                    return wd;
                });
            mJITEngine.compile();
            mCalEnergy = mJITEngine.findMethod("jse_nep_calEnergy");
            mCalEnergyForce = mJITEngine.findMethod("jse_nep_calEnergyForce");
            mComputeLammps = mJITEngine.findMethod("jse_nep_computeLammps");
        }
        mConstructTable = mJITEngine.findMethod("jse_nep_constructTable");
        mStatNeiNumLammps = mJITEngine.findMethod("jse_nep_statNeiNumLammps");
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
        try (BufferedReader tReader = IO.toReader(aSourceURL)) {
            tLines = IO.readAllLines(tReader);
        }
        IO.write(aTargetPath, processLines_(tLines, aGenMap));
    }
    private static String codeGenStr_(URL aSourceURL, Map<String, Object> aGenMap) throws Exception {
        List<String> tLines;
        try (BufferedReader tReader = IO.toReader(aSourceURL)) {
            tLines = IO.readAllLines(tReader);
        }
        return String.join("\n", processLines_(tLines, aGenMap));
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
        paramb = new ParaMB(mPtrMng, mSingle, mCuda);
        annmb = new ANN(mPtrMng, mSingle, mCuda);
        zbl = new ZBL(mPtrMng, mSingle, mCuda);
        
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
        Fp = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        sum_fxyz = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        
        gn_radial = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        gnp_radial = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        gn_angular = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        gnp_angular = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        
        mOutEng = mPtrMng.newDoubleOrFloatCPointer(mSingle, 1);
        mNlDx = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        mNlDy = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        mNlDz = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        mNlFx = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        mNlFy = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        mNlFz = mPtrMng.newDoubleOrFloatCPointer(mSingle);
        mNlType = mPtrMng.newIntCPointer();
        mNlIdx = mPtrMng.newIntCPointer();
        
        // init cuda pointer here
        if (mCuda) {
            cuda_Fp = mPtrMng.newFloatCudaPointer();
            cuda_sum_fxyz = mPtrMng.newFloatCudaPointer();
            
            cuda_gn_radial = mPtrMng.newFloatCudaPointer();
            cuda_gnp_radial = mPtrMng.newFloatCudaPointer();
            cuda_gn_angular = mPtrMng.newFloatCudaPointer();
            cuda_gnp_angular = mPtrMng.newFloatCudaPointer();
            
            mFltBuf = mPtrMng.newFloatCPointer(128);
            mIntBuf = mPtrMng.newIntCPointer(1024);
            mCudaX = mPtrMng.newFloatCudaPointer(128);
            mCudaF = mPtrMng.newFloatCudaPointer(128);
            mCudaEatom0 = mPtrMng.newFloatCudaPointer(128);
            mCudaVatom0 = mPtrMng.newFloatCudaPointer(128);
            mCudaVatom1 = mPtrMng.newFloatCudaPointer(128);
            mCudaType = mPtrMng.newIntCudaPointer(128);
            mCudaIlist = mPtrMng.newIntCudaPointer(128);
            mCudaNumneigh = mPtrMng.newIntCudaPointer(128);
            mCudaGNeiNum = mPtrMng.newIntCudaPointer(128);
            mCudaGCType = mPtrMng.newIntCudaPointer(128);
            mCudaFirstneigh = mPtrMng.newIntCudaPointer(1024);
            mCudaGNlType = mPtrMng.newIntCudaPointer(1024);
            mCudaGNlIdx = mPtrMng.newIntCudaPointer(1024);
            mCudaGNlDx = mPtrMng.newFloatCudaPointer(1024);
            mCudaGNlDy = mPtrMng.newFloatCudaPointer(1024);
            mCudaGNlDz = mPtrMng.newFloatCudaPointer(1024);
            mCudaGNlFx = mPtrMng.newFloatCudaPointer(1024);
            mCudaGNlFy = mPtrMng.newFloatCudaPointer(1024);
            mCudaGNlFz = mPtrMng.newFloatCudaPointer(1024);
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
            mPtrMng.ensureCapacity(gn_radial, (long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1));
            mPtrMng.ensureCapacity(gnp_radial, (long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1));
            mPtrMng.ensureCapacity(gn_angular, (long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1));
            mPtrMng.ensureCapacity(gnp_angular, (long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1));
            mConstructTable.invoke(parametersPtr, gn_radial, gn_angular, gnp_radial, gnp_angular);
            if (mCuda) {
                long count;
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1);
                mPtrMng.ensureCapacity(cuda_gn_radial, count); cuda_gn_radial.fill((FloatCPointer)gn_radial, count);
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_radial + 1);
                mPtrMng.ensureCapacity(cuda_gnp_radial, count); cuda_gnp_radial.fill((FloatCPointer)gnp_radial, count);
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1);
                mPtrMng.ensureCapacity(cuda_gn_angular, count); cuda_gn_angular.fill((FloatCPointer)gn_angular, count);
                count = (long) table_length * paramb.num_types_sq * (paramb.n_max_angular + 1);
                mPtrMng.ensureCapacity(cuda_gnp_angular, count); cuda_gnp_angular.fill((FloatCPointer)gnp_angular, count);
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
            buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, count); buf.fillD(ptr, count); ptr.rightShift(count);
            annmb.w0.putAt(t, buf);
            count = annmb.num_neurons1;
            buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, count); buf.fillD(ptr, count); ptr.rightShift(count);
            annmb.b0.putAt(t, buf);
            count = paramb.version==5 ? annmb.num_neurons1+1 : annmb.num_neurons1; // one extra bias for NEP5 stored in ann.w1[t]
            buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, count); buf.fillD(ptr, count); ptr.rightShift(count);
            annmb.w1.putAt(t, buf);
        }
        buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, 1); buf.setD(ptr.get()); ptr.next();
        annmb.b1 = buf;
        
        if (paramb.model_type == 2) {
            for (int t = 0; t < paramb.num_types; ++t) {
                if (t > 0 && paramb.version == 3) { // Use the same set of NN parameters for NEP3
                    ptr.leftShift((long) (annmb.dim + 2) * annmb.num_neurons1);
                }
                count = annmb.num_neurons1 * annmb.dim;
                buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, count); buf.fillD(ptr, count); ptr.rightShift(count);
                annmb.w0_pol.putAt(t, buf);
                count = annmb.num_neurons1;
                buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, count); buf.fillD(ptr, count); ptr.rightShift(count);
                annmb.b0_pol.putAt(t, buf);
                count = annmb.num_neurons1;
                buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, count); buf.fillD(ptr, count); ptr.rightShift(count);
                annmb.w1_pol.putAt(t, buf);
            }
            buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, 1); buf.setD(ptr.get()); ptr.next();
            annmb.b1_pol = buf;
        }
        count = annmb.num_para - annmb.num_para_ann;
        buf = mPtrMng.newDoubleOrFloatCPointer(mSingle, count); buf.fillD(ptr, count);
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
