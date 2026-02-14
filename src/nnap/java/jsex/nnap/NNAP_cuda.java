package jsex.nnap;

import jse.code.IO;
import jse.code.OS;
import jse.cptr.*;
import jse.gpu.*;
import jse.jit.IJITMethod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * jse 实现的 nnap gpu 版本
 * @author liqa
 */
@ApiStatus.Experimental
class NNAP_cuda extends NNAP2 {
    public final static class Conf {
        /**
         * 自定义构建 nnap 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_NNAP_CUDA", NNAP2.Conf.CMAKE_SETTING);
        /**
         * 自定义构建 nnap 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER  = OS.env("JSE_CMAKE_CXX_COMPILER_NNAP_CUDA", NNAP2.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS     = OS.env("JSE_CMAKE_CXX_FLAGS_NNAP_CUDA"   , NNAP2.Conf.CMAKE_CXX_FLAGS);
        public static @Nullable String CMAKE_CUDA_COMPILER = OS.env("JSE_CMAKE_CUDA_COMPILER_NNAP");
        public static @Nullable String CMAKE_CUDA_FLAGS    = OS.env("JSE_CMAKE_CUDA_FLAGS_NNAP");
        /**
         * 自定义构建 nnap 时的优化等级，
         * 默认会使用 BASE 优化
         */
        public static int OPTIM_LEVEL = OS.envI("JSE_NNAP_OPTIM_LEVEL_CUDA", NNAP2.Conf.OPTIM_LEVEL);
    }
    
    // cpu 数据
    private final GrowableInt64CPointer mDisplsneigh;
    private final GrowableFloatCPointer mFltBuf;
    private final GrowableIntCPointer mIntBuf;
    // cuda 数据
    private final GrowableFloatCudaPointer mCudaX;
    private final GrowableIntCudaPointer mCudaType, mCudaIlist, mCudaNumneigh;
    private final GrowableInt64CudaPointer mCudaDisplsneigh;
    private final GrowableIntCudaPointer mCudaFirstneigh;
    
    NNAP_cuda(@Nullable String aLibDir, @Nullable String aProjectName, Map<?, ?> aModelInfo) throws Exception {
        super(aLibDir, aProjectName, aModelInfo, 1, "single");
        mDisplsneigh = new GrowableInt64CPointer(128);
        mFltBuf = new GrowableFloatCPointer(128);
        mIntBuf = new GrowableIntCPointer(1024);
        mCudaX = new GrowableFloatCudaPointer(128);
        mCudaType = new GrowableIntCudaPointer(128);
        mCudaIlist = new GrowableIntCudaPointer(128);
        mCudaNumneigh = new GrowableIntCudaPointer(128);
        mCudaDisplsneigh = new GrowableInt64CudaPointer(128);
        mCudaFirstneigh = new GrowableIntCudaPointer(1024);
    }
    public NNAP_cuda(Map<?, ?> aModelInfo) throws Exception {
        this(null, null, aModelInfo);
        // 开始 jit 编译
        compileCuda_();
    }
    public NNAP_cuda(String aModelPath) throws Exception {
        this(IO.toParentPath(aModelPath), toValidProjectName(IO.toFileName(aModelPath)),
             aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath));
        // 开始 jit 编译
        compileCuda_();
    }
    
    // jit stuffs
    IJITMethod mStatDisplsneighLammps = null;
    IJITMethod mLammps2Cuda = null, mCuda2Lammps = null;
    private void compileCuda_() throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mJITEngine!=null) throw new IllegalStateException("compileCuda() has already been called");
        // 开始 jit
        mJITEngine = mNNAPGEN.initEngineCuda("cuda", NNAP2.VERSION, Conf.OPTIM_LEVEL, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING)
            .setCmakeCudaCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCudaFlags(Conf.CMAKE_CXX_FLAGS)
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setCmakeSettings(Conf.CMAKE_SETTING).setOptimLevel(Conf.OPTIM_LEVEL);
        mJITEngine.setMethodNames("jse_nnap_statDisplsneighLammps", "jse_nnap_lammps2cuda", "jse_nnap_cuda2lammps").compile();
        mStatDisplsneighLammps = mJITEngine.findMethod("jse_nnap_statDisplsneighLammps");
        mLammps2Cuda = mJITEngine.findMethod("jse_nnap_lammps2cuda");
        mCuda2Lammps = mJITEngine.findMethod("jse_nnap_cuda2lammps");
    }
    
    @Override void shutdown_() {
        super.shutdown_();
        mDisplsneigh.free();
        mFltBuf.free();
        mIntBuf.free();
        try {
            mCudaX.free();
            mCudaType.free();
            mCudaIlist.free();
            mCudaNumneigh.free();
            mCudaDisplsneigh.free();
            mCudaFirstneigh.free();
        } catch (CudaException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) throws Exception {
        throw new UnsupportedOperationException();
    }
    @Override public void calEnergyForceVirial(int aAtomNumber, INeighborListGetter aNeighborListGetter, @Nullable IEnergyAccumulator rEnergyAccumulator, @Nullable IForceAccumulator rForceAccumulator, @Nullable IVirialAccumulator rVirialAccumulator) throws Exception {
        throw new UnsupportedOperationException();
    }
    @Override void computeLammps(PairNNAP2 aPair) throws CudaException {
        // 常规缓存向量长度规范
        int inum = aPair.listInum();
        final int nlocal = aPair.atomNlocal();
        final int nghost = aPair.atomNghost();
        final int ntot = nlocal + nghost;
        mFltBuf.ensureCapacity((long)ntot*9);
        mDisplsneigh.ensureCapacity(nlocal+1);
        mCudaX.ensureCapacity((long)ntot*3);
        mCudaType.ensureCapacity(ntot);
        mCudaIlist.ensureCapacity(inum);
        mCudaNumneigh.ensureCapacity(nlocal);
        mCudaDisplsneigh.ensureCapacity(nlocal+1);
        // 近邻列表大小位置标识获取
        IntCPointer numneigh = aPair.listNumneigh();
        mInNums.putAt(0, nlocal);
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, numneigh);
        mStatDisplsneighLammps.invoke(mDataIn, mDisplsneigh);
        // 近邻列表缓存向量长度规范
        long tTotNeiNum = mDisplsneigh.getAt(nlocal);
        mIntBuf.ensureCapacity(tTotNeiNum);
        mCudaFirstneigh.ensureCapacity(tTotNeiNum);
        
        // lammps -> cuda
        mInNums.putAt(0, inum);
        mInNums.putAt(1, nlocal);
        mInNums.putAt(2, nghost);
        
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, aPair.atomX());
        mDataIn.putAt(2, aPair.atomType());
        mDataIn.putAt(3, aPair.listIlist());
        mDataIn.putAt(4, numneigh);
        mDataIn.putAt(5, mDisplsneigh);
        mDataIn.putAt(6, aPair.listFirstneigh());
        
        mDataOut.putAt(0, mFltBuf);
        mDataOut.putAt(1, mIntBuf);
        mDataOut.putAt(2, mCudaX);
        mDataOut.putAt(3, mCudaType);
        mDataOut.putAt(4, mCudaIlist);
        mDataOut.putAt(5, mCudaNumneigh);
        mDataOut.putAt(6, mCudaDisplsneigh);
        mDataOut.putAt(7, mCudaFirstneigh);
        
        int tCode = mLammps2Cuda.invoke(mDataIn, mDataOut);
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda -> lammps
    }
}
