package jtoolex.ml;


import jtool.code.CS;
import jtool.code.UT;
import jtool.code.collection.AbstractCollections;
import jtool.code.collection.NewCollections;
import jtool.code.filter.IIndexFilter;
import jtool.math.vector.ILogicalVector;
import jtool.math.vector.IVector;
import jtool.math.vector.LogicalVector;
import jtool.math.vector.Vectors;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jtool.code.CS.RANDOM;

/**
 * 机器学习使用，决策树的 jtool 实现，
 * 这里只接受浮点的 {@link IVector} 输入
 * @author liqa
 */
public class DecisionTree {
    
    /** 内部使用的节点类 */
    interface INode {
        /** 根据输入判断获取下一个节点 */
        INode nextNode(IVector aInput);
        /** 判断是否是叶节点 */
        boolean isLeaf();
        /** 如果是叶节点，返回叶节点的结果 */
        boolean result();
    }
    /** 叶节点 */
    static final class NodeLeaf implements INode {
        private final boolean mResult;
        public NodeLeaf(boolean aResult) {mResult = aResult;}
        @Override public INode nextNode(IVector aInput) {throw new UnsupportedOperationException("nextNode");}
        @Override public boolean isLeaf() {return true;}
        @Override public boolean result() {return mResult;}
    }
    /** 二叉树的节点 */
    static abstract class NodeBinary implements INode {
        protected final INode mLeft, mRight;
        protected final int mIndex;
        public NodeBinary(INode aLeft, INode aRight, int aIndex) {
            mLeft = aLeft; mRight = aRight;
            mIndex = aIndex; // 需要记录分类输入对应的 index，即输入变量的种类
        }
        @Override public final boolean isLeaf() {return false;}
        @Override public final boolean result() {throw new UnsupportedOperationException("result");}
    }
    /** 连续情况节点，需要记录此节点考虑的分划点 */
    static final class NodeContinue extends NodeBinary {
        private final double mSplit;
        public NodeContinue(INode aLeft, INode aRight, int aIndex, double aSplit) {
            super(aLeft, aRight, aIndex);
            mSplit = aSplit;
        }
        /** 小于分化点则返回左分支的节点，反之返回右分支节点 */
        @Override public INode nextNode(IVector aInput) {
            return aInput.get(mIndex)<mSplit ? mLeft : mRight;
        }
    }
    
    
    /** 决策树使用的构造器 */
    public static class Builder {
        /** 输入训练数据组成的 List（不适用矩阵因为在优化过程中会不断拆分样本） */
        private final List<? extends IVector> mTrainDataInput;
        /** 输出训练数据 */
        private final ILogicalVector mTrainDataOutput;
        /** 对于连续变量最大的分划次数 */
        private int mMaxSplit = 16;
        /** 决策树最大的深度 */
        private int mMaxDepth = 16;
        /** 最小不纯度 */
        private double mMinImpurity = 0.001;
        /** 最小分类样本数目 */
        private int mMinSample = 1;
        /** 停止搜寻最小 Gini 指数的阈值 */
        private double mStopGini = 0.0001;
        /** 可定义的随机数生成器，默认为 {@link CS#RANDOM} */
        private Random mRNG = RANDOM;
        
        private final int mSampleNum;
        private final int mInputDim;
        
        Builder(List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput) {
            mTrainDataInput = aTrainDataInput;
            mTrainDataOutput = aTrainDataOutput;
            // 输入输出样本数应匹配
            mSampleNum = mTrainDataInput.size();
            mInputDim = UT.Code.first(mTrainDataInput).size();
            if (mSampleNum != mTrainDataOutput.size()) throw new IllegalArgumentException("Sample Number of Input and Output should be same, input: "+mSampleNum+", output: "+mTrainDataOutput.size());
        }
        
