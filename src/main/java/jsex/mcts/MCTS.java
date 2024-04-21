package jsex.mcts;

import jse.code.UT;
import jse.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static jse.code.CS.RANDOM;

/**
 * Monte Carlo Tree Search
 * @author liqa
 */
public class MCTS<C> {
    class Node {
        @Nullable Node mParent;
        /** 采用两个列表的方式来快速处理没有访问的特殊情况 */
        private List<Node> mChildrenNoVisit = null;
        private List<Node> mChildren = null;
        /** null 表示还没有创建，需要从父节点来创建；延迟创建可以减少选择过程中的冗余内存占用 */
        private @Nullable IGame<C> mGame;
        /** null 表示没有选择，此时 mGame 为此节点的 Game 而不是父节点的 */
        final @Nullable C mChoice;
        
        /** MCTS stuffs */
        int mVisitTimes = 0;
        double mQuality = 0.0;
        Node(@Nullable Node aParent, @Nullable IGame<C> aGame, @Nullable C aChoice) {
            if (aParent==null && aGame==null) throw new NullPointerException();
            if (aGame==null && aChoice==null) throw new NullPointerException();
            mParent = aParent;
            mGame = aGame;
            mChoice = aChoice;
        }
        void setRoot() {
            validGame_();
            mParent = null;
        }
        private void validGame_() {
            // 如果没有 Game 则需要创建一下
            if (mGame == null) {
                assert mParent!=null && mParent.mGame!=null;
                mGame = mParent.mGame.copy();
                mGame.applyChoice(mChoice);
            }
        }
        
        /** 从子节点中选择一个，在探索时会考虑访问次数；返回 null 表示没有子节点 */
        Node chooseChild(boolean aIsExplore) {
            // 如果没有子节点列表则临时创建
            if (mChildren == null) {
                validGame_();
                assert mGame != null;
                mChildren = new ArrayList<>();
                mGame.forEachChoice(choice -> mChildren.add(new Node(this, null, choice)));
                mChildrenNoVisit = new ArrayList<>(mChildren);
                // 随机打乱保证随机性，这样后续不再需要随机选取
                Collections.shuffle(mChildren, mRNG);
                Collections.shuffle(mChildrenNoVisit, mRNG);
            }
            if (mChildren.isEmpty()) {if (aIsExplore) {++mVisitTimes;} return null;}
            if (aIsExplore) {
                // 首先考虑返回 mChildrenNoVisit
                if (!mChildrenNoVisit.isEmpty()) {
                    Node tLast = UT.Code.removeLast(mChildrenNoVisit);
                    ++mVisitTimes;
                    ++tLast.mVisitTimes;
                    return tLast;
                }
                // 否则按照权重选取最大的
                Node tChoose = null;
                double tMaxScore = Double.NEGATIVE_INFINITY;
                for (Node tChild : mChildren) {
                    if (mVisitTimes==0 || tChild.mVisitTimes==0) throw new IllegalStateException();
                    double tScore = (tChild.mQuality / (double)tChild.mVisitTimes) + (MathEX.SQRT2_INV * MathEX.Fast.sqrt(2.0 * MathEX.Fast.log(mVisitTimes) / (double)tChild.mVisitTimes));
                    if (tScore > tMaxScore) {
                        tMaxScore = tScore;
                        tChoose = tChild;
                    }
                }
                assert tChoose != null;
                ++mVisitTimes;
                ++tChoose.mVisitTimes;
                return tChoose;
            } else {
                // 非 explore 下没有 visit 的认为分数为 0
                if (mVisitTimes == 0) {
                    return UT.Code.last(mChildren);
                }
                Node tChoose = null;
                double tMaxScore = Double.NEGATIVE_INFINITY;
                for (Node tChild : mChildren) {
                    double tScore = (tChild.mVisitTimes==0) ? 0.0 : (tChild.mQuality / (double)tChild.mVisitTimes);
                    if (tScore > tMaxScore) {
                        tMaxScore = tScore;
                        tChoose = tChild;
                    }
                }
                return tChoose;
            }
        }
        /** 根据 mGame 的分数来更新整条计数，注意只有根节点才能合法调用 */
        void checkScoreAndUpdate() {
            validGame_();
            assert mGame != null;
            double tScore = mGame.score();
            if (Double.isNaN(tScore)) throw new IllegalStateException("No score in this node");
            mQuality += tScore;
            Node tParent = mParent;
            while (tParent != null) {
                tScore = -tScore;
                tParent.mQuality += tScore;
                tParent = tParent.mParent;
            }
        }
    }
    
    private final Random mRNG;
    private Node mRoot;
    public MCTS(IGame<C> aGame, Random aRNG) {
        mRoot = new Node(null, aGame, null);
        mRNG = aRNG;
    }
    public MCTS(IGame<C> aGame) {
        this(aGame, RANDOM);
    }
    
    /**
     * 执行 MCTS 来统计权重，从而可以实现更好的选择
     * @param aMax 最大的探索次数，用来限制时间
     */
    public void simulate(long aMax) {
        long tN = 0;
        while (tN < aMax) {
            // 直接模拟到最后获取到分数并更新，这里只考虑这种情况
            Node oChild = mRoot;
            Node tChild = mRoot.chooseChild(true);
            ++tN;
            while (tChild != null) {
                oChild = tChild;
                tChild = tChild.chooseChild(true);
                ++tN;
            }
            oChild.checkScoreAndUpdate();
        }
    }
    
    /** 选择最大得分的子节点返回，并同时更新自身的 root 来应对下一步 */
    public C choose(boolean aUpdate) {
        Node tChoose = mRoot.chooseChild(false);
        if (aUpdate) {
            tChoose.setRoot(); // 剪枝
            mRoot = tChoose;
        }
        return tChoose.mChoice;
    }
    /** 默认情况下不使用旧的值，重新模拟一般会更加聪明一些 */
    public C choose() {return choose(false);}
    public void update(IGame<C> aGame) {
        mRoot = new Node(null, aGame, null);
    }
}
