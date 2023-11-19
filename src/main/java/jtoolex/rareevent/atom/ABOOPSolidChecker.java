package jtoolex.rareevent.atom;

import jtool.atom.MonatomicParameterCalculator;
import jtool.math.MathEX;
import jtool.math.vector.ILogicalVector;

import static jtool.code.CS.R_NEAREST_MUL;

/**
 * 内部使用 {@link MonatomicParameterCalculator#calConnectCountABOOP} 来进行计算的固体判据器
 * @author liqa
 */
public class ABOOPSolidChecker implements ISolidChecker {
    private double mRNearestMul = R_NEAREST_MUL; // 用来搜索的最近邻半径的倍数
    private int mNnn = -1; // 最大最近邻数目
    
    private int mLInBOOP = 6; // 计算 BOOP 所用的 l，默认为 6
    private double mConnectThreshold =  0.83; // 用来判断两个原子是否是相连接的阈值，默认为 0.83
    private int mSolidThreshold = 7; // 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 7
    
    /** 将一些设置参数放在这里避免过于复杂的构造函数 */
    public ABOOPSolidChecker setRNearestMul(double aRNearestMul) {mRNearestMul = Math.max(0.1, aRNearestMul); return this;}
    public ABOOPSolidChecker setNnn(int aNnn) {mNnn = aNnn; return this;}
    public ABOOPSolidChecker setLInBOOP(int aLInBOOP) {mLInBOOP = Math.max(1, aLInBOOP); return this;}
    public ABOOPSolidChecker setConnectThreshold(double aConnectThreshold) {mConnectThreshold = MathEX.Code.toRange(0.0, 1.0, aConnectThreshold); return this;}
    public ABOOPSolidChecker setSolidThreshold(int aSolidThreshold) {mSolidThreshold = Math.max(0, aSolidThreshold); return this;}
    
    @Override public ILogicalVector checkSolid(MonatomicParameterCalculator aMPC) {
        return aMPC.calConnectCountABOOP(mLInBOOP, mConnectThreshold, aMPC.unitLen()*mRNearestMul, mNnn).greaterOrEqual(mSolidThreshold);
    }
}