        /** 修改参数 */
        public Builder setMaxSplit(int aMaxSplit) {mMaxSplit = Math.max(1, aMaxSplit); return this;}
        public Builder setMaxDepth(int aMaxDepth) {mMaxDepth = Math.max(1, aMaxDepth); return this;}
        public Builder setMinImpurity(double aMinImpurity) {mMinImpurity = Math.max(0.0, aMinImpurity); return this;}
        public Builder setMinSample(int aMinSample) {mMinSample = Math.max(1, aMinSample); return this;}
        public Builder setStopGini(double aStopGini) {mStopGini = Math.max(0.0, aStopGini); return this;}
        public Builder setRNG(long aSeed) {mRNG = new Random(aSeed); return this;}
        
        
        /** 构造并返回决策树 */
        public DecisionTree build() {
            // 连续值对于重要部分进行分点
            IVector[] rCharacteristics = new IVector[mInputDim]; // 每个连续变量获取到的分点组成的向量
            for (int i = 0; i < mInputDim; ++i) {
                // 需要先将值按照从小到大进行排序，并统计 ture 和 false 的数目
                NavigableMap<Double, Integer> tCountMap = new TreeMap<>();
                for (int j = 0; j < mSampleNum; ++j) {
                    final int subCount = mTrainDataOutput.get(j) ? 1 : -1;
                    tCountMap.compute(mTrainDataInput.get(j).get(i), (k, v) -> v==null ? subCount : v+subCount);
                }
                // 然后统计所有使得 ture 数目变化较大的点作为分点
                rCharacteristics[i] = getSplit(tCountMap);
            }
            // 使用 CART 方法构造决策树并获取根节点
            return new DecisionTree(getRootNodeCART(mTrainDataInput, mTrainDataOutput, rCharacteristics, 0));
        }
        
        /** stuff to override */
        protected int getConsiderCharaNumber(int aAllCharaNum) {return aAllCharaNum;}
        
        protected static double Gini(double aProb) {
            return 2.0 * aProb * (1.0 - aProb);
        }
        
        /** 统计所有使得结果变化较大的点作为分点，分点价值选取分点左右两分点间所有的 |真样本-假样本| */
        final IVector getSplit(NavigableMap<Double, Integer> aCountMap) {
            // TreeMap 转为 Vector
            final IVector tKeys = Vectors.from(aCountMap.keySet());
            final IVector tCounts = Vectors.from(aCountMap.values());
            final int tSize = tKeys.size();
            final int tSizePP = tSize+1;
            final int tSizeMM = tSize-1;
            // 统计所有待选分划点的变化率，这里区分正负并且增加两端；
            // 同样简单起见，直接使用随机访问而不是迭代器
            IVector tSplitRate = Vectors.zeros(tSizePP);
            for (int i = 1; i < tSize; ++i) {
                int imm = i - 1;
                tSplitRate.set(i, (tCounts.get(i) - tCounts.get(imm))/(tKeys.get(i) - tKeys.get(imm)));
            }
            // 选取变化率的峰值作为分点；
            // 同样简单起见，直接使用随机访问而不是迭代器
            final IVector rSplit = Vectors.NaN(tSizeMM);
            int rSplitNum = 0;
            for (int i = 0; i < tSizeMM; ++i) {
                double tRateL = tSplitRate.get(i);
                double tRateM = tSplitRate.get(i+1);
                double tRateR = tSplitRate.get(i+2);
                if ((tRateM>0.0 && tRateM>=tRateL && tRateM>tRateR) || (tRateM<0.0 && tRateM<=tRateL && tRateM<tRateR)) {
                    rSplit.set(i, (tKeys.get(i) + tKeys.get(i+1))*0.5);
                    ++rSplitNum;
                }
            }
            IIndexFilter tSplitIndex = i -> !Double.isNaN(rSplit.get(i));
            if (rSplitNum <= mMaxSplit) return rSplit.slicer().get(tSplitIndex);
            // 超过限制则需要移除多余分点，统计分点的权重
            final IVector rWeight = Vectors.NaN(tSizeMM);
            rWeight.refSlicer().get(tSplitIndex).fill(0.0);
            for (int i = 0; i < tSize; ++i) {
                double tCount = tCounts.get(i);
                for (int j = 0; j < tSizeMM; ++j) if (tSplitIndex.accept(j)) {
                    // 右侧分点增加计数，左侧减少计数，得到两侧的计数差值
                    if (j >= i) rWeight.add(j, tCount);
                    else rWeight.add(j, -tCount);
                }
            }
            final IVector tWeight = rWeight.slicer().get(tSplitIndex);
            // 权重取绝对值
            tWeight.operation().map2this(Math::abs);
            // 排序选取权重最高的 aMaxSplit 个分点
            List<Integer> rSortedIndex = NewCollections.from(tSizeMM, i->i);
            rSortedIndex.sort(Comparator.comparingDouble(tWeight::get).reversed());
            return rSplit.refSlicer().get(tSplitIndex).refSlicer().get(rSortedIndex).slicer().get(AbstractCollections.range(mMaxSplit));
        }
        
