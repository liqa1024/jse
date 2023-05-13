package com.guan.parallel;

import com.guan.code.UT;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;


/**
 * @author liqa
 * <p> 用来并行执行任意 java 方法的线程池 </p>
 * <p> 与一般的 java 线程池不同，为了方便外部调用，
 * 这里直接接受 String 的方法名称，使用反射来调用这个方法 </p>
 */
public class MethodThreadPool extends AbstractHasThreadPool<IExecutorEX> {
    public MethodThreadPool(int aThreadNum) {super(newPool(aThreadNum));}
    
    // 提交任务
    public Future<Object> submit(@NotNull Object aInstance, String aMethodName, Object... aArgs) {try {return pool().submit(UT.Hack.getTaskCallOfMethod(aInstance, aMethodName, aArgs));} catch (NoSuchMethodException e) {e.printStackTrace(); return null;}}
    public Future<Object> submitStatic(String aClassName, String aMethodName, Object... aArgs) {try {return pool().submit(UT.Hack.getTaskCallOfStaticMethod(aClassName, aMethodName, aArgs));} catch (NoSuchMethodException e) {e.printStackTrace(); return null;}}
}
