package com.guan.system;

import com.guan.code.UT;
import com.guan.io.IHasIOFiles;
import com.guan.io.MergedIOFiles;
import com.guan.parallel.AbstractHasThreadPool;
import com.guan.parallel.IExecutorEX;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Supplier;


/**
 * @author liqa
 * <p> 将一般实现放入抽象类中，因为 submit 一定需要在 pool 中使用，如果直接嵌套文件的输入流会在写入前就关闭，默认输出在 System.out </p>
 */
public abstract class AbstractThreadPoolSystemExecutor extends AbstractHasThreadPool<IExecutorEX> implements ISystemExecutor {
    protected AbstractThreadPoolSystemExecutor(IExecutorEX aPool) {super(aPool);}
    
    private boolean mNoConsoleOutput = false;
    @Override public final ISystemExecutor setNoConsoleOutput(boolean aNoConsoleOutput) {mNoConsoleOutput = aNoConsoleOutput; return this;}
    @Override public final boolean noConsoleOutput() {return mNoConsoleOutput;}
    
    
    @Override public final int system(String aCommand                                                 ) {return system_(aCommand, this::outPrintln);}
    @Override public final int system(String aCommand, final String aOutFilePath                      ) {return system_(aCommand, () -> filePrintln(aOutFilePath));}
    @Override public final int system(String aCommand                           , IHasIOFiles aIOFiles) {return system_(aCommand, this::outPrintln, aIOFiles);}
    @Override public final int system(String aCommand, final String aOutFilePath, IHasIOFiles aIOFiles) {return system_(aCommand, () -> filePrintln(aOutFilePath), aIOFiles);}
    @Override public final List<String> system_str(String aCommand                      ) {final List<String> rList = new ArrayList<>(); system_(aCommand, () -> listPrintln(rList)          ); return rList;}
    @Override public final List<String> system_str(String aCommand, IHasIOFiles aIOFiles) {final List<String> rList = new ArrayList<>(); system_(aCommand, () -> listPrintln(rList), aIOFiles); return rList;}
    
    
    @Override public final Future<Integer> submitSystem(String aCommand                                                 ) {return submitSystem_(aCommand, this::outPrintln);}
    @Override public final Future<Integer> submitSystem(String aCommand, final String aOutFilePath                      ) {return submitSystem_(aCommand, () -> filePrintln(aOutFilePath));}
    @Override public final Future<Integer> submitSystem(String aCommand                           , IHasIOFiles aIOFiles) {return submitSystem_(aCommand, this::outPrintln, aIOFiles);}
    @Override public final Future<Integer> submitSystem(String aCommand, final String aOutFilePath, IHasIOFiles aIOFiles) {return submitSystem_(aCommand, () -> filePrintln(aOutFilePath), aIOFiles);}
    
    @Override public final Future<List<String>> submitSystem_str(String aCommand) {
        final List<String> rList = new ArrayList<>();
        final Future<Integer> tSystemTask = submitSystem_(aCommand, () -> listPrintln(rList));
        return UT.Code.map(tSystemTask, v -> rList);
    }
    @Override public final Future<List<String>> submitSystem_str(String aCommand, IHasIOFiles aIOFiles) {
        final List<String> rList = new ArrayList<>();
        final Future<Integer> tSystemTask = submitSystem_(aCommand, () -> listPrintln(rList), aIOFiles);
        return UT.Code.map(tSystemTask, v -> rList);
    }
    
    /** 批量任务直接遍历提交，使用 UT.Code.mergeAll 来管理 Future */
    private List<String> mBatchCommands = new ArrayList<>();
    private MergedIOFiles mBatchIOFiles = new MergedIOFiles();
    @Override public final Future<List<Integer>> getSubmit() {
        Future<List<Integer>> tFuture = batchSubmit_(mBatchCommands, mBatchIOFiles);
        mBatchCommands = new ArrayList<>();
        mBatchIOFiles = new MergedIOFiles();
        return tFuture;
    }
    @Override public final void putSubmit(String aCommand) {
        mBatchCommands.add(aCommand);
    }
    @Override public final void putSubmit(String aCommand, IHasIOFiles aIOFiles) {
        mBatchCommands.add(aCommand);
        mBatchIOFiles.merge(aIOFiles);
    }
    protected final Future<List<Integer>> batchSubmit_(Iterable<String> aCommands) {
        List<Future<Integer>> rFutures = new ArrayList<>();
        for (String tCommand : aCommands) rFutures.add(submitSystem(tCommand));
        return UT.Code.mergeAll(rFutures);
    }
    
    
    /** only support println */
    protected interface IPrintln extends AutoCloseable {void println(String aLine); void close();}
    /** submit 相关需要使用 supplier，只在需要输入的时候进行创建 */
    @FunctionalInterface protected interface IPrintlnSupplier extends Supplier<IPrintln> {@NotNull IPrintln get();}
    /** 不使用静态方法方便子类重写 */
    protected IPrintln outPrintln() {
        return new IPrintln() {
            @Override public void println(String aLine) {System.out.println(aLine);}
            @Override public void close() {/**/}
        };
    }
    @SuppressWarnings("resource")
    protected IPrintln filePrintln(String aFilePath) {
        final PrintStream tFilePS;
        try {
           tFilePS = UT.IO.toPrintStream(aFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new IPrintln() {
            @Override public void println(String aLine) {tFilePS.println(aLine);}
            @Override public void close() {tFilePS.close();}
        };
    }
    protected IPrintln listPrintln(final List<String> aList) {
        return new IPrintln() {
            @Override public void println(String aLine) {aList.add(aLine);}
            @Override public void close() {/**/}
        };
    }
    
    
    /** stuff to override */
    protected abstract int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln);
    protected abstract int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles);
    protected abstract Future<Integer> submitSystem_(String aCommand, @NotNull IPrintlnSupplier aPrintln);
    protected abstract Future<Integer> submitSystem_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles);
    protected abstract Future<List<Integer>> batchSubmit_(Iterable<String> aCommands, IHasIOFiles aIOFiles);
}
