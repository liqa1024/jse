package jse.parallel;

import jse.code.UT;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;


/**
 * @author liqa
 * <p> 用来并行执行任意 java 方法的线程池 </p>
 * <p> 与一般的 java 线程池不同，为了方便外部调用，
 * 这里直接接受 String 的方法名称，使用反射来调用这个方法 </p>
 */
public class MethodThreadPool extends AbstractThreadPool<IExecutorEX> {
    public MethodThreadPool(int aThreadNum) {super(newPool(aThreadNum));}
    
    // 提交任务
    public Future<Object> submit(@NotNull Object aInstance, String aMethodName, Object... aArgs) {try {return pool().submit(UT.Hack.getCallableOfMethod(aInstance, aMethodName, aArgs));} catch (NoSuchMethodException e) {throw new RuntimeException(e);}}
    public Future<Object> submitStatic(String aClassName, String aMethodName, Object... aArgs) {try {return pool().submit(UT.Hack.getCallableOfStaticMethod(aClassName, aMethodName, aArgs));} catch (NoSuchMethodException | ClassNotFoundException e) {throw new RuntimeException(e);}}
}
