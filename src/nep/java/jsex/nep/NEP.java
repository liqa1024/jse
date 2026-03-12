package jsex.nep;

import jse.atom.IPairPotential;
import jse.clib.Compiler;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.cptr.*;
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
    private final static String INTERFACE_NAME = "nep_interface.cpp";
    private final static String INTERFACE_HEAD_NAME = "nep_interface.h";
    private final static String SRC_NAME = "nep_core.hpp";
    
    NEP() {}
    public NEP(String aPotentialFileName) throws Exception {
        init_from_file(aPotentialFileName);
    }
    
    @Override public int atomTypeNumber() {return element_list.size();}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return element_list.get(aType-1);}
    @Override public double rcutMax() {return Math.max(paramb.rc_radial, paramb.rc_angular);}
    
    
    private final DoubleList mNlDxBuf = new DoubleList(16), mNlDyBuf = new DoubleList(16), mNlDzBuf = new DoubleList(16);
    private final IntList mNlTypeBuf = new IntList(16), mNlIdxBuf = new IntList(16);
    
    private int buildNL_(IDxyzTypeIdxIterable aNL, double aRCut) {
        final int tTypeNum = atomTypeNumber();
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
        mGradNlDx.ensureCapacity(tNeiNum);
        mGradNlDy.ensureCapacity(tNeiNum);
        mGradNlDz.ensureCapacity(tNeiNum);
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
            mDataOut.putAt(1, mGradNlDx);
            mDataOut.putAt(2, mGradNlDy);
            mDataOut.putAt(3, mGradNlDz);
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
            mDataOut.putAt(1, mGradNlDx);
            mDataOut.putAt(2, mGradNlDy);
            mDataOut.putAt(3, mGradNlDz);
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
                double fx = mGradNlDx.getAtD(j);
                double fy = mGradNlDy.getAtD(j);
                double fz = mGradNlDz.getAtD(j);
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
        mGradNlDx.ensureCapacity(aNeiNum);
        mGradNlDy.ensureCapacity(aNeiNum);
        mGradNlDz.ensureCapacity(aNeiNum);
    }
    void computeLammps(PairNEP aPair) {
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
        mDataOut.putAt(1, mGradNlDx);
        mDataOut.putAt(2, mGradNlDy);
        mDataOut.putAt(3, mGradNlDz);
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
        DoubleCPointer q_scaler = DoubleCPointer.malloc(140);
        IntCPointer atomic_numbers = IntCPointer.malloc(NUM_ELEMENTS);
        
        private boolean mFree = false;
        void free() {
            if (mFree) throw new IllegalStateException();
            mFree = true;
            q_scaler.free();
            atomic_numbers.free();
        }
    }
    static class ANN {
        private boolean inited = false;
        int dim = 0;
        int num_neurons1 = 0;
        int num_para = 0;
        int num_para_ann = 0;
        NestedDoubleCPointer w0 = NestedDoubleCPointer.calloc(NUM_ELEMENTS);
        NestedDoubleCPointer b0 = NestedDoubleCPointer.calloc(NUM_ELEMENTS);
        NestedDoubleCPointer w1 = NestedDoubleCPointer.calloc(NUM_ELEMENTS);
        DoubleCPointer b1 = null;
        DoubleCPointer c = null;
        // for the scalar part of polarizability
        NestedDoubleCPointer w0_pol = NestedDoubleCPointer.calloc(NUM_ELEMENTS);
        NestedDoubleCPointer b0_pol = NestedDoubleCPointer.calloc(NUM_ELEMENTS);
        NestedDoubleCPointer w1_pol = NestedDoubleCPointer.calloc(NUM_ELEMENTS);
        DoubleCPointer b1_pol = null;
        
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
            b1.free();
            c.free();
            b1_pol.free();
        }
        private void getAndFree_(int i, NestedDoubleCPointer aNestedPtr) {
            DoubleCPointer tPtr = aNestedPtr.getAt(i);
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
        }
    }
    static class ZBL {
        boolean enabled = false;
        boolean flexibled = false;
        int num_types;
        double rc_inner = 1.0;
        double rc_outer = 2.0;
        DoubleCPointer para = DoubleCPointer.calloc(550);
        
        private boolean mFree = false;
        void free() {
            if (mFree) throw new IllegalStateException();
            mFree = true;
            para.free();
        }
    }
    
    ParaMB paramb = new ParaMB();
    ANN annmb = new ANN();
    ZBL zbl = new ZBL();
    GrowableDoubleCPointer Fp = new GrowableDoubleCPointer(1);
    GrowableDoubleCPointer sum_fxyz = new GrowableDoubleCPointer(1);
    ArrayList<String> element_list = new ArrayList<>();
    
    GrowableDoubleCPointer gn_radial = new GrowableDoubleCPointer(1);   // tabulated gn_radial functions
    GrowableDoubleCPointer gnp_radial = new GrowableDoubleCPointer(1);  // tabulated gnp_radial functions
    GrowableDoubleCPointer gn_angular = new GrowableDoubleCPointer(1);  // tabulated gn_angular functions
    GrowableDoubleCPointer gnp_angular = new GrowableDoubleCPointer(1); // tabulated gnp_angular functions
    
    boolean mSinglePrecision = false;
    AnyCPointer mDataIn = AnyCPointer.calloc(32), mDataOut = AnyCPointer.calloc(32);
    IntCPointer mInNums = IntCPointer.malloc(32), mOutNums = IntCPointer.malloc(32);
    DoubleCPointer mOutEng = DoubleCPointer.malloc(1);
    GrowableDoubleCPointer mNlDx = new GrowableDoubleCPointer(16);
    GrowableDoubleCPointer mNlDy = new GrowableDoubleCPointer(16);
    GrowableDoubleCPointer mNlDz = new GrowableDoubleCPointer(16);
    GrowableDoubleCPointer mGradNlDx = new GrowableDoubleCPointer(16);
    GrowableDoubleCPointer mGradNlDy = new GrowableDoubleCPointer(16);
    GrowableDoubleCPointer mGradNlDz = new GrowableDoubleCPointer(16);
    GrowableIntCPointer mNlType = new GrowableIntCPointer(16), mNlIdx = new GrowableIntCPointer(16);
    /// jit stuffs
    IJITEngine mJITEngine = null;
    private static final String NAME_CAL_ENERGY = "jse_nep_calEnergy", NAME_CAL_ENERGYFORCE = "jse_nep_calEnergyForce";
    private static final String NAME_CONSTRUCT_TABLE = "jse_nep_constructTable";
    private static final String NAME_STAT_NEINUM_LAMMPS = "jse_nep_statNeiNumLammps", NAME_COMPUTE_LAMMPS = "jse_nep_computeLammps";
    private IJITMethod mCalEnergy = null, mCalEnergyForce = null;
    private IJITMethod mConstructTable = null;
    private IJITMethod mStatNeiNumLammps = null, mComputeLammps = null;
    private String mLibDir = OS.WORKING_DIR, mProjectName = JIT_NAME;
    
    private final static Pattern PROJECT_INVALID_NAME = Pattern.compile("[^a-zA-Z0-9_\\-]");
    
    private static final String MARKER_REMOVE_START = "// >>> NEPGEN REMOVE";
    private static final String MARKER_REMOVE_END = "// <<< NEPGEN REMOVE";
    private static final int STATE_NORMAL = 0, STATE_REMOVE = 1;
    
    boolean mDead = false;
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        
        mDataIn.free(); mDataOut.free();
        mInNums.free();
        mOutEng.free();
        Fp.free(); sum_fxyz.free();
        gn_radial.free(); gnp_radial.free();
        gn_angular.free(); gnp_angular.free();
        mNlDx.free(); mNlDy.free(); mNlDz.free();
        mGradNlDx.free(); mGradNlDy.free(); mGradNlDz.free();
        mNlType.free(); mNlIdx.free();
        if (mJITEngine!=null) {
            mJITEngine.shutdown();
        }
        paramb.free();
        annmb.free();
        zbl.free();
    }
    
    void compileJIT() throws Exception {
        Map<String, Object> rGenMap = initGenMap_();
        rGenMap.put("[PRECISION]", mSinglePrecision ? "single" : "double");
        rGenMap.put("[ARCH]", "cpu");
        String tUniqueID = UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, NEP.VERSION_SHA,
                                            rGenMap, Conf.OPTIM_LEVEL, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING);
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
        mConstructTable = mJITEngine.findMethod(NAME_CONSTRUCT_TABLE);
        mStatNeiNumLammps = mJITEngine.findMethod(NAME_STAT_NEINUM_LAMMPS);
        mComputeLammps = mJITEngine.findMethod(NAME_COMPUTE_LAMMPS);
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
    @SuppressWarnings({"SwitchStatementWithTooFewBranches"})
    private static List<String> processLines_(List<String> aLines, Map<String, Object> aGenMap) {
        int tState = STATE_NORMAL;
        List<String> rOutLines = new ArrayList<>(aLines.size());
        for (String tLine : aLines) {
            switch(tState) {
            case STATE_NORMAL: {
                switch(tLine.trim()) {
                case MARKER_REMOVE_START: {
                    tState = STATE_REMOVE;
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
    
    void init_from_file(String potential_filename) throws Exception {
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
                paramb.q_scaler.putAt(d, Double.parseDouble(tokens[0]));
            }
            
            // flexible zbl potential parameters if (zbl.flexibled)
            if (zbl.flexibled) {
                int num_type_zbl = (paramb.num_types * (paramb.num_types + 1)) / 2;
                for (int d = 0; d < 10 * num_type_zbl; ++d) {
                    tokens = get_tokens(input);
                    zbl.para.putAt(d, Double.parseDouble(tokens[0]));
                }
                zbl.num_types = paramb.num_types;
            }
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
        }
        parametersPtr.free();
    }
    void update_potential(DoubleCPointer ptr) {
        annmb.clear();
        int count;
        DoubleCPointer buf;
        for (int t = 0; t < paramb.num_types; ++t) {
            if (t > 0 && paramb.version == 3) { // Use the same set of NN parameters for NEP3
                ptr.leftShift((long) (annmb.dim + 2) * annmb.num_neurons1);
            }
            count = annmb.num_neurons1 * annmb.dim;
            buf = DoubleCPointer.malloc(count); buf.fill(ptr, count); ptr.rightShift(count);
            annmb.w0.putAt(t, buf);
            count = annmb.num_neurons1;
            buf = DoubleCPointer.malloc(count); buf.fill(ptr, count); ptr.rightShift(count);
            annmb.b0.putAt(t, buf);
            count = paramb.version==5 ? annmb.num_neurons1+1 : annmb.num_neurons1; // one extra bias for NEP5 stored in ann.w1[t]
            buf = DoubleCPointer.malloc(count); buf.fill(ptr, count); ptr.rightShift(count);
            annmb.w1.putAt(t, buf);
        }
        buf = DoubleCPointer.malloc(1); buf.set(ptr.get()); ptr.next();
        annmb.b1 = buf;
        
        if (paramb.model_type == 2) {
            for (int t = 0; t < paramb.num_types; ++t) {
                if (t > 0 && paramb.version == 3) { // Use the same set of NN parameters for NEP3
                    ptr.leftShift((long) (annmb.dim + 2) * annmb.num_neurons1);
                }
                count = annmb.num_neurons1 * annmb.dim;
                buf = DoubleCPointer.malloc(count); buf.fill(ptr, count); ptr.rightShift(count);
                annmb.w0_pol.putAt(t, buf);
                count = annmb.num_neurons1;
                buf = DoubleCPointer.malloc(count); buf.fill(ptr, count); ptr.rightShift(count);
                annmb.b0_pol.putAt(t, buf);
                count = annmb.num_neurons1;
                buf = DoubleCPointer.malloc(count); buf.fill(ptr, count); ptr.rightShift(count);
                annmb.w1_pol.putAt(t, buf);
            }
            buf = DoubleCPointer.malloc(1); buf.set(ptr.get()); ptr.next();
            annmb.b1_pol = buf;
        }
        count = annmb.num_para - annmb.num_para_ann;
        buf = DoubleCPointer.malloc(count); buf.fill(ptr, count);
        annmb.c = buf;
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
