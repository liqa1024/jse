package jtoolex.ml;

import com.google.common.collect.ImmutableMap;
import jtool.code.CS;
import jtool.code.UT;
import jtool.code.collection.AbstractCollections;
import jtool.code.collection.NewCollections;
import jtool.iofile.ISavable;
import jtool.math.MathEX;
import jtool.math.random.LocalRandom;
import jtool.math.vector.ILogicalVector;
import jtool.math.vector.IVector;
import jtool.parallel.AbstractThreadPool;
import jtool.parallel.ParforThreadPool;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static jtool.code.CS.DEFAULT_THREAD_NUM;
import static jtool.code.CS.RANDOM;

/**
 * 机器学习使用，随机森林的 jtool 实现，
 * 这里只接受浮点的 {@link IVector} 输入
 * @author liqa
 */
public class RandomForest extends AbstractThreadPool<ParforThreadPool> implements ISavable {
    
    private final List<DecisionTree> mTrees;
    /** 构造一个空的随机森林，用于使用 put 手动构造 */
    public RandomForest(int aThreadNum) {super(new ParforThreadPool(aThreadNum, true)); mTrees = new ArrayList<>();}
    public RandomForest() {this(DEFAULT_THREAD_NUM);} // 随机森林默认会开启并行
    public RandomForest put(DecisionTree aTree) {mTrees.add(aTree); return this;}
    
    /** 随机森林使用略微修改的决策树来增加随机性 */
    public static DecisionTree.Builder treeBuilder(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput) {
        return new DecisionTree.Builder(aTrainDataInput, aTrainDataOutput) {
            @Override protected int getConsiderCharaNumber(int aAllCharaNum) {
                return Math.max(1, (int)Math.round(MathEX.Fast.sqrt(aAllCharaNum)));
            }
        }.setMaxDepth(8);
    }
    
    /** 现在支持设置线程数 */
    public RandomForest setThreadNum(int aThreadNum)  {if (aThreadNum!=nThreads()) setPool(new ParforThreadPool(aThreadNum, true)); return this;}
    
    /** 输入 x 进行进行决策判断 */
    public double predict(final IVector aInput) {
        int tThreadNum = nThreads();
        int tTreeNum = mTrees.size();
        int[] rPredTrueNumBuffer = new int[tThreadNum];
        pool().parfor(tTreeNum, (i, threadID) -> {
            if (mTrees.get(i).makeDecision(aInput)) ++rPredTrueNumBuffer[threadID];
        });
        int tPredTrueNum = 0;
        for (int subPredTrueNum : rPredTrueNumBuffer) tPredTrueNum += subPredTrueNum;
        return tPredTrueNum / (double)tTreeNum;
    }
    public boolean makeDecision(final IVector aInput, double aRatio) {
        return predict(aInput) > aRatio;
    }
    public boolean makeDecision(IVector aInput) {
        return makeDecision(aInput, 0.5);
    }
    
    
    
