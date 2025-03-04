package jsex.ml;


import com.google.common.collect.ImmutableMap;
import jse.code.CS;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoublePair;
import jse.code.functional.IIndexFilter;
import jse.code.io.ISavable;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

import static jse.code.CS.RANDOM;

/**
 * 机器学习使用，决策树的 jse 实现，
 * 这里只接受浮点的 {@link IVector} 输入
 * @author liqa
 */
public class DecisionTree implements ISavable {
    
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
        final boolean mResult;
        NodeLeaf(boolean aResult) {mResult = aResult;}
        @Override public INode nextNode(IVector aInput) {throw new UnsupportedOperationException("nextNode");}
        @Override public boolean isLeaf() {return true;}
        @Override public boolean result() {return mResult;}
    }
    /** 二叉树的节点 */
    static abstract class NodeBinary implements INode {
        final INode mLeft, mRight;
        final int mIndex;
        NodeBinary(INode aLeft, INode aRight, int aIndex) {
            mLeft = aLeft; mRight = aRight;
            mIndex = aIndex; // 需要记录分类输入对应的 index，即输入变量的种类
        }
        @Override public final boolean isLeaf() {return false;}
        @Override public final boolean result() {throw new UnsupportedOperationException("result");}
    }
    /** 连续情况节点，需要记录此节点考虑的分划点 */
    static final class NodeContinue extends NodeBinary {
        final double mSplit;
        NodeContinue(INode aLeft, INode aRight, int aIndex, double aSplit) {
            super(aLeft, aRight, aIndex);
            mSplit = aSplit;
        }
        /** 小于分化点则返回左分支的节点，反之返回右分支节点 */
        @Override public INode nextNode(IVector aInput) {
            return aInput.get(mIndex)<mSplit ? mLeft : mRight;
        }
    }
    
    
    /** 连续变量的分划策略 */
    public enum SplitPolicy {
          RATE_PEAK
        , UNIFORM
        , RANDOM
    }
    
    /** 决策树使用的构造器 */
    public static class Builder {
        /** 输入训练数据组成的 List（不适用矩阵因为在优化过程中会不断拆分样本） */
        private final @Unmodifiable List<? extends IVector> mTrainDataInput;
        /** 输出训练数据 */
        private final ILogicalVector mTrainDataOutput;
        /** 对于连续变量最大的分划次数 */
        private int mMaxSplit = 5;
        /** 连续变量的分划策略 */
        private SplitPolicy mSplitPolicy = SplitPolicy.RATE_PEAK;
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
        
        Builder(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput) {
            mTrainDataInput = aTrainDataInput;
            mTrainDataOutput = aTrainDataOutput;
            // 输入输出样本数应匹配
            mSampleNum = mTrainDataInput.size();
            mInputDim = UT.Code.first(mTrainDataInput).size();
            if (mSampleNum != mTrainDataOutput.size()) throw new IllegalArgumentException("Sample Number of Input and Output should be same, input: "+mSampleNum+", output: "+mTrainDataOutput.size());
        }
        
        /** 修改参数 */
        public Builder setMaxSplit(int aMaxSplit) {mMaxSplit = Math.max(1, aMaxSplit); return this;}
        public Builder setSplitPolicy(SplitPolicy aSplitPolicy) {mSplitPolicy = aSplitPolicy; return this;}
        public Builder setMaxDepth(int aMaxDepth) {mMaxDepth = Math.max(1, aMaxDepth); return this;}
        public Builder setMinImpurity(double aMinImpurity) {mMinImpurity = Math.max(0.0, aMinImpurity); return this;}
        public Builder setMinSample(int aMinSample) {mMinSample = Math.max(1, aMinSample); return this;}
        public Builder setStopGini(double aStopGini) {mStopGini = Math.max(0.0, aStopGini); return this;}
        public Builder setRNG(long aSeed) {mRNG = new Random(aSeed); return this;}
        
        
        /** 构造并返回决策树 */
        public DecisionTree build() {
            // 连续值对于重要部分进行分点
            @Nullable IVector[] rCharacteristics = new IVector[mInputDim]; // 每个连续变量获取到的分点组成的向量
            for (int i = 0; i < mInputDim; ++i) {
                // 需要先将值按照从小到大进行排序，并统计 ture 和 false 的数目
                NavigableMap<Double, DoublePair> tCountMap = new TreeMap<>();
                for (int j = 0; j < mSampleNum; ++j) {
                    final boolean tOut = mTrainDataOutput.get(j);
                    tCountMap.compute(mTrainDataInput.get(j).get(i), (k, pair) -> {
                        if (pair == null) pair = new DoublePair(0, 0);
                        if (tOut) ++pair.mFirst;
                        else ++pair.mSecond;
                        return pair;
                    });
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
        
        final @Nullable IVector getSplit(NavigableMap<Double, DoublePair> aCountMap) {
            // TreeMap 转为 Vector
            final IVector tKeys = Vectors.from(aCountMap.keySet());
            final int tSize = tKeys.size();
            final int tSizePP = tSize+1;
            final int tSizeMM = tSize-1;
            // 最特殊情况，aCountMap 只有一个值，此时此特征值没有意义，返回 null
            if (tSize <= 1) return null;
            // 特殊情况，如果 aCountMap 是离散的，总数就小于 mMaxSplit，则直接全部都划分
            if (tSizeMM <= mMaxSplit) {
                IVector rSplit = Vectors.zeros(tSizeMM);
                for (int i = 0; i < tSizeMM; ++i) {
                    rSplit.set(i, (tKeys.get(i) + tKeys.get(i+1))*0.5);
                }
                return rSplit;
            }
            // 现在有多种分划策略
            switch (mSplitPolicy) {
            case UNIFORM: {
                // 均匀策略直接统计正负样本总数，按照总数均匀分划
                IVector tCounts = Vectors.from(AbstractCollections.map(aCountMap.values(), pair->pair.mFirst+pair.mSecond));
                final double tThreshold = tCounts.sum() / (mMaxSplit+1.0);
                // 构造分划点
                IVector rSplit = Vectors.zeros(mMaxSplit);
                int tIdx = 0;
                double rThreshold = tCounts.first();
                for (int i = 1; i < tSize; ++i) {
                    rThreshold += tCounts.get(i);
                    if (MathEX.Code.numericGreater(rThreshold, tThreshold)) {
                        rThreshold -= tThreshold;
                        rSplit.set(tIdx, (tKeys.get(i-1) + tKeys.get(i))*0.5);
                        ++tIdx;
                    }
                }
                return rSplit;
            }
            case RANDOM: {
                IIntVector tRandIdx = Vectors.range(tSizeMM);
                tRandIdx.shuffle(mRNG);
                tRandIdx = tRandIdx.subVec(0, mMaxSplit);
                tRandIdx.sort();
                IVector rSplit = Vectors.zeros(mMaxSplit);
                for (int i = 0; i < mMaxSplit; ++i) {
                    int j = tRandIdx.get(i);
                    rSplit.set(i, (tKeys.get(j) + tKeys.get(j+1))*0.5);
                }
                return rSplit;
            }
            case RATE_PEAK: {
                // 统计所有使得结果变化较大的点作为分点，分点价值选取分点左右两分点间所有的 |真样本-假样本|
                // 这里简单起见直接将正负统计数合并
                IVector tCounts = Vectors.from(AbstractCollections.map(aCountMap.values(), pair->pair.mFirst-pair.mSecond));
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
                // 为了避免退化的空分划点破坏一致性，这里检测到空则直接返回 null
                if (rSplitNum == 0) return null;
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
                final IVector tSplit  = rSplit .slicer().get(tSplitIndex);
                // 权重取绝对值
                tWeight.abs2this();
                // 排序选取权重最高的 aMaxSplit 个分点
                tWeight.operation().biSort(tSplit);
                return tSplit.operation().refReverse().subVec(0, mMaxSplit);
            }
            default: throw new RuntimeException();
            }
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
                IIntVector rRandIndex = tValidChara.where();
                rRandIndex.shuffle(mRNG);
                ILogicalVector rConsiderID = LogicalVector.zeros(mInputDim);
                rConsiderID.refSlicer().get(rRandIndex.subVec(0, tConsiderNum)).fill(true);
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
            rCharaLeft[rMinCharaID] = rMinCharaIndex > 0 ? subChara.subVec(0, rMinCharaIndex) : null;
            IVector[] rCharaRight = Arrays.copyOf(aCharacteristics, aCharacteristics.length);
            rCharaRight[rMinCharaID] = rMinCharaIndex+1 < subChara.size() ? subChara.subVec(rMinCharaIndex+1, subChara.size()) : null;
            
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
    
    public static Builder builder(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput) {return new Builder(aTrainDataInput, aTrainDataOutput);}
    public static Builder builder(IVector[] aTrainDataInput, ILogicalVector aTrainDataOutput) {return builder(AbstractCollections.from(aTrainDataInput), aTrainDataOutput);}
    public static Builder builder(IMatrix aTrainDataInput, ILogicalVector aTrainDataOutput) {return builder(aTrainDataInput.rows(), aTrainDataOutput);}
    public static Builder builder(@Unmodifiable List<? extends IVector> aTrainDataInput, Collection<Boolean> aTrainDataOutput) {return builder(aTrainDataInput, Vectors.fromBoolean(aTrainDataOutput));}
    public static Builder builder(IVector[] aTrainDataInput, List<Boolean> aTrainDataOutput) {return builder(aTrainDataInput, Vectors.fromBoolean(aTrainDataOutput));}
    public static Builder builder(IMatrix aTrainDataInput, List<Boolean> aTrainDataOutput) {return builder(aTrainDataInput, Vectors.fromBoolean(aTrainDataOutput));}
    public static Builder builder(@Unmodifiable List<? extends IVector> aTrainDataInput, boolean[] aTrainDataOutput) {return builder(aTrainDataInput, new LogicalVector(aTrainDataOutput));}
    public static Builder builder(IVector[] aTrainDataInput, boolean[] aTrainDataOutput) {return builder(aTrainDataInput, new LogicalVector(aTrainDataOutput));}
    public static Builder builder(IMatrix aTrainDataInput, boolean[] aTrainDataOutput) {return builder(aTrainDataInput, new LogicalVector(aTrainDataOutput));}
    
    /** 输入 x 进行进行决策判断 */
    public boolean makeDecision(IVector aInput) {
        INode tNode = mRoot;
        while (!tNode.isLeaf()) {
            tNode = tNode.nextNode(aInput);
        }
        return tNode.result();
    }
    
    
    /** save/load，因为这里转为 map 是引用的，因此不等价于原本的 save 操作 */
    public Map<String, Object> asMap() {
        return ImmutableMap.of("root", asMap_(mRoot));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.putAll(asMap());
    }
    public static DecisionTree load(Map<?, ?> aLoadFrom) {
        return new DecisionTree(load_((Map<?, ?>)aLoadFrom.get("root")));
    }
    
    
    Map<String, Object> asMap_(INode aNode) {
        if (aNode.isLeaf()) {
            return ImmutableMap.of("type", "leaf",
                                   "result", aNode.result());
        } else
        if (aNode instanceof NodeContinue) {
            NodeContinue tNode = (NodeContinue)aNode;
            return ImmutableMap.of("type" , "continue",
                                   "split", tNode.mSplit,
                                   "index", tNode.mIndex,
                                   "left" , asMap_(tNode.mLeft),
                                   "right", asMap_(tNode.mRight));
        } else {
            throw new RuntimeException();
        }
    }
    static INode load_(Map<?, ?> aLoadFrom) {
        String tType = UT.Code.toString(aLoadFrom.get("type"));
        switch (tType) {
        case "leaf": {
            return new NodeLeaf((Boolean)aLoadFrom.get("result"));
        }
        case "continue": {
            Map<?, ?> tLeft  = (Map<?, ?>)aLoadFrom.get("left");
            Map<?, ?> tRight = (Map<?, ?>)aLoadFrom.get("right");
            int tIndex = ((Number)aLoadFrom.get("index")).intValue();
            double tSplit = ((Number)aLoadFrom.get("split")).doubleValue();
            return new NodeContinue(load_(tLeft), load_(tRight), tIndex, tSplit);
        }
        default: throw new RuntimeException();
        }
    }
}
