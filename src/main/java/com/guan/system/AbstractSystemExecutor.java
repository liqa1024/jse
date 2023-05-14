package com.guan.system;

import com.guan.io.IHasIOFiles;
import com.guan.code.UT;
import com.guan.parallel.AbstractHasThreadPool;
import com.guan.parallel.IExecutorEX;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


/**
 * @author liqa
 * <p> 将一般实现放入抽象类中，因为 submit 一定需要在 pool 中使用，如果直接嵌套文件的输入流会在写入前就关闭，默认输出在 System.out </p>
 */
public abstract class AbstractSystemExecutor extends AbstractHasThreadPool<IExecutorEX> implements ISystemExecutor {
    protected AbstractSystemExecutor(int aThreadNum) {super(newPool(aThreadNum));}
    
    
    @Override public int system_NO(String aCommand                                           ) {return system(aCommand, (IPrintln)null);} // No Output
    @Override public int system   (String aCommand                                           ) {return system(aCommand, System.out::println);}
    @Override public int system   (String aCommand, String aOutFilePath                      ) throws IOException {try (final PrintStream tFilePS = UT.IO.toPrintStream(aOutFilePath)) {return system(aCommand, tFilePS::println);}}
    @Override public int system_NO(String aCommand                     , IHasIOFiles aIOFiles) {return system(aCommand, (IPrintln)null, aIOFiles);} // No Output
    @Override public int system   (String aCommand                     , IHasIOFiles aIOFiles) {return system(aCommand, System.out::println, aIOFiles);}
    @Override public int system   (String aCommand, String aOutFilePath, IHasIOFiles aIOFiles) throws IOException {try (final PrintStream tFilePS = UT.IO.toPrintStream(aOutFilePath)) {return system(aCommand, tFilePS::println, aIOFiles);}}
    @Override public List<String> system_str(String aCommand) {final List<String> rList = new ArrayList<>(); system(aCommand, rList::add); return rList;}
    @Override public List<String> system_str(String aCommand, IHasIOFiles aIOFiles) {final List<String> rList = new ArrayList<>(); system(aCommand, rList::add, aIOFiles); return rList;}
    
    
    @Override public Future<Integer> submitSystem_NO(final String aCommand                                                        ) {return pool().submit(() -> system_NO(aCommand));}
    @Override public Future<Integer> submitSystem   (final String aCommand                                                        ) {return pool().submit(() -> system   (aCommand));}
    @Override public Future<Integer> submitSystem   (final String aCommand, final String aOutFilePath                             ) {return pool().submit(() -> system   (aCommand, aOutFilePath));}
    @Override public Future<List<String>> submitSystem_str(final String aCommand) {return pool().submit(() -> system_str(aCommand));}
    
    
    /** only support println */
    @FunctionalInterface protected interface IPrintln {void println(String aLine);}
    
    
    /** stuff to override */
    public abstract int system(String aCommand, @Nullable IPrintln aPrintln);
    public abstract int system(String aCommand, @Nullable IPrintln aPrintln, IHasIOFiles aIOFiles);
}
