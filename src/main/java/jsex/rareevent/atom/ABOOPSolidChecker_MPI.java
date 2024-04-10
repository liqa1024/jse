package jsex.rareevent.atom;

import jse.atom.MonatomicParameterCalculator;
import jse.math.MathEX;
import jse.math.vector.ILogicalVector;
import jse.math.vector.IVector;
import jse.parallel.MPI;
import jse.cache.VectorCache;
import jse.parallel.MPIException;

import static jse.code.CS.R_NEAREST_MUL;

/**
 * 内部使用 {@link MonatomicParameterCalculator#calConnectCountABOOP_MPI} 来进行计算的固体判据器
 * @author liqa
 */
public class ABOOPSolidChecker_MPI implements ISolidChecker {
    private MPI.Comm mComm = MPI.Comm.WORLD; // 用来 MPI 计算的 Comm
    
    private double mRNearestMul = R_NEAREST_MUL; // 用来搜索的最近邻半径的倍数
    private int mNnn = -1; // 最大最近邻数目
    
    private int mLInBOOP = 6; // 计算 BOOP 所用的 l，默认为 6
    private double mConnectThreshold =  0.83; // 用来判断两个原子是否是相连接的阈值，默认为 0.83
    private int mSolidThreshold = 7; // 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 7
    private boolean mUseRatio = false; // 是否使用比例版本，此时 mSolidThreshold 会失效，默认关闭保持兼容
    
    /** 将一些设置参数放在这里避免过于复杂的构造函数 */
    public ABOOPSolidChecker_MPI setComm(MPI.Comm aComm) {mComm = aComm; return this;}
    public ABOOPSolidChecker_MPI setRNearestMul(double aRNearestMul) {mRNearestMul = Math.max(0.1, aRNearestMul); return this;}
    public ABOOPSolidChecker_MPI setNnn(int aNnn) {mNnn = aNnn; return this;}
    public ABOOPSolidChecker_MPI setLInBOOP(int aLInBOOP) {mLInBOOP = Math.max(1, aLInBOOP); return this;}
    public ABOOPSolidChecker_MPI setConnectThreshold(double aConnectThreshold) {mConnectThreshold = MathEX.Code.toRange(0.0, 1.0, aConnectThreshold); return this;}
    public ABOOPSolidChecker_MPI setSolidThreshold(int aSolidThreshold) {mSolidThreshold = Math.max(0, aSolidThreshold); return this;}
    public ABOOPSolidChecker_MPI setUseRatio(boolean aUseRatio) {mUseRatio = aUseRatio; return this;}
    public ABOOPSolidChecker_MPI setUseRatio() {return setUseRatio(true);}
    
    @Override public ILogicalVector checkSolid(MonatomicParameterCalculator aMPC) {
        try {
            ILogicalVector tIsSolid;
            if (mUseRatio) {
                IVector tConnectRatio = aMPC.calConnectRatioABOOP_MPI(mComm, mLInBOOP, mConnectThreshold, aMPC.unitLen()*mRNearestMul, mNnn);
                tIsSolid = tConnectRatio.greaterOrEqual(0.5);
                VectorCache.returnVec(tConnectRatio);
            } else {
                IVector tConnectCount = aMPC.calConnectCountABOOP_MPI(mComm, mLInBOOP, mConnectThreshold, aMPC.unitLen()*mRNearestMul, mNnn);
                tIsSolid = tConnectCount.greaterOrEqual(mSolidThreshold);
                VectorCache.returnVec(tConnectCount);
            }
            return tIsSolid;
        } catch (MPIException e) {
            throw new RuntimeException(e);
        }
    }
}
