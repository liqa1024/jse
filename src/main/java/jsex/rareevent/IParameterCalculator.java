package jsex.rareevent;


import jse.atom.IAtomData;

/**
 * 基于参数分割的稀有事件采样方法所采样的对象，需要能够从给定的点计算出对应的参数 λ，
 * 内部会借助这个参数 λ 将所有的相空间进行分割，主动选取需要的方向的 λ 来继续采样
 * <p>
 * 要求这些方法是线程安全的，可以同一个实例并行运行同一个方法
 * @author liqa
 * @param <T> 点的类型，对于 lammps 模拟则是原子结构信息 {@link IAtomData}
 */
public interface IParameterCalculator<T> {
    double lambdaOf(T aPoint);
}
