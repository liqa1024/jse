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
         * 自定义 nnap cuda 版本使用的缓存近邻列表大小，超出此大小将会截断，
         * 且默认会抛出错误提示增加更高的大小。更大的值会占用更高的显存并可能会降低速度；
         * 默认为 {@code 256}
         */
        public static int CUDA_NLSIZE = OS.envI("JSE_NNAP_CUDA_NLSIZE", 256);
        /**
         * 自定义 nnap cuda 中使用的 block_size 值，这可能会影响速度；
         * 默认为 {@code 256}
         */
        public static int CUDA_BLOCKSIZE = OS.envI("JSE_NNAP_CUDA_BLOCKSIZE", 256);
        
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
    private final GrowableFloatCudaPointer mCudaX, mCudaF0, mCudaF1, mCudaEatom, mCudaVatom0, mCudaVatom1;
    private final GrowableIntCudaPointer mCudaType, mCudaIlist, mCudaNumneigh;
    private final GrowableInt64CudaPointer mCudaDisplsneigh;
    private final GrowableIntCudaPointer mCudaFirstneigh;
    
    NNAP_cuda(@Nullable String aLibDir, @Nullable String aProjectName, Map<?, ?> aModelInfo) throws Exception {
        super(aLibDir, aProjectName, aModelInfo, 1, "single");
        mDisplsneigh = new GrowableInt64CPointer(128);
        mFltBuf = new GrowableFloatCPointer(128);
        mIntBuf = new GrowableIntCPointer(1024);
        mCudaX = new GrowableFloatCudaPointer(128);
        mCudaF0 = new GrowableFloatCudaPointer(128);
        mCudaF1 = new GrowableFloatCudaPointer(128);
        mCudaEatom = new GrowableFloatCudaPointer(128);
        mCudaVatom0 = new GrowableFloatCudaPointer(128);
        mCudaVatom1 = new GrowableFloatCudaPointer(128);
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
    IJITMethod mLammps2Cuda = null, mCuda2Lammps = null, mComputeLammpsCuda = null;
    private void compileCuda_() throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mJITEngine!=null) throw new IllegalStateException("compileCuda() has already been called");
        // 开始 jit
        mJITEngine = mNNAPGEN.initEngineCuda(Conf.CUDA_NLSIZE, Conf.CUDA_BLOCKSIZE, Conf.OPTIM_LEVEL, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_CUDA_COMPILER, Conf.CMAKE_CUDA_FLAGS, Conf.CMAKE_SETTING);
        mJITEngine.setMethodNames("jse_nnap_statDisplsneighLammps", "jse_nnap_lammps2cuda", "jse_nnap_cuda2lammps", "jse_nnap_computeLammpsCuda").compile();
        mStatDisplsneighLammps = mJITEngine.findMethod("jse_nnap_statDisplsneighLammps");
        mLammps2Cuda = mJITEngine.findMethod("jse_nnap_lammps2cuda");
        mCuda2Lammps = mJITEngine.findMethod("jse_nnap_cuda2lammps");
        mComputeLammpsCuda = mJITEngine.findMethod("jse_nnap_computeLammpsCuda");
    }
    
    @Override void shutdown_() {
        super.shutdown_();
        mDisplsneigh.free();
        mFltBuf.free();
        mIntBuf.free();
        try {
            mCudaX.free();
            mCudaF0.free();
            mCudaF1.free();
            mCudaEatom.free();
            mCudaVatom0.free();
            mCudaVatom1.free();
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
        boolean cvflagAtom = aPair.cvflagAtom();
        int inum = aPair.listInum();
        final int nlocal = aPair.atomNlocal();
        final int nghost = aPair.atomNghost();
        final int ntot = nlocal + nghost;
        mFltBuf.ensureCapacity((long)ntot*9);
        mDisplsneigh.ensureCapacity(nlocal+1);
        mCudaX.ensureCapacity((long)ntot*3);
        mCudaF0.ensureCapacity((long)nlocal*3);
        mCudaF1.ensureCapacity((long)ntot*3);
        mCudaEatom.ensureCapacity((long)nlocal);
        mCudaVatom0.ensureCapacity((long)nlocal*6);
        mCudaVatom1.ensureCapacity((long)ntot*(cvflagAtom?9:6));
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
        
        // cuda compute
        mInNums.putAt(0, inum);
        mInNums.putAt(1, nlocal);
        mInNums.putAt(2, nghost);
        mInNums.putAt(3, aPair.eflagEither()?1:0);
        mInNums.putAt(4, aPair.vflagEither()?1:0);
        mInNums.putAt(5, aPair.eflagAtom()?1:0);
        mInNums.putAt(6, aPair.vflagAtom()?1:0);
        mInNums.putAt(7, cvflagAtom?1:0);
        
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, mCudaX);
        mDataIn.putAt(2, mCudaType);
        mDataIn.putAt(3, mCudaIlist);
        mDataIn.putAt(4, mCudaNumneigh);
        mDataIn.putAt(5, mCudaDisplsneigh);
        mDataIn.putAt(6, mCudaFirstneigh);
        
        mDataOut.putAt(0, mCudaF0);
        mDataOut.putAt(1, mCudaF1);
        mDataOut.putAt(2, mCudaEatom);
        mDataOut.putAt(3, mCudaVatom0);
        mDataOut.putAt(3, mCudaVatom1);
        
        tCode = mComputeLammpsCuda.invoke(mDataIn, mDataOut);
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda -> lammps
        mInNums.putAt(0, nlocal);
        mInNums.putAt(1, nghost);
        mInNums.putAt(2, aPair.eflagEither()?1:0);
        mInNums.putAt(3, aPair.vflagEither()?1:0);
        mInNums.putAt(4, aPair.eflagAtom()?1:0);
        mInNums.putAt(5, aPair.vflagAtom()?1:0);
        mInNums.putAt(6, cvflagAtom?1:0);
        
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, mFltBuf);
        mDataIn.putAt(2, mCudaF0);
        mDataIn.putAt(3, mCudaF1);
        mDataIn.putAt(4, mCudaEatom);
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
}
