package jse.system;

import jse.code.IO;
import jse.code.UT;
import jse.code.collection.NewCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.*;

import static jse.code.CS.*;

/**
 * @author liqa
 * <p> SystemExecutor 的一般实现，直接在本地运行 </p>
 */
public class LocalSystemExecutor extends AbstractSystemExecutor {
    public LocalSystemExecutor() {super();}
    
    /** stuff to override */
    protected String @NotNull[] programAndArgs_() {return ZL_STR;}
    protected Charset charset_() {return Charset.defaultCharset();}
    
    private @Nullable File mWorkingDir = null;
    @Override public final LocalSystemExecutor setWorkingDir(@Nullable String aWorkingDir) {mWorkingDir = aWorkingDir==null ? null : IO.toFile(aWorkingDir); return this;}
    
    /** 本地则在本地创建目录即可 */
    @Override public final void validPath(String aPath) throws IOException {IO.validPath(aPath);}
    @Override public final void makeDir(String aDir) throws IOException {IO.makeDir(aDir);}
    @Override public final void removeDir(String aDir) throws IOException {IO.removeDir(aDir);}
    @Override public final void delete(String aPath) throws IOException {IO.delete(aPath);}
    @Override public final boolean isFile(String aFilePath) {return IO.isFile(aFilePath);}
    @Override public final boolean isDir(String aDir) {return IO.isDir(aDir);}
    @Override public final String @NotNull[] list(String aDir) throws IOException {return IO.list(aDir);}
    
    @Override protected Future<Integer> submitSystem__(String aCommand, @NotNull IO.IWriteln aWriteln) {
        // 对于空指令专门优化，不执行操作
        if (aCommand == null || aCommand.isEmpty()) return SUC_FUTURE;
        return new LocalSystemFuture(aCommand, aWriteln);
    }
    
    class LocalSystemFuture implements Future<Integer> {
        private final static int TRY_TIMES = 100;
        
        private final @Nullable Process mProcess;
        private final Future<Void> mErrTask, mOutTask;
        private volatile boolean mCancelled = false;
        LocalSystemFuture(String aCommand, final @NotNull IO.IWriteln aWriteln) {
            // 执行指令
            Process tProcess;
            try {
                // 这里对于一般情况改为更加稳定的 processBuilder
                String[] tProgramAndArgs = programAndArgs_();
                String[] tCommands = tProgramAndArgs.length==0 ? IO.Text.splitBlank(aCommand) : NewCollections.merge(tProgramAndArgs, aCommand);
                tProcess = new ProcessBuilder(tCommands).directory(mWorkingDir).start();
            } catch (Exception e) {
                printStackTrace(e); tProcess = null;
            }
            mProcess = tProcess;
            if (mProcess == null) {
                mErrTask = null;
                mOutTask = null;
                return;
            }
            // 使用另外两个线程读取错误流和输出流（由于内部会对输出自动 buffer，获取 stream 和执行的顺序不重要）
            mErrTask = UT.Par.redirectStream(mProcess.getErrorStream(), true, noERROutput() ? NUL_PRINT_STREAM : System.err);
            // 读取执行的输出，如果是标准输出则直接重定向
            if (aWriteln == STD_OUT_WRITELN) {
                mOutTask = UT.Par.redirectStream(mProcess.getInputStream(), true, noSTDOutput() ? NUL_PRINT_STREAM : System.out);
            } else {
                mOutTask = UT.Par.runAsync(() -> {
                    try (BufferedReader tOutReader = IO.toReader(mProcess.getInputStream(), charset_()); IO.IWriteln tWriteln = (noSTDOutput() ? NUL_WRITELN: aWriteln)) {
                        // 对于 Process，由于内部已经有 buffered 输出流，因此必须要获取输出流并遍历，避免发生流死锁
                        String tLine;
                        while ((tLine = tOutReader.readLine()) != null) tWriteln.writeln(tLine);
                    } catch (Exception e) {printStackTrace(e);}
                });
            }
        }
        
        public void doFinal() {/**/}
        @Override public boolean cancel(boolean mayInterruptIfRunning) {
            if (mProcess == null) return false;
            if (mayInterruptIfRunning && mProcess.isAlive()) {
                mProcess.destroy(); // anyway, 总之是没有办法在 java 中直接发送 ctrl-c 到指定进程的
                for (int i = 0; i < TRY_TIMES; ++i) {
                    if (!mProcess.isAlive()) {mCancelled = true; doFinal(); return true;}
                    try {Thread.sleep(SYNC_SLEEP_TIME);}
                    catch (InterruptedException e) {return false;}
                }
            }
            return false;
        }
        @Override public boolean isCancelled() {return mCancelled;}
        @Override public boolean isDone() {return mProcess==null || !mProcess.isAlive();}
        @Override public Integer get() throws InterruptedException, ExecutionException {
            if (mProcess == null) return -1;
            int tExitValue = mProcess.waitFor();
            mErrTask.get();
            mOutTask.get();
            if (mCancelled) throw new CancellationException();
            doFinal();
            return tExitValue;
        }
        @Override public Integer get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (mProcess == null) return -1;
            int tExitValue;
            long tic = System.nanoTime();
            long tRestTime = unit.toNanos(timeout);
            if (mProcess.waitFor(tRestTime, TimeUnit.NANOSECONDS)) {
                tExitValue = mProcess.exitValue();
                long toc = System.nanoTime();
                tRestTime -= toc - tic;
                tic = toc;
            } else {
                throw new TimeoutException();
            }
            if (tRestTime <= 0) throw new TimeoutException();
            mErrTask.get(tRestTime, TimeUnit.NANOSECONDS);
            tRestTime -= System.nanoTime() - tic;
            if (tRestTime <= 0) throw new TimeoutException();
            mOutTask.get(tRestTime, TimeUnit.NANOSECONDS);
            if (mCancelled) throw new CancellationException();
            doFinal();
            return tExitValue;
        }
    }
    
    
    /** 对于本地的不需要同步输入输出文件 */
    @Override protected final void putFiles_(Iterable<String> aFiles) {/**/}
    @Override protected final void getFiles_(Iterable<String> aFiles) {/**/}
}