        /** 递归方式生成树 */
        final INode getRootNodeCART(List<? extends IVector> aDataInput, ILogicalVector aDataOutput, @Nullable IVector[] aCharacteristics, int aCurrentDepth) {
            // 统计必要数据
            int tSampleNum = aDataInput.size();
            int tPositiveNum = aDataOutput.count();
            ILogicalVector tValidChara = LogicalVector.zeros(mInputDim);
            tValidChara.fill(i -> aCharacteristics[i] != null);
            int tValidCharaNum = tValidChara.count();
            // 所有标注相同，返回标注的叶节点
            if (tPositiveNum == 0) return new NodeLeaf(false);
            if (tPositiveNum == tSampleNum) return new NodeLeaf(true);
            // 特征向量为空，返回标注最多的叶节点
            if (tValidCharaNum==0) return new NodeLeaf(tPositiveNum*2 > tSampleNum);
            // 样本数不足最小样本数，强制返回叶节点
            if (tSampleNum <= mMinSample) return new NodeLeaf(tPositiveNum*2 > tSampleNum);
            // 节点的不纯性小于最小值，强制返回叶节点
            if (Gini(tPositiveNum/(double)tSampleNum) < mMinImpurity) return new NodeLeaf(tPositiveNum*2 > tSampleNum);
            // 深度超过了最大深度，强制返回叶节点
            if (aCurrentDepth >= mMaxDepth) return new NodeLeaf(tPositiveNum*2 > tSampleNum);
            ++aCurrentDepth;
            
            // 统计需要考虑的特征变量
            int tConsiderNum = getConsiderCharaNumber(tValidCharaNum);
            IIndexFilter tConsiderID;
            if (tConsiderNum < tValidCharaNum) {
                List<Integer> rRandIndex = NewCollections.filterInteger(tValidChara.size(), tValidChara);
                Collections.shuffle(rRandIndex, mRNG);
                ILogicalVector rConsiderID = LogicalVector.zeros(mInputDim);
                rConsiderID.refSlicer().get(rRandIndex.subList(0, tConsiderNum)).fill(true);
                tConsiderID = rConsiderID;
            } else {
                tConsiderID = tValidChara;
            }
            // 计算每个特征的 Gini 指数，统计最小的特征作为分点
            int rMinCharaID = -1, rMinCharaIndex = -1;
            double rMinCharaValue = Double.NaN;
            double rMinGini = Double.POSITIVE_INFINITY;
            for (int charaID = 0; charaID < mInputDim; ++charaID) if (tConsiderID.accept(charaID)) {
                IVector tCharacteristic = aCharacteristics[charaID];
                assert tCharacteristic != null;
                for (int charaIndex = 0; charaIndex < tCharacteristic.size(); ++charaIndex) {
                    double tCharaValue = tCharacteristic.get(charaIndex);
                    double tGini = statGini(aDataInput, aDataOutput, charaID, tCharaValue);
                    if (tGini < rMinGini) {
                        rMinGini = tGini;
                        rMinCharaID = charaID;
                        rMinCharaIndex = charaIndex;
                        rMinCharaValue = tCharaValue;
                    }
                    if (rMinGini < mStopGini) break;
                }
                if (rMinGini < mStopGini) break;
            }
            
            // 按照统计结果拆分数据
            List<IVector> rDataInputLeft = new ArrayList<>();
            List<IVector> rDataInputRight = new ArrayList<>();
            LogicalVector.Builder rDataOutputLeft = LogicalVector.builder();
            LogicalVector.Builder rDataOutputRight = LogicalVector.builder();
            for (int i = 0; i < tSampleNum; ++i) {
                IVector tInput = aDataInput.get(i);
                boolean tOutput = aDataOutput.get(i);
                if (tInput.get(rMinCharaID) < rMinCharaValue) {
                    rDataInputLeft.add(tInput);
                    rDataOutputLeft.add(tOutput);
                } else {
                    rDataInputRight.add(tInput);
                    rDataOutputRight.add(tOutput);
                }
            }
            // 拆分失败同样强制返回叶节点
            if (rDataInputLeft.isEmpty() || rDataInputRight.isEmpty()) return new NodeLeaf(tPositiveNum*2 > tSampleNum);
            
            // 拆分一下特征标，分化点也分为左右两部分
            IVector subChara = aCharacteristics[rMinCharaID];
            assert subChara != null;
            IVector[] rCharaLeft = Arrays.copyOf(aCharacteristics, aCharacteristics.length);
            rCharaLeft[rMinCharaID] = rMinCharaIndex > 0 ? subChara.slicer().get(AbstractCollections.range(rMinCharaIndex)) : null;
            IVector[] rCharaRight = Arrays.copyOf(aCharacteristics, aCharacteristics.length);
            rCharaRight[rMinCharaID] = rMinCharaIndex+1 < subChara.size() ? subChara.slicer().get(AbstractCollections.range(rMinCharaIndex+1, subChara.size())) : null;
            
            // 按照统计结果进行扩展节点
            return new NodeContinue(getRootNodeCART(rDataInputLeft, rDataOutputLeft.build(), rCharaLeft, aCurrentDepth), getRootNodeCART(rDataInputRight, rDataOutputRight.build(), rCharaRight, aCurrentDepth), rMinCharaID, rMinCharaValue);
        }
        
