package jtoolex.rareevent.atom;

import jtool.atom.MonatomicParameterCalculator;
import jtool.math.MathEX;
import jtool.math.vector.ILogicalVector;
import jtool.math.vector.IVector;
import jtool.parallel.MPI;
import jtool.parallel.VectorCache;

import static jtool.code.CS.R_NEAREST_MUL;

/**
 * 内部使用 {@link MonatomicParameterCalculator#calConnectCountBOOP_MPI} 来进行计算的固体判据器
 * @author liqa
 */
public class BOOPSolidChecker_MPI implements ISolidChecker {
    private MPI.Comm mComm = MPI.Comm.WORLD; // 用来 MPI 计算的 Comm
    
    private double mRNearestMul = R_NEAREST_MUL; // 用来搜索的最近邻半径的倍数
    private int mNnn = -1; // 最大最近邻数目
    
    private int mLInBOOP = 6; // 计算 BOOP 所用的 l，默认为 6
    private double mConnectThreshold =  0.5; // 用来判断两个原子是否是相连接的阈值，默认为 0.5
    private int mSolidThreshold = 7; // 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 7
    
    /** 将一些设置参数放在这里避免过于复杂的构造函数 */
    public BOOPSolidChecker_MPI setComm(MPI.Comm aComm) {mComm = aComm; return this;}
    public BOOPSolidChecker_MPI setRNearestMul(double aRNearestMul) {mRNearestMul = Math.max(0.1, aRNearestMul); return this;}
    public BOOPSolidChecker_MPI setNnn(int aNnn) {mNnn = aNnn; return this;}
    public BOOPSolidChecker_MPI setLInBOOP(int aLInBOOP) {mLInBOOP = Math.max(1, aLInBOOP); return this;}
    public BOOPSolidChecker_MPI setConnectThreshold(double aConnectThreshold) {mConnectThreshold = MathEX.Code.toRange(0.0, 1.0, aConnectThreshold); return this;}
    public BOOPSolidChecker_MPI setSolidThreshold(int aSolidThreshold) {mSolidThreshold = Math.max(0, aSolidThreshold); return this;}
    
    @Override public ILogicalVector checkSolid(MonatomicParameterCalculator aMPC) {
        IVector tConnectCount = aMPC.calConnectCountBOOP_MPI(mComm, mLInBOOP, mConnectThreshold, aMPC.unitLen()*mRNearestMul, mNnn);
        ILogicalVector tIsSolid = tConnectCount.greaterOrEqual(mSolidThreshold);
        VectorCache.returnVec(tConnectCount);
        return tIsSolid;
    }
}
