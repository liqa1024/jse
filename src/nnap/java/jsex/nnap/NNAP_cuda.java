package jsex.nnap;

import jse.code.IO;
import jse.code.OS;
import jse.cptr.*;
import jse.gpu.*;
import jse.jit.IJITMethod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jse.cptr.CPointer.NULL;

/**
 * jse 实现的 nnap gpu 版本
 * @author liqa
 */
@ApiStatus.Experimental
class NNAP_cuda extends NNAP2 {
    public final static class Conf {
        public static boolean DEV = false;
        /**
         * 自定义 nnap cuda 中使用的 block_size 值，这可能会影响速度；
         * 默认为 {@code 192}
         */
        public static int CUDA_BLOCKSIZE = OS.envI("JSE_NNAP_CUDA_BLOCKSIZE", 192);
        
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
    
    private int mNeighnumMax = -1;
    // cpu 数据
    private final GrowableFloatCPointer mFltBuf;
    private final GrowableIntCPointer mIntBuf;
    // cuda 数据
    private final GrowableFloatCudaPointer mCudaX, mCudaF0, mCudaF1, mCudaEatom0, mCudaVatom0, mCudaVatom1;
    private final GrowableIntCudaPointer mCudaType, mCudaIlist, mCudaNumneigh, mCudaBufNeiNum, mCudaBufCType;
    private final GrowableIntCudaPointer mCudaFirstneigh, mCudaBufNlType, mCudaBufNlIdx;
    private final GrowableFloatCudaPointer mCudaBufNlDx, mCudaBufNlDy, mCudaBufNlDz, mCudaBufGradNlDx, mCudaBufGradNlDy, mCudaBufGradNlDz;
    private FloatCudaPointer mCudaCutsq = null;
    private IntCudaPointer mCudaLmpType2NNAPType = null;
    private final AnyCPointer mCudaFpHyperParam, mCudaFpParam, mCudaNnParam, mCudaNormParam;
    private final CudaPointer mCudaCudaFpHyperParam, mCudaCudaFpParam, mCudaCudaNnParam, mCudaCudaNormParam;
    
