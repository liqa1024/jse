package jse.parallel;

/**
 * 内部包含可以关闭的类，接口主要用于规范化子类实现
 * @author liqa
 */
public interface IHasAutoShutdown extends IAutoShutdown {
    IHasAutoShutdown setDoNotShutdown(boolean aDoNotShutdown);
}
