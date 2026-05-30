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
@ApiStatus.Experimental @ApiStatus.Obsolete
class NNAP_cuda extends NNAP {
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
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_NNAP_CUDA", NNAP.Conf.CMAKE_SETTING);
        /**
         * 自定义构建 nnap 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER  = OS.env("JSE_CMAKE_CXX_COMPILER_NNAP_CUDA", NNAP.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS     = OS.env("JSE_CMAKE_CXX_FLAGS_NNAP_CUDA"   , NNAP.Conf.CMAKE_CXX_FLAGS);
        public static @Nullable String CMAKE_CUDA_COMPILER = OS.env("JSE_CMAKE_CUDA_COMPILER_NNAP");
        public static @Nullable String CMAKE_CUDA_FLAGS    = OS.env("JSE_CMAKE_CUDA_FLAGS_NNAP");
        /**
         * 自定义构建 nnap 时的优化等级，
         * 默认会使用 BASE 优化
         */
        public static int OPTIM_LEVEL = OS.envI("JSE_NNAP_OPTIM_LEVEL_CUDA", NNAP.Conf.OPTIM_LEVEL);
    }
    
    private int mNeighnumMax = -1;
    // cpu 数据
    private final FloatCPointer mFltBuf;
    private final IntCPointer mIntBuf;
    // cuda 数据
    private final FloatCudaPointer mCudaX, mCudaF0, mCudaF1, mCudaEatom0, mCudaVatom0, mCudaVatom1;
    private final IntCudaPointer mCudaType, mCudaIlist, mCudaNumneigh, mCudaBufNeiNum, mCudaBufCType;
    private final IntCudaPointer mCudaFirstneigh, mCudaBufNlType, mCudaBufNlIdx;
    private final FloatCudaPointer mCudaBufNlDx, mCudaBufNlDy, mCudaBufNlDz, mCudaBufGradNlDx, mCudaBufGradNlDy, mCudaBufGradNlDz;
    private FloatCudaPointer mCudaCutsq = null;
    private IntCudaPointer mCudaLmpType2NNAPType = null;
    private final CudaPointer mCudaFpHyperParam, mCudaFpParam, mCudaNnParam, mCudaNormParam;
    
    NNAP_cuda(@Nullable String aLibDir, @Nullable String aProjectName, Map<?, ?> aModelInfo) throws Exception {
        super(aLibDir, aProjectName, aModelInfo, 1, "single");
        if (!Conf.DEV) throw new UnsupportedOperationException();
        mFltBuf = mPtrMng.newFloatCPointer(128);
        mIntBuf = mPtrMng.newIntCPointer(1024);
        mCudaX = mPtrMng.newFloatCudaPointer(128);
        mCudaF0 = mPtrMng.newFloatCudaPointer(128);
        mCudaF1 = mPtrMng.newFloatCudaPointer(128);
        mCudaEatom0 = mPtrMng.newFloatCudaPointer(128);
        mCudaVatom0 = mPtrMng.newFloatCudaPointer(128);
        mCudaVatom1 = mPtrMng.newFloatCudaPointer(128);
        mCudaType = mPtrMng.newIntCudaPointer(128);
        mCudaIlist = mPtrMng.newIntCudaPointer(128);
        mCudaNumneigh = mPtrMng.newIntCudaPointer(128);
        mCudaBufNeiNum = mPtrMng.newIntCudaPointer(128);
        mCudaBufCType = mPtrMng.newIntCudaPointer(128);
        mCudaFirstneigh = mPtrMng.newIntCudaPointer(1024);
        mCudaBufNlType = mPtrMng.newIntCudaPointer(1024);
        mCudaBufNlIdx = mPtrMng.newIntCudaPointer(1024);
        mCudaBufNlDx = mPtrMng.newFloatCudaPointer(1024);
        mCudaBufNlDy = mPtrMng.newFloatCudaPointer(1024);
        mCudaBufNlDz = mPtrMng.newFloatCudaPointer(1024);
        mCudaBufGradNlDx = mPtrMng.newFloatCudaPointer(1024);
        mCudaBufGradNlDy = mPtrMng.newFloatCudaPointer(1024);
        mCudaBufGradNlDz = mPtrMng.newFloatCudaPointer(1024);
        
        int tTypeNum = mSymbols.length;
        AnyCPointer tCudaFpHyperParam = AnyCPointer.calloc(tTypeNum);
        AnyCPointer tCudaFpParam = AnyCPointer.calloc(tTypeNum);
        AnyCPointer tCudaNnParam = AnyCPointer.calloc(tTypeNum);
        AnyCPointer tCudaNormParam = AnyCPointer.calloc(tTypeNum);
        mCudaFpHyperParam = mPtrMng.newCudaPointer(tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaFpParam = mPtrMng.newCudaPointer(tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaNnParam = mPtrMng.newCudaPointer(tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaNormParam = mPtrMng.newCudaPointer(tTypeNum*AnyCPointer.TYPE_SIZE);
        for (int i = 0; i < tTypeNum; ++i) {
            int tSize = mBasis[i].cptrHyperParameterSize();
            FloatCPointer tSubParam = mFpHyperParam.getAsFloatCPointerAt(i);
            FloatCudaPointer tSubCudaParam = mPtrMng.newFloatCudaPointer(tSize);
            tSubCudaParam.fill(tSubParam, tSize);
            tCudaFpHyperParam.putAt(i, tSubCudaParam);
            
            tSize = mBasis[i].cptrParameterSize();
            tSubParam = mFpParam.getAsFloatCPointerAt(i);
            tSubCudaParam = mPtrMng.newFloatCudaPointer(tSize);
            tSubCudaParam.fill(tSubParam, tSize);
            tCudaFpParam.putAt(i, tSubCudaParam);
            
            tSize = mNN[i].cptrParameterSize();
            tSubParam = mNnParam.getAsFloatCPointerAt(i);
            tSubCudaParam = mPtrMng.newFloatCudaPointer(tSize);
            tSubCudaParam.fill(tSubParam, tSize);
            tCudaNnParam.putAt(i, tSubCudaParam);
            
            tSize = mBasis[i].size()*2 + 2;
            tSubParam = mNormParam.getAsFloatCPointerAt(i);
            tSubCudaParam = mPtrMng.newFloatCudaPointer(tSize);
            tSubCudaParam.fill(tSubParam, tSize);
            tCudaNormParam.putAt(i, tSubCudaParam);
        }
        mCudaFpHyperParam.memcpy2this(tCudaFpHyperParam, tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaFpParam.memcpy2this(tCudaFpParam, tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaNnParam.memcpy2this(tCudaNnParam, tTypeNum*AnyCPointer.TYPE_SIZE);
        mCudaNormParam.memcpy2this(tCudaNormParam, tTypeNum*AnyCPointer.TYPE_SIZE);
        tCudaFpHyperParam.free();
        tCudaFpParam.free();
        tCudaNnParam.free();
        tCudaNormParam.free();
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
//        mJITEngine.setMethodNames("jse_nnap_statNeiNumLammps", "jse_nnap_lammps2cuda", "jse_nnap_cuda2lammps", "jse_nnap_computeLammpsCuda", "jse_nnap_computeGPUMD").compile();
        mStatNeiNumLammps = mJITEngine.findMethod("jse_nnap_statNeiNumLammps");
        mLammps2Cuda = mJITEngine.findMethod("jse_nnap_lammps2cuda");
        mCuda2Lammps = mJITEngine.findMethod("jse_nnap_cuda2lammps");
        mComputeLammpsCuda = mJITEngine.findMethod("jse_nnap_computeLammpsCuda");
        mComputeGPUMD = mJITEngine.findMethod("jse_nnap_computeGPUMD");
    }
    
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) throws Exception {
        throw new UnsupportedOperationException();
    }
    @Override public void calEnergyForceVirial(int aAtomNumber, INeighborListGetter aNeighborListGetter, @Nullable IEnergyAccumulator rEnergyAccumulator, @Nullable IForceAccumulator rForceAccumulator, @Nullable IVirialAccumulator rVirialAccumulator) throws Exception {
        throw new UnsupportedOperationException();
    }
    
    private boolean mCudaParaInited = false;
    private void initLmpParamCuda_(PairNNAP aPair) throws CudaException {
        if (mCudaParaInited) return;
        mCudaParaInited = true;
        mCudaLmpType2NNAPType = mPtrMng.newIntCudaPointer(aPair.mNumTypes+1);
        mCudaLmpType2NNAPType.fill(aPair.mLmpType2NNAPType, aPair.mNumTypes+1);
        mCudaCutsq = mPtrMng.newFloatCudaPointer(aPair.mNumTypes+1);
        mCudaCutsq.fillD(aPair.mCutsq, aPair.mNumTypes+1);
    }
    @Override void computeLammps(PairNNAP aPair) throws CudaException {
        initLmpParamCuda_(aPair);
        // 常规缓存向量长度规范
        final boolean nlflag = mNeighnumMax<0 || aPair.neighborAgo()==0;
        final boolean cvflagAtom = aPair.cvflagAtom();
        final int inum = aPair.listInum();
        final int nlocal = aPair.atomNlocal();
        final int nghost = aPair.atomNghost();
        final int nlocalghost = nlocal + nghost;
        mPtrMng.ensureCapacity(mFltBuf, (long)nlocalghost*9);
        mPtrMng.ensureCapacity(mCudaX, (long)nlocalghost*3);
        mPtrMng.ensureCapacity(mCudaF0, (long)inum*3);
        mPtrMng.ensureCapacity(mCudaF1, (long)nlocalghost*3);
        mPtrMng.ensureCapacity(mCudaEatom0, (long)inum);
        mPtrMng.ensureCapacity(mCudaVatom0, (long)inum*6);
        mPtrMng.ensureCapacity(mCudaVatom1, (long)nlocalghost*(cvflagAtom?9:6));
        mPtrMng.ensureCapacity(mCudaType, nlocalghost);
        if (nlflag) {
            mPtrMng.ensureCapacity(mCudaIlist, inum);
            mPtrMng.ensureCapacity(mCudaNumneigh, inum);
        }
        mPtrMng.ensureCapacity(mCudaBufNeiNum, inum);
        mPtrMng.ensureCapacity(mCudaBufCType, inum);
        // 近邻列表大小获取和缓存合理化
        IPointer ilist = NULL;
        IPointer numneigh = NULL;
        IPointer firstneigh = NULL;
//        if (nlflag) {
//            ilist = aPair.listIlist();
//            numneigh = aPair.listNumneigh();
//            firstneigh = aPair.listFirstneigh();
//            mInNums[0].putAt(0, inum);
//            mDataIn[0].putAt(0, mInNums[0]);
//            mDataIn[0].putAt(1, ilist);
//            mDataIn[0].putAt(2, numneigh);
//            mStatNeiNumLammps.invoke(mDataIn[0], mOutNums[0]);
//            mNeighnumMax = mOutNums[0].getAt(0);
//        }
//        // 近邻列表缓存向量长度规范
//        if (nlflag) {
//            int tTotNeiNum = inum*mNeighnumMax;
//            mPtrMng.ensureCapacity(mIntBuf, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaFirstneigh, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufNlType, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufNlIdx, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufNlDx, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufNlDy, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufNlDz, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufGradNlDx, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufGradNlDy, tTotNeiNum);
//            mPtrMng.ensureCapacity(mCudaBufGradNlDz, tTotNeiNum);
//        }
//
//        // lammps -> cuda
//        mInNums[0].putAt(0, inum);
//        mInNums[0].putAt(1, nlocalghost);
//        mInNums[0].putAt(2, nlflag?1:0);
//        mInNums[0].putAt(3, mNeighnumMax);
//
//        mDataIn[0].putAt(0, mInNums[0]);
//        mDataIn[0].putAt(1, aPair.atomX());
//        mDataIn[0].putAt(2, aPair.atomType());
//        if (nlflag) {
//            mDataIn[0].putAt(3, ilist);
//            mDataIn[0].putAt(4, numneigh);
//            mDataIn[0].putAt(5, firstneigh);
//        }
//
//        mDataOut[0].putAt(0, mFltBuf);
//        mDataOut[0].putAt(1, mIntBuf);
//        mDataOut[0].putAt(2, mCudaX);
//        mDataOut[0].putAt(3, mCudaType);
//        if (nlflag) {
//            mDataOut[0].putAt(4, mCudaIlist);
//            mDataOut[0].putAt(5, mCudaNumneigh);
//            mDataOut[0].putAt(6, mCudaFirstneigh);
//        }
//
//        int tCode = mLammps2Cuda.invoke(mDataIn[0], mDataOut[0]);
//        CudaCore.cudaExceptionCheck(tCode);
//
//        // cuda compute
//        mInNums[0].putAt(0, inum);
//        mInNums[0].putAt(1, nlocalghost);
//        mInNums[0].putAt(2, mNeighnumMax);
//        mInNums[0].putAt(3, aPair.eflagEither()?1:0);
//        mInNums[0].putAt(4, aPair.vflagEither()?1:0);
//        mInNums[0].putAt(5, aPair.eflagAtom()?1:0);
//        mInNums[0].putAt(6, aPair.vflagAtom()?1:0);
//        mInNums[0].putAt(7, cvflagAtom?1:0);
//
//        mDataIn[0].putAt(0, mInNums[0]);
//        mDataIn[0].putAt(1, mCudaX);
//        mDataIn[0].putAt(2, mCudaType);
//        mDataIn[0].putAt(3, mCudaIlist);
//        mDataIn[0].putAt(4, mCudaNumneigh);
//        mDataIn[0].putAt(5, mCudaFirstneigh);
//        mDataIn[0].putAt(6, mCudaCutsq);
//        mDataIn[0].putAt(7, mCudaLmpType2NNAPType);
//        mDataIn[0].putAt(8, mCudaFpHyperParam);
//        mDataIn[0].putAt(9, mCudaFpParam);
//        mDataIn[0].putAt(10, mCudaNnParam);
//        mDataIn[0].putAt(11, mCudaNormParam);
//
//        mDataOut[0].putAt(0, mCudaF0);
//        mDataOut[0].putAt(1, mCudaF1);
//        mDataOut[0].putAt(2, mCudaEatom0);
//        mDataOut[0].putAt(3, mCudaVatom0);
//        mDataOut[0].putAt(4, mCudaVatom1);
//        mDataOut[0].putAt(5, mCudaBufNlDx);
//        mDataOut[0].putAt(6, mCudaBufNlDy);
//        mDataOut[0].putAt(7, mCudaBufNlDz);
//        mDataOut[0].putAt(8, mCudaBufNlType);
//        mDataOut[0].putAt(9, mCudaBufNlIdx);
//        mDataOut[0].putAt(10, mCudaBufNeiNum);
//        mDataOut[0].putAt(11, mCudaBufCType);
//        mDataOut[0].putAt(12, mCudaBufGradNlDx);
//        mDataOut[0].putAt(13, mCudaBufGradNlDy);
//        mDataOut[0].putAt(14, mCudaBufGradNlDz);
//
//        tCode = mComputeLammpsCuda.invoke(mDataIn[0], mDataOut[0]);
//        CudaCore.cudaExceptionCheck(tCode);
//
//        // cuda -> lammps
//        mInNums[0].putAt(0, inum);
//        mInNums[0].putAt(1, nlocalghost);
//        mInNums[0].putAt(2, aPair.eflagEither()?1:0);
//        mInNums[0].putAt(3, aPair.vflagEither()?1:0);
//        mInNums[0].putAt(4, aPair.eflagAtom()?1:0);
//        mInNums[0].putAt(5, aPair.vflagAtom()?1:0);
//        mInNums[0].putAt(6, cvflagAtom?1:0);
//
//        mDataIn[0].putAt(0, mInNums[0]);
//        mDataIn[0].putAt(1, mFltBuf);
//        mDataIn[0].putAt(2, ilist);
//        mDataIn[0].putAt(3, mCudaF1);
//        mDataIn[0].putAt(4, mCudaEatom0);
//        mDataIn[0].putAt(5, mCudaVatom0);
//        mDataIn[0].putAt(6, mCudaVatom1);
//
//        mDataOut[0].putAt(0, aPair.atomF());
//        mDataOut[0].putAt(1, aPair.engVdwl());
//        mDataOut[0].putAt(2, aPair.eatom());
//        mDataOut[0].putAt(3, aPair.virial());
//        mDataOut[0].putAt(4, aPair.vatom());
//        mDataOut[0].putAt(5, aPair.cvatom());
//
//        tCode = mCuda2Lammps.invoke(mDataIn[0], mDataOut[0]);
//        CudaCore.cudaExceptionCheck(tCode);
    }
    
    void computeGPUMD(int number_of_particles, int N1, int N2, int neighnumMax,
                      long g_neighbor_number, long g_neighbor_list,
                      long nl_dx, long nl_dy, long nl_dz,
                      long g_type,
                      long g_fx, long g_fy, long g_fz,
                      long g_virial, long g_potential) throws CudaException {
        // 近邻列表缓存向量长度规范
        int tTotNeiNum = number_of_particles*neighnumMax;
        mPtrMng.ensureCapacity(mCudaBufNlType, tTotNeiNum);
        mPtrMng.ensureCapacity(mCudaBufNlIdx, tTotNeiNum);
        mPtrMng.ensureCapacity(mCudaBufNlDx, tTotNeiNum);
        mPtrMng.ensureCapacity(mCudaBufNlDy, tTotNeiNum);
        mPtrMng.ensureCapacity(mCudaBufNlDz, tTotNeiNum);
        mPtrMng.ensureCapacity(mCudaBufGradNlDx, tTotNeiNum);
        mPtrMng.ensureCapacity(mCudaBufGradNlDy, tTotNeiNum);
        mPtrMng.ensureCapacity(mCudaBufGradNlDz, tTotNeiNum);
        
//        // 数据打包
//        mInNums[0].putAt(0, number_of_particles);
//        mInNums[0].putAt(1, N1);
//        mInNums[0].putAt(2, N2);
//        mInNums[0].putAt(3, neighnumMax);
//
//        mDataIn[0].putAt(0, mInNums[0]);
//        mDataIn[0].putAt(1, new CudaPointer(g_neighbor_number));
//        mDataIn[0].putAt(2, new CudaPointer(g_neighbor_list));
//        mDataIn[0].putAt(3, new CudaPointer(nl_dx));
//        mDataIn[0].putAt(4, new CudaPointer(nl_dy));
//        mDataIn[0].putAt(5, new CudaPointer(nl_dz));
//        mDataIn[0].putAt(6, new CudaPointer(g_type));
//        mDataIn[0].putAt(7, mCudaFpHyperParam);
//        mDataIn[0].putAt(8, mCudaFpParam);
//        mDataIn[0].putAt(9, mCudaNnParam);
//        mDataIn[0].putAt(10, mCudaNormParam);
//
//        mDataOut[0].putAt(0, new CudaPointer(g_fx));
//        mDataOut[0].putAt(1, new CudaPointer(g_fy));
//        mDataOut[0].putAt(2, new CudaPointer(g_fz));
//        mDataOut[0].putAt(3, new CudaPointer(g_virial));
//        mDataOut[0].putAt(4, new CudaPointer(g_potential));
//        mDataOut[0].putAt(5, mCudaBufNlDx);
//        mDataOut[0].putAt(6, mCudaBufNlDy);
//        mDataOut[0].putAt(7, mCudaBufNlDz);
//        mDataOut[0].putAt(8, mCudaBufNlType);
//        mDataOut[0].putAt(9, mCudaBufNlIdx);
//        mDataOut[0].putAt(10, mCudaBufGradNlDx);
//        mDataOut[0].putAt(11, mCudaBufGradNlDy);
//        mDataOut[0].putAt(12, mCudaBufGradNlDz);
//
//        int tCode = mComputeGPUMD.invoke(mDataIn[0], mDataOut[0]);
//        CudaCore.cudaExceptionCheck(tCode);
    }
}