    NNAP_cuda(@Nullable String aLibDir, @Nullable String aProjectName, Map<?, ?> aModelInfo) throws Exception {
        super(aLibDir, aProjectName, aModelInfo, 1, "single");
        if (!Conf.DEV) throw new UnsupportedOperationException();
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
        mCudaBufNeiNum = new GrowableIntCudaPointer(128);
        mCudaBufCType = new GrowableIntCudaPointer(128);
        mCudaFirstneigh = new GrowableIntCudaPointer(1024);
        mCudaBufNlType = new GrowableIntCudaPointer(1024);
        mCudaBufNlIdx = new GrowableIntCudaPointer(1024);
        mCudaBufNlDx = new GrowableFloatCudaPointer(1024);
        mCudaBufNlDy = new GrowableFloatCudaPointer(1024);
        mCudaBufNlDz = new GrowableFloatCudaPointer(1024);
        mCudaBufGradNlDx = new GrowableFloatCudaPointer(1024);
        mCudaBufGradNlDy = new GrowableFloatCudaPointer(1024);
        mCudaBufGradNlDz = new GrowableFloatCudaPointer(1024);
        
        int tTypeNum = mSymbols.length;
        mCudaFpHyperParam = AnyCPointer.malloc(tTypeNum);
        mCudaFpParam = AnyCPointer.malloc(tTypeNum);
        mCudaNnParam = AnyCPointer.malloc(tTypeNum);
        mCudaNormParam = AnyCPointer.malloc(tTypeNum);
        mCudaCudaFpHyperParam = CudaPointer.malloc(tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaCudaFpParam = CudaPointer.malloc(tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaCudaNnParam = CudaPointer.malloc(tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaCudaNormParam = CudaPointer.malloc(tTypeNum*AnyCPointer.TYPE_SIZE);
        for (int i = 0; i < tTypeNum; ++i) {
            int tSize = mBasis[i].cptrHyperParameterSize();
            FloatCPointer tSubParam = mFpHyperParam.getAsFloatCPointerAt(i);
            FloatCudaPointer tSubCudaParam = FloatCudaPointer.malloc(Math.max(1, tSize));
            tSubCudaParam.fill(tSubParam, tSize);
            mCudaFpHyperParam.putAt(i, tSubCudaParam);
            
            tSize = mBasis[i].cptrParameterSize();
            tSubParam = mFpParam.getAsFloatCPointerAt(i);
            tSubCudaParam = FloatCudaPointer.malloc(Math.max(1, tSize));
            tSubCudaParam.fill(tSubParam, tSize);
            mCudaFpParam.putAt(i, tSubCudaParam);
            
            tSize = mNN[i].cptrParameterSize();
            tSubParam = mNnParam.getAsFloatCPointerAt(i);
            tSubCudaParam = FloatCudaPointer.malloc(Math.max(1, tSize));
            tSubCudaParam.fill(tSubParam, tSize);
            mCudaNnParam.putAt(i, tSubCudaParam);
            
            tSize = mBasis[i].size()*2 + 2;
            tSubParam = mNormParam.getAsFloatCPointerAt(i);
            tSubCudaParam = FloatCudaPointer.malloc(Math.max(1, tSize));
            tSubCudaParam.fill(tSubParam, tSize);
            mCudaNormParam.putAt(i, tSubCudaParam);
        }
        mCudaCudaFpHyperParam.memcpy2this(mCudaFpHyperParam, tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaCudaFpParam.memcpy2this(mCudaFpParam, tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaCudaNnParam.memcpy2this(mCudaNnParam, tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaCudaNormParam.memcpy2this(mCudaNormParam, tTypeNum*AnyCPointer.TYPE_SIZE);
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
    IJITMethod mStatNeiNumLammps = null;
    IJITMethod mLammps2Cuda = null, mCuda2Lammps = null, mComputeLammpsCuda = null;
    IJITMethod mComputeGPUMD = null;
    private void compileCuda_() throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mJITEngine!=null) throw new IllegalStateException("compileCuda() has already been called");
        // 开始 jit
        mJITEngine = mNNAPGEN.initEngineCuda();
        mJITEngine.setMethodNames("jse_nnap_statNeiNumLammps", "jse_nnap_lammps2cuda", "jse_nnap_cuda2lammps", "jse_nnap_computeLammpsCuda", "jse_nnap_computeGPUMD").compile();
        mStatNeiNumLammps = mJITEngine.findMethod("jse_nnap_statNeiNumLammps");
        mLammps2Cuda = mJITEngine.findMethod("jse_nnap_lammps2cuda");
        mCuda2Lammps = mJITEngine.findMethod("jse_nnap_cuda2lammps");
        mComputeLammpsCuda = mJITEngine.findMethod("jse_nnap_computeLammpsCuda");
        mComputeGPUMD = mJITEngine.findMethod("jse_nnap_computeGPUMD");
    }
    
    @Override void close_() throws Exception {
        super.close_();
        mFltBuf.free();
        mIntBuf.free();
        
        mCudaX.free();
        mCudaF0.free();
        mCudaF1.free();
        mCudaEatom0.free();
        mCudaVatom0.free();
        mCudaVatom1.free();
        mCudaType.free();
        mCudaIlist.free();
        mCudaNumneigh.free();
        mCudaBufNeiNum.free();
        mCudaBufCType.free();
        mCudaFirstneigh.free();
        mCudaBufNlType.free();
        mCudaBufNlIdx.free();
        mCudaBufNlDx.free();
        mCudaBufNlDy.free();
        mCudaBufNlDz.free();
        mCudaBufGradNlDx.free();
        mCudaBufGradNlDy.free();
        mCudaBufGradNlDz.free();
        if (mCudaCutsq != null) {
            mCudaCutsq.free();
            mCudaCutsq = null;
        }
        if (mCudaLmpType2NNAPType != null) {
            mCudaLmpType2NNAPType.free();
            mCudaLmpType2NNAPType = null;
        }
        for (int i = 0; i < mSymbols.length; ++i) {
            mCudaFpHyperParam.getAsCudaPointerAt(i).free();
            mCudaFpParam.getAsCudaPointerAt(i).free();
            mCudaNnParam.getAsCudaPointerAt(i).free();
            mCudaNormParam.getAsCudaPointerAt(i).free();
        }
        mCudaFpHyperParam.free();
        mCudaFpParam.free();
        mCudaNnParam.free();
        mCudaNormParam.free();
        mCudaCudaFpHyperParam.free();
        mCudaCudaFpParam.free();
        mCudaCudaNnParam.free();
        mCudaCudaNormParam.free();
    }
    
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) throws Exception {
        throw new UnsupportedOperationException();
    }
    @Override public void calEnergyForceVirial(int aAtomNumber, INeighborListGetter aNeighborListGetter, @Nullable IEnergyAccumulator rEnergyAccumulator, @Nullable IForceAccumulator rForceAccumulator, @Nullable IVirialAccumulator rVirialAccumulator) throws Exception {
        throw new UnsupportedOperationException();
    }
    
    private boolean mCudaParaInited = false;
    private void initLmpParamCuda_(PairNNAP2 aPair) throws CudaException {
        if (mCudaParaInited) return;
        mCudaParaInited = true;
        mCudaLmpType2NNAPType = IntCudaPointer.malloc(aPair.mTypeNum+1);
        mCudaLmpType2NNAPType.fill(aPair.mLmpType2NNAPType, aPair.mTypeNum+1);
        mCudaCutsq = FloatCudaPointer.malloc(aPair.mTypeNum+1);
        mCudaCutsq.fillD(aPair.mCutsq, aPair.mTypeNum+1);
    }
    @Override void computeLammps(PairNNAP2 aPair) throws CudaException {
        initLmpParamCuda_(aPair);
        // 常规缓存向量长度规范
        final boolean nlflag = mNeighnumMax<0 || aPair.neighborAgo()==0;
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
        mCudaBufNeiNum.ensureCapacity(inum);
        mCudaBufCType.ensureCapacity(inum);
        // 近邻列表大小获取和缓存合理化
        IPointer ilist = NULL;
        IPointer numneigh = NULL;
        IPointer firstneigh = NULL;
        if (nlflag) {
            ilist = aPair.listIlist();
            numneigh = aPair.listNumneigh();
            firstneigh = aPair.listFirstneigh();
            mInNums[0].putAt(0, inum);
            mDataIn[0].putAt(0, mInNums[0]);
            mDataIn[0].putAt(1, ilist);
            mDataIn[0].putAt(2, numneigh);
            mStatNeiNumLammps.invoke(mDataIn[0], mOutNums[0]);
            mNeighnumMax = mOutNums[0].getAt(0);
        }
        // 近邻列表缓存向量长度规范
        if (nlflag) {
            int tTotNeiNum = inum*mNeighnumMax;
            mIntBuf.ensureCapacity(tTotNeiNum);
            mCudaFirstneigh.ensureCapacity(tTotNeiNum);
            mCudaBufNlType.ensureCapacity(tTotNeiNum);
            mCudaBufNlIdx.ensureCapacity(tTotNeiNum);
            mCudaBufNlDx.ensureCapacity(tTotNeiNum);
            mCudaBufNlDy.ensureCapacity(tTotNeiNum);
            mCudaBufNlDz.ensureCapacity(tTotNeiNum);
            mCudaBufGradNlDx.ensureCapacity(tTotNeiNum);
            mCudaBufGradNlDy.ensureCapacity(tTotNeiNum);
            mCudaBufGradNlDz.ensureCapacity(tTotNeiNum);
        }
        
        // lammps -> cuda
        mInNums[0].putAt(0, inum);
        mInNums[0].putAt(1, nlocalghost);
        mInNums[0].putAt(2, nlflag?1:0);
        mInNums[0].putAt(3, mNeighnumMax);
        
        mDataIn[0].putAt(0, mInNums[0]);
        mDataIn[0].putAt(1, aPair.atomX());
        mDataIn[0].putAt(2, aPair.atomType());
        if (nlflag) {
            mDataIn[0].putAt(3, ilist);
            mDataIn[0].putAt(4, numneigh);
            mDataIn[0].putAt(5, firstneigh);
        }
        
        mDataOut[0].putAt(0, mFltBuf);
        mDataOut[0].putAt(1, mIntBuf);
        mDataOut[0].putAt(2, mCudaX);
        mDataOut[0].putAt(3, mCudaType);
        if (nlflag) {
            mDataOut[0].putAt(4, mCudaIlist);
            mDataOut[0].putAt(5, mCudaNumneigh);
            mDataOut[0].putAt(6, mCudaFirstneigh);
        }
        
        int tCode = mLammps2Cuda.invoke(mDataIn[0], mDataOut[0]);
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda compute
        mInNums[0].putAt(0, inum);
        mInNums[0].putAt(1, nlocalghost);
        mInNums[0].putAt(2, mNeighnumMax);
        mInNums[0].putAt(3, aPair.eflagEither()?1:0);
        mInNums[0].putAt(4, aPair.vflagEither()?1:0);
        mInNums[0].putAt(5, aPair.eflagAtom()?1:0);
        mInNums[0].putAt(6, aPair.vflagAtom()?1:0);
        mInNums[0].putAt(7, cvflagAtom?1:0);
        
        mDataIn[0].putAt(0, mInNums[0]);
        mDataIn[0].putAt(1, mCudaX);
        mDataIn[0].putAt(2, mCudaType);
        mDataIn[0].putAt(3, mCudaIlist);
        mDataIn[0].putAt(4, mCudaNumneigh);
        mDataIn[0].putAt(5, mCudaFirstneigh);
        mDataIn[0].putAt(6, mCudaCutsq);
        mDataIn[0].putAt(7, mCudaLmpType2NNAPType);
        mDataIn[0].putAt(8, mCudaCudaFpHyperParam);
        mDataIn[0].putAt(9, mCudaCudaFpParam);
        mDataIn[0].putAt(10, mCudaCudaNnParam);
        mDataIn[0].putAt(11, mCudaCudaNormParam);
        
        mDataOut[0].putAt(0, mCudaF0);
        mDataOut[0].putAt(1, mCudaF1);
        mDataOut[0].putAt(2, mCudaEatom0);
        mDataOut[0].putAt(3, mCudaVatom0);
        mDataOut[0].putAt(4, mCudaVatom1);
        mDataOut[0].putAt(5, mCudaBufNlDx);
        mDataOut[0].putAt(6, mCudaBufNlDy);
        mDataOut[0].putAt(7, mCudaBufNlDz);
        mDataOut[0].putAt(8, mCudaBufNlType);
        mDataOut[0].putAt(9, mCudaBufNlIdx);
        mDataOut[0].putAt(10, mCudaBufNeiNum);
        mDataOut[0].putAt(11, mCudaBufCType);
        mDataOut[0].putAt(12, mCudaBufGradNlDx);
        mDataOut[0].putAt(13, mCudaBufGradNlDy);
        mDataOut[0].putAt(14, mCudaBufGradNlDz);
        
        tCode = mComputeLammpsCuda.invoke(mDataIn[0], mDataOut[0]);
        CudaCore.cudaExceptionCheck(tCode);
        
        // cuda -> lammps
        mInNums[0].putAt(0, inum);
        mInNums[0].putAt(1, nlocalghost);
        mInNums[0].putAt(2, aPair.eflagEither()?1:0);
        mInNums[0].putAt(3, aPair.vflagEither()?1:0);
        mInNums[0].putAt(4, aPair.eflagAtom()?1:0);
        mInNums[0].putAt(5, aPair.vflagAtom()?1:0);
        mInNums[0].putAt(6, cvflagAtom?1:0);
        
        mDataIn[0].putAt(0, mInNums[0]);
        mDataIn[0].putAt(1, mFltBuf);
        mDataIn[0].putAt(2, ilist);
        mDataIn[0].putAt(3, mCudaF1);
        mDataIn[0].putAt(4, mCudaEatom0);
        mDataIn[0].putAt(5, mCudaVatom0);
        mDataIn[0].putAt(6, mCudaVatom1);
        
        mDataOut[0].putAt(0, aPair.atomF());
        mDataOut[0].putAt(1, aPair.engVdwl());
        mDataOut[0].putAt(2, aPair.eatom());
        mDataOut[0].putAt(3, aPair.virial());
        mDataOut[0].putAt(4, aPair.vatom());
        mDataOut[0].putAt(5, aPair.cvatom());
        
        tCode = mCuda2Lammps.invoke(mDataIn[0], mDataOut[0]);
        CudaCore.cudaExceptionCheck(tCode);
    }
    
    void computeGPUMD(int number_of_particles, int N1, int N2, int neighnumMax,
                      long g_neighbor_number, long g_neighbor_list,
                      long nl_dx, long nl_dy, long nl_dz,
                      long g_type,
                      long g_fx, long g_fy, long g_fz,
                      long g_virial, long g_potential) throws CudaException {
        // 近邻列表缓存向量长度规范
        int tTotNeiNum = number_of_particles*neighnumMax;
        mCudaBufNlType.ensureCapacity(tTotNeiNum);
        mCudaBufNlIdx.ensureCapacity(tTotNeiNum);
        mCudaBufNlDx.ensureCapacity(tTotNeiNum);
        mCudaBufNlDy.ensureCapacity(tTotNeiNum);
        mCudaBufNlDz.ensureCapacity(tTotNeiNum);
        mCudaBufGradNlDx.ensureCapacity(tTotNeiNum);
        mCudaBufGradNlDy.ensureCapacity(tTotNeiNum);
        mCudaBufGradNlDz.ensureCapacity(tTotNeiNum);
        
        // 数据打包
        mInNums[0].putAt(0, number_of_particles);
        mInNums[0].putAt(1, N1);
        mInNums[0].putAt(2, N2);
        mInNums[0].putAt(3, neighnumMax);
        
        mDataIn[0].putAt(0, mInNums[0]);
        mDataIn[0].putAt(1, new CudaPointer(g_neighbor_number));
        mDataIn[0].putAt(2, new CudaPointer(g_neighbor_list));
        mDataIn[0].putAt(3, new CudaPointer(nl_dx));
        mDataIn[0].putAt(4, new CudaPointer(nl_dy));
        mDataIn[0].putAt(5, new CudaPointer(nl_dz));
        mDataIn[0].putAt(6, new CudaPointer(g_type));
        mDataIn[0].putAt(7, mCudaCudaFpHyperParam);
        mDataIn[0].putAt(8, mCudaCudaFpParam);
        mDataIn[0].putAt(9, mCudaCudaNnParam);
        mDataIn[0].putAt(10, mCudaCudaNormParam);
        
        mDataOut[0].putAt(0, new CudaPointer(g_fx));
        mDataOut[0].putAt(1, new CudaPointer(g_fy));
        mDataOut[0].putAt(2, new CudaPointer(g_fz));
        mDataOut[0].putAt(3, new CudaPointer(g_virial));
        mDataOut[0].putAt(4, new CudaPointer(g_potential));
        mDataOut[0].putAt(5, mCudaBufNlDx);
        mDataOut[0].putAt(6, mCudaBufNlDy);
        mDataOut[0].putAt(7, mCudaBufNlDz);
        mDataOut[0].putAt(8, mCudaBufNlType);
        mDataOut[0].putAt(9, mCudaBufNlIdx);
        mDataOut[0].putAt(10, mCudaBufGradNlDx);
        mDataOut[0].putAt(11, mCudaBufGradNlDy);
        mDataOut[0].putAt(12, mCudaBufGradNlDz);
        
        int tCode = mComputeGPUMD.invoke(mDataIn[0], mDataOut[0]);
        CudaCore.cudaExceptionCheck(tCode);
    }
}
