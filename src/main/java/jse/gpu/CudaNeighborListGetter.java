package jse.gpu;

import jse.atom.XYZ;
import jse.clib.Compiler;
import jse.clib.JNIUtil;
import jse.clib.NVCC;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.timer.AccumulatedTimer;
import jse.cptr.*;
import jse.lmp.LmpPlugin;
import jse.math.MathEX;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.JAR_DIR;
import static jse.code.OS.JAVA_HOME;

/**
 * 基于 JNI 调用 cuda 实现的高效近邻列表获取器，用于最大限度提高 GPU 上的效率。
 * <p>
 * 目前实现主要针对 LAMMPS 已有的信息和数据格式设计
 * <p>
 * 由于 GPU 的并行性要求缓存所有的近邻，因此这里采用需要更少内存/显存的格式，并可以和 LAMMPS 一致
 *
 * @author liqa
 */
@ApiStatus.Experimental
public class CudaNeighborListGetter implements AutoCloseable {
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link CudaNeighborListGetter} 相关的 JNI 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link CudaNeighborListGetter} 相关的 JNI 库 */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            if (!INITIALIZED) String.valueOf(INIT_FLAG_);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义 cudanl 中使用的 block_size 值，这可能会影响速度；
         * 默认为 {@code 256}
         */
        public static int BLOCKSIZE = OS.envI("JSE_CUDANL_BLOCKSIZE", 256);
        
        /**
         * 自定义构建 cudanl 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_CUDANL");
        
        /**
         * 自定义构建 cudanl 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_COMPILER_CUDANL} 来设置
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_CUDANL", jse.code.Conf.CMAKE_CXX_COMPILER);
        /**
         * 自定义构建 cudanl 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_FLAGS_CUDANL} 来设置
         */
        public static @Nullable String CMAKE_CXX_FLAGS = OS.env("JSE_CMAKE_CXX_FLAGS_CUDANL", jse.code.Conf.CMAKE_CXX_FLAGS);
        /**
         * 自定义构建 cudanl 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CUDA_COMPILER_CUDANL} 来设置
         */
        public static @Nullable String CMAKE_CUDA_COMPILER = OS.env("JSE_CMAKE_CUDA_COMPILER_CUDANL");
        /**
         * 自定义构建 cudanl 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CUDA_FLAGS_CUDANL} 来设置
         */
        public static @Nullable String CMAKE_CUDA_FLAGS = OS.env("JSE_CMAKE_CUDA_FLAGS_CUDANL");
        /**
         * 自定义构建 cudanl 时的 cuda 架构，用于覆盖默认的 75
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CUDA_ARCHITECTURES_CUDANL} 来设置
         */
        public static @Nullable String CMAKE_CUDA_ARCHITECTURES = OS.env("JSE_CMAKE_CUDA_ARCHITECTURES_CUDANL");
    }
    
    /** 当前 {@link CudaNeighborListGetter} JNI 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR = JAR_DIR+"gpu/nl/" +
        UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, NVCC.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK,
                         Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_CUDA_COMPILER, Conf.CMAKE_CUDA_FLAGS,
                         Conf.CMAKE_CUDA_ARCHITECTURES, Conf.CMAKE_SETTING) + "/";
    /** 当前 {@link CudaNeighborListGetter} JNI 库的路径 */
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_gpu_CudaNeighborListGetter.cu"
        , "jse_gpu_CudaNeighborListGetter.h"
    };
    
    private static final boolean INIT_FLAG_;
    static {
        InitHelper.INITIALIZED = true;
        INIT_FLAG_ = true;
        // 依赖 CudaCore
        CudaCore.InitHelper.init();
        
        LIB_PATH = new JNIUtil.LibBuilder("cudanl", "CUDA_NL", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("cudanl", SRC_NAME)
            .setEnvChecker(NVCC::printInfo) // 在这里输出 nvcc 信息，保证只在第一次构建时输出一次；可能存在和 cmake 检测不一致的问题
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setCmakeCudaCompiler(Conf.CMAKE_CUDA_COMPILER).setCmakeCudaFlags(Conf.CMAKE_CUDA_FLAGS)
            .setCmakeCudaArch(Conf.CMAKE_CUDA_ARCHITECTURES)
            .get();
        // 设置库路径，这里直接使用 System.load
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    /// OOP sutffs
    final double mRCut, mRCutSq;
    final boolean mSortByType;
    final PointerManager mPtrMng;
    private final CudaPointer mCells;
    private final AnyCPointer mCellsCpu;
    private final IntCudaPointer mCellTot, mCellSize, mNlIdx, mNlSize;
    private final IntCPointer mCellSizeCpu, mNlSizeCpu;
    private int mLocalCellCapacity = -1, mGhostCellCapacity = -1, mNlCapacity = -1;
    private final IntCPointer mLocalCellMax, mGhostCellMax, mNlMax;
    
    private final FloatCudaPointer mPos;
    private FloatCudaPointer mPosX = null, mPosY = null, mPosZ = null;
    private final FloatCPointer mPosCpu;
    private final IntCudaPointer mType;
    private final IntCPointer mTypeCpu, mIListCpu;
    
    public CudaNeighborListGetter(double aRCut, boolean aSortByType) throws CudaException {
        mRCut = aRCut;
        mRCutSq = aRCut*aRCut;
        mSortByType = aSortByType;
        mPtrMng = new PointerManager();
        
        mCells = mPtrMng.newCudaPointer(0);
        mCellsCpu = mPtrMng.newAnyCPointer();
        mCellTot = mPtrMng.newIntCudaPointer();
        mCellSize = mPtrMng.newIntCudaPointer();
        mCellSizeCpu = mPtrMng.newIntCPointer();
        mNlIdx = mPtrMng.newIntCudaPointer();
        mNlSize = mPtrMng.newIntCudaPointer();
        mNlSizeCpu = mPtrMng.newIntCPointer();
        mLocalCellMax = mPtrMng.newIntCPointer(1);
        mGhostCellMax = mPtrMng.newIntCPointer(1);
        mNlMax = mPtrMng.newIntCPointer(1);
        mPos = mPtrMng.newFloatCudaPointer();
        mPosCpu = mPtrMng.newFloatCPointer();
        mType = mPtrMng.newIntCudaPointer();
        mTypeCpu = mPtrMng.newIntCPointer();
        mIListCpu = mPtrMng.newIntCPointer();
    }
    public CudaNeighborListGetter(double aRCut) throws CudaException {
        this(aRCut, false);
    }
    public final static int MAX_SLICE = 512;
    private int mSliceX = 0, mSliceY = 0, mSliceZ = 0;
    private boolean mPrism = false;
    private final XYZ mA = new XYZ(), mB = new XYZ(), mC = new XYZ();
    private final XYZ mBC = new XYZ(), mCA = new XYZ(), mAB = new XYZ();
    private double mVolume = Double.NaN;
    
    @Override public void close() {
        mPtrMng.close();
    }
    
    void initBox(double ax, double ay, double az,
                 double bx, double by, double bz,
                 double cx, double cy, double cz) {
        mPrism = true;
        mA.setXYZ(ax, ay, az);
        mB.setXYZ(bx, by, bz);
        mC.setXYZ(cx, cy, cz);
        mVolume = mA.mixed(mB, mC);
        
        mB.cross2dest(mC, mBC);
        mC.cross2dest(mA, mCA);
        mA.cross2dest(mB, mAB);
        double tPx = mA.dot(mBC) / mBC.norm();
        double tPy = mB.dot(mCA) / mCA.norm();
        double tPz = mC.dot(mAB) / mAB.norm();
        
        mSliceX = MathEX.Code.toRange(1, MAX_SLICE, MathEX.Code.floor2int(tPx/mRCut));
        mSliceY = MathEX.Code.toRange(1, MAX_SLICE, MathEX.Code.floor2int(tPy/mRCut));
        mSliceZ = MathEX.Code.toRange(1, MAX_SLICE, MathEX.Code.floor2int(tPz/mRCut));
    }
    void initBox(double x, double y, double z) {
        mPrism = false;
        mA.setXYZ(x, 0, 0);
        mB.setXYZ(0, y, 0);
        mC.setXYZ(0, 0, z);
        mVolume = x*y*z;
        
        mSliceX = MathEX.Code.toRange(1, MAX_SLICE, MathEX.Code.floor2int(x/mRCut));
        mSliceY = MathEX.Code.toRange(1, MAX_SLICE, MathEX.Code.floor2int(y/mRCut));
        mSliceZ = MathEX.Code.toRange(1, MAX_SLICE, MathEX.Code.floor2int(z/mRCut));
    }
    
    void initCells(int nlocal, int nghost) throws CudaException {
        final int tCellCount = (mSliceX+2)*(mSliceY+2)*(mSliceZ+2);
        final int tLocalCellCount = mSliceX*mSliceY*mSliceZ;
        final int tGhostCellCount = tCellCount-tLocalCellCount;
        final int tLocalCap = MathEX.Code.ceil2int(nlocal / (double)tLocalCellCount);
        final int tGhostCap = MathEX.Code.ceil2int(nghost / (double)tGhostCellCount);
        if (tLocalCap>mLocalCellCapacity || tGhostCap>mGhostCellCapacity) {
            if (tLocalCap>mLocalCellCapacity) mLocalCellCapacity = MathEX.Code.ceil2int(tLocalCap*1.5);
            if (tGhostCap>mGhostCellCapacity) mGhostCellCapacity = MathEX.Code.ceil2int(tGhostCap*2.0);
        }
        mPtrMng.ensureCapacity(mCellTot, (long)tLocalCellCount*mLocalCellCapacity + (long)tGhostCellCount*mGhostCellCapacity, false);
        mPtrMng.ensureCapacity(mCellSize, tCellCount);
        mPtrMng.ensureCapacity(mCellSizeCpu, tCellCount);
        mPtrMng.ensureCapacity(mCells, tCellCount*AnyCPointer.TYPE_SIZE);
        mPtrMng.ensureCapacity(mCellsCpu, tCellCount);
        int tCode = initCells0(
            mSliceX, mSliceY, mSliceZ,
            mCellTot.ptr_(), mCells.ptr_(), mCellsCpu.ptr_(),
            mLocalCellCapacity, mGhostCellCapacity
        );
        CudaCore.cudaExceptionCheck(tCode);
    }
    void buildCells(int nlocal, int nghost) throws CudaException {
        int tCode = buildCells0(
            Conf.BLOCKSIZE, nlocal, nghost, mPrism, (float)mA.mX, (float)mA.mY, (float)mA.mZ,
            (float)mB.mX, (float)mB.mY, (float)mB.mZ, (float)mC.mX, (float)mC.mY, (float)mC.mZ,
            mPos.ptr_(), mSliceX, mSliceY, mSliceZ,
            mCells.ptr_(), mCellSize.ptr_(), mCellSizeCpu.ptr_(),
            mLocalCellCapacity, mGhostCellCapacity,
            mLocalCellMax.ptr_(), mGhostCellMax.ptr_()
        );
        CudaCore.cudaExceptionCheck(tCode);
    }
    void validCells(int nlocal, int nghost) throws CudaException {
        // 检测是否 cell 大小存在超出，超出后需要重新构建
        int tLocalCellMax = mLocalCellMax.get();
        int tGhostCellMax = mGhostCellMax.get();
        if (tLocalCellMax>mLocalCellCapacity || tGhostCellMax>mGhostCellCapacity) {
            if (tLocalCellMax>mLocalCellCapacity) mLocalCellCapacity = MathEX.Code.ceil2int(tLocalCellMax*1.25);
            if (tGhostCellMax>mGhostCellCapacity) mGhostCellCapacity = MathEX.Code.ceil2int(tGhostCellMax*1.25);
            final int tLocalCellCount = mSliceX*mSliceY*mSliceZ;
            final int tGhostCellCount = (mSliceX+2)*(mSliceY+2)*(mSliceZ+2)-tLocalCellCount;
            mPtrMng.ensureCapacity(mCellTot, (long)tLocalCellCount*mLocalCellCapacity + (long)tGhostCellCount*mGhostCellCapacity, false);
            int tCode = initCells0(
                mSliceX, mSliceY, mSliceZ,
                mCellTot.ptr_(), mCells.ptr_(), mCellsCpu.ptr_(),
                mLocalCellCapacity, mGhostCellCapacity
            );
            CudaCore.cudaExceptionCheck(tCode);
            buildCells(nlocal, nghost);
        }
    }
    
    void initNl(int nlocal) throws CudaException {
        final int tNlCap = MathEX.Code.ceil2int(nlocal/mVolume * mRCut*mRCut*mRCut * (4.0/3.0*MathEX.PI));
        if (tNlCap > mNlCapacity) {
            mNlCapacity = MathEX.Code.ceil2int(tNlCap*1.25);
        }
        mPtrMng.ensureCapacity(mNlIdx, (long)nlocal*mNlCapacity, false);
        mPtrMng.ensureCapacity(mNlSize, nlocal);
        mPtrMng.ensureCapacity(mNlSizeCpu, nlocal);
    }
    void buildNl(int nlocal, int nghost) throws CudaException {
        int tCode = buildNl0(
            Conf.BLOCKSIZE, nlocal, nghost, mPrism, (float)mA.mX, (float)mA.mY, (float)mA.mZ,
            (float)mB.mX, (float)mB.mY, (float)mB.mZ, (float)mC.mX, (float)mC.mY, (float)mC.mZ,
            mPos.ptr_(), mSliceX, mSliceY, mSliceZ,
            mCells.ptr_(), mCellSize.ptr_(), (float)mRCutSq,
            mNlIdx.ptr_(), mNlSize.ptr_(), mNlSizeCpu.ptr_(), mNlCapacity, mNlMax.ptr_()
        );
        CudaCore.cudaExceptionCheck(tCode);
    }
    void validNl(int nlocal, int nghost) throws CudaException {
        // 检测是否 nl 大小存在超出，超出后需要重新构建
        int tNlMax = mNlMax.get();
        if (tNlMax > mNlCapacity) {
            mNlCapacity = MathEX.Code.ceil2int(tNlMax*1.25);
            mPtrMng.ensureCapacity(mNlIdx, (long)nlocal*mNlCapacity, false);
            buildNl(nlocal, nghost);
        }
    }
    
    
    public FloatCudaPointer posX() {
        return mPosX;
    }
    public FloatCudaPointer posY() {
        return mPosY;
    }
    public FloatCudaPointer posZ() {
        return mPosZ;
    }
    public IntCudaPointer type() {
        return mType;
    }
    public IntCPointer ilist() {
        return mIListCpu;
    }
    public IntCudaPointer nlIdx() {
        return mNlIdx;
    }
    public IntCudaPointer nlSize() {
        return mNlSize;
    }
    public int nlMax() {
        return mNlMax.get();
    }
    
    private final AccumulatedTimer mCopyTimer = new AccumulatedTimer(), mCellTimer = new AccumulatedTimer(), mNlTimer = new AccumulatedTimer();
    public double copyTime() {
        return mCopyTimer.get();
    }
    public double cellTime() {
        return mCellTimer.get();
    }
    public double nlTime() {
        return mNlTimer.get();
    }
    public void resetTimer() {
        mCopyTimer.reset();
        mCellTimer.reset();
        mNlTimer.reset();
    }
    
    public void build(LmpPlugin.Pair aPair) throws CudaException {
        final int nlocal = aPair.atomNlocal();
        final int nghost = aPair.atomNghost();
        DoubleCPointer tBoxLo = aPair.domainBoxlo();
        DoubleCPointer tBoxHi = aPair.domainBoxhi();
        
        mCopyTimer.from();
        double xlo = tBoxLo.getAt(0), ylo = tBoxLo.getAt(1), zlo = tBoxLo.getAt(2);
        mPtrMng.ensureCapacity(mPos, 3L*(nlocal+nghost));
        mPtrMng.ensureCapacity(mPosCpu, 3L*(nlocal+nghost));
        mPtrMng.ensureCapacity(mType, (nlocal+nghost));
        mPtrMng.ensureCapacity(mTypeCpu, (nlocal+nghost));
        mPtrMng.ensureCapacity(mIListCpu, (nlocal+nghost));
        initPosTypeLmp0(
            nlocal, nghost,
            (float)xlo, (float)ylo, (float)zlo,
            aPair.atomX().ptr_(), mPos.ptr_(), mPosCpu.ptr_(),
            aPair.atomType().ptr_(), mType.ptr_(), mTypeCpu.ptr_(),
            mSortByType, aPair.atomNtypes(), mIListCpu.ptr_()
        );
        mPosX = mPos.copy();
        mPosY = mPosX.plus(nlocal+nghost);
        mPosZ = mPosY.plus(nlocal+nghost);
        mCopyTimer.to();
        
        double ax = tBoxHi.getAt(0) - xlo;
        double by = tBoxHi.getAt(1) - ylo;
        double cz = tBoxHi.getAt(2) - zlo;
        if (aPair.domainTriclinic()) {
            double bx = aPair.domainXy().get();
            double cx = aPair.domainXz().get();
            double cy = aPair.domainYz().get();
            initBox(ax, 0, 0, bx, by, 0, cx, cy, cz);
        } else {
            initBox(ax, by, cz);
        }
        
        mCellTimer.from();
        initCells(nlocal, nghost);
        buildCells(nlocal, nghost);
        validCells(nlocal, nghost);
        mCellTimer.to();
        
        mNlTimer.from();
        initNl(nlocal);
        buildNl(nlocal, nghost);
        validNl(nlocal, nghost);
        mNlTimer.to();
    }
    
    private static native int initPosTypeLmp0(
        int nlocal, int nghost, float xlo, float ylo, float zlo,
        long posLmp, long pos, long posCpu,
        long typeLmp, long type, long typeCpu,
        boolean sortByType, int ntypes, long ilistCpu);
    
    private static native int initCells0(
        int sliceX, int sliceY, int sliceZ, long cellsTot, long cells, long cellsCpu,
        int localCellCapacity, int ghostCellCapacity);
    
    private static native int buildCells0(
        int aBlockSize, int nlocal, int nghost, boolean aPrism, float ax, float ay, float az,
        float bx, float by, float bz, float cx, float cy, float cz,
        long pos, int sliceX, int sliceY, int sliceZ,
        long cells, long cellSize, long cellSizeCpu,
        int localCellCapacity, int ghostCellCapacity,
        long localCellMax, long ghostCellMax);
    
    private static native int buildNl0(
        int aBlockSize, int nlocal, int nghost, boolean aPrism, float ax, float ay, float az,
        float bx, float by, float bz, float cx, float cy, float cz,
        long pos, int sliceX, int sliceY, int sliceZ,
        long cells, long cellSize, float rcutsq,
        long nl, long nlSize, long nlSizeCpu, int nlCapacity, long nlMax);
}
