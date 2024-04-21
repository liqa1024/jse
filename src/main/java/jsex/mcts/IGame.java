package jsex.mcts;

import java.util.function.Consumer;

/**
 * 用于输入的抽象对局接口
 * @author liqa
 */
public interface IGame<C> {
    /** 现在改为 for-each 写法，实现起来更加方便 */
    void forEachChoice(Consumer<C> aCon);
    /** 应用修改 */
    void applyChoice(C aChoice);
    /** NaN 表示还不存在分数（还未结束）*/
    double score();
    
    /** 复制一份新的 Game 用于模拟 */
    IGame<C> copy();
}