    /** save/load，因为这里转为 map 是引用的，因此不等价于原本的 save 操作 */
    public Map<String, Object> asMap() {
        return ImmutableMap.of("trees", AbstractCollections.map(mTrees, DecisionTree::asMap));
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.putAll(asMap());
    }
    public static RandomForest load(Map<?, ?> aLoadFrom) {
        RandomForest rRandomForest = new RandomForest();
        rRandomForest.mTrees.addAll(AbstractCollections.map((List<?>)aLoadFrom.get("trees"), obj->DecisionTree.load((Map<?, ?>)obj)));
        return rRandomForest;
    }
    
    
    private static long[] genSeeds_(int aSize, Random aRNG) {
        long[] rSeeds = new long[aSize];
        for (int i = 0; i < aSize; ++i) rSeeds[i] = aRNG.nextLong();
        return rSeeds;
    }
    /**
     * 根据输入参数直接构造随机森林，对于随机森林，
     * 决策树内部的参数不再重要，因此这里也不再直接提供接口
     * @author liqa
     * @param aNoPBar 是否关闭进度条，默认 false（开启进度条）
     * @param aTrainDataInput 训练样本的输入数据
     * @param aTrainDataOutput 训练样本的输出数据
     * @param aTreeNum 创建的决策树数目，默认为 1000
     * @param aTrainRatio 每个决策树使用总样本数的比例，默认为 0.01
     * @param aThreadNum 随机森林使用的线程数，默认为处理器线程数
     * @param aRNG 可自定义的随机数生成器，默认为 {@link CS#RANDOM}
     */
    RandomForest(final boolean aNoPBar, final @Unmodifiable List<? extends IVector> aTrainDataInput, final ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio, int aThreadNum, Random aRNG, boolean aNoCompetitive) {
        super(new ParforThreadPool(aThreadNum, aNoCompetitive));
        
        // 输入输出样本数应匹配
        final int tSampleNum = aTrainDataInput.size();
        if (tSampleNum != aTrainDataOutput.size()) throw new IllegalArgumentException("Sample Number of Input and Output should be same, input: "+tSampleNum+", output: "+aTrainDataOutput.size());
        
        // 获取训练需要的样本数
        final int tTrainSampleNum = Math.max(1, (int)Math.round(tSampleNum * aTrainRatio));
        
        int tThreadNum = nThreads();
        // 为了保证结果可重复，这里统一为每个线程生成一个种子，用于创建 LocalRandom
        final long[] tSeeds = genSeeds_(tThreadNum, aRNG);
        // 统一创建好 mTrees 避免并行写入的问题
        mTrees = NewCollections.nulls(aTreeNum);
        
        // 并行创建随机森林
        if (!aNoPBar) UT.Timer.progressBar("RandomForest Init", aTreeNum);
        pool().parfor(aTreeNum, (i, threadID) -> {
            LocalRandom tRNG = new LocalRandom(tSeeds[threadID]);
            // 随机选取部分样本集
            List<Integer> tRandIndex = NewCollections.from(tSampleNum, j->j);
            LocalRandom.shuffle(tRandIndex, tRNG);
            tRandIndex = tRandIndex.subList(0, tTrainSampleNum);
            List<IVector> subTrainDataInput = AbstractCollections.slice(aTrainDataInput, tRandIndex);
            ILogicalVector subTrainDataOutput = aTrainDataOutput.refSlicer().get(tRandIndex);
            // 随机森林使用略微修改的决策树来增加随机性
            mTrees.set(i, treeBuilder(subTrainDataInput, subTrainDataOutput).setRNG(tRNG.nextLong()).build());
            if (!aNoPBar) UT.Timer.progressBar();
        });
        
        // 统一设置回竞争形式的 pool，预测开启非竞争可以提高效率
        if (aThreadNum!=1 && !aNoCompetitive) setPool(new ParforThreadPool(aThreadNum, true));
    }
    RandomForest(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio, int aThreadNum, Random aRNG, boolean aNoCompetitive) {
        this(false, aTrainDataInput, aTrainDataOutput, aTreeNum, aTrainRatio, aThreadNum, aRNG, aNoCompetitive);
    }
    public RandomForest(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio, int aThreadNum, long aSeed) {
        this(aTrainDataInput, aTrainDataOutput, aTreeNum, aTrainRatio, aThreadNum, new Random(aSeed), true);
    }
    public RandomForest(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio, int aThreadNum) {
        this(aTrainDataInput, aTrainDataOutput, aTreeNum, aTrainRatio, aThreadNum, RANDOM, false);
    }
    public RandomForest(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio) {
        this(aTrainDataInput, aTrainDataOutput, aTreeNum, aTrainRatio, DEFAULT_THREAD_NUM);
    }
    public RandomForest(@Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput) {
        this(aTrainDataInput, aTrainDataOutput, 1000, 0.01);
    }
    
    public RandomForest(boolean aNoPBar, @Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio, int aThreadNum, long aSeed) {
        this(aNoPBar, aTrainDataInput, aTrainDataOutput, aTreeNum, aTrainRatio, aThreadNum, new Random(aSeed), true);
    }
    public RandomForest(boolean aNoPBar, @Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio, int aThreadNum) {
        this(aNoPBar, aTrainDataInput, aTrainDataOutput, aTreeNum, aTrainRatio, aThreadNum, RANDOM, false);
    }
    public RandomForest(boolean aNoPBar, @Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput, int aTreeNum, double aTrainRatio) {
        this(aNoPBar, aTrainDataInput, aTrainDataOutput, aTreeNum, aTrainRatio, DEFAULT_THREAD_NUM);
    }
    public RandomForest(boolean aNoPBar, @Unmodifiable List<? extends IVector> aTrainDataInput, ILogicalVector aTrainDataOutput) {
        this(aNoPBar, aTrainDataInput, aTrainDataOutput, 1000, 0.01);
    }
}