        /** 统计数据中某个特征某个取值时的 Gini 指数 */
        double statGini(List<? extends IVector> aDataInput, ILogicalVector aDataOutput, int aCharaID, double aCharaValue) {
            int tN = aDataInput.size();
            int tNp = 0, tNpp = 0; // 统计此特征为真以及并且结果为真的样本个数
            int tNnp = 0;          // 统计此特征为假并且结果为真的样本个数
            for (int i = 0; i < tN; ++i) {
                if (aDataInput.get(i).get(aCharaID) < aCharaValue) {
                    ++tNp;
                    if (aDataOutput.get(i)) ++tNpp;
                } else {
                    if (aDataOutput.get(i)) ++tNnp;
                }
            }
            int tNn = tN - tNp;
            if (tNp == 0) return tNn/(double)tN * Gini(tNnp/(double)tNn);
            if (tNn == 0) return tNp/(double)tN * Gini(tNpp/(double)tNp);
            return tNp/(double)tN * Gini(tNpp/(double)tNp) + tNn/(double)tN * Gini(tNnp/(double)tNn);
        }
    }
    
    
    
    /** 存储一个根节点即可 */
    private final INode mRoot;
    DecisionTree(INode aRoot) {
        mRoot = aRoot;
    }
    
    public static Builder builder(List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput) {return new Builder(aTrainDataInput, aTrainDataOutput);}
    
    /** 输入 x 进行进行决策判断 */
    public boolean makeDecision(IVector aInput) {
        INode tNode = mRoot;
        while (!tNode.isLeaf()) {
            tNode = tNode.nextNode(aInput);
        }
        return tNode.result();
    }
}
