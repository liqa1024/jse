package jse.system;

import jse.code.UT;
import jse.code.collection.NewCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    protected Charset charset_() {return StandardCharsets.UTF_8;}
    
    /** 本地则在本地创建目录即可 */
    @Override public final void validPath(String aPath) throws IOException {UT.IO.validPath(aPath);}
    @Override public final void makeDir(String aDir) throws IOException {UT.IO.makeDir(aDir);}
    @Override public final void removeDir(String aDir) throws IOException {UT.IO.removeDir(aDir);}
    @Override public final void delete(String aPath) throws IOException {UT.IO.delete(aPath);}
    @Override public final boolean isFile(String aFilePath) {return UT.IO.isFile(aFilePath);}
    @Override public final boolean isDir(String aDir) {return UT.IO.isDir(aDir);}
    
    @Override protected Future<Integer> submitSystem__(String aCommand, @NotNull AbstractSystemExecutor.IWritelnSupplier aWriteln) {
        // 对于空指令专门优化，不执行操作
        if (aCommand == null || aCommand.isEmpty()) return SUC_FUTURE;
        
        return new LocalSystemFuture(aCommand, aWriteln);
    }
    
    private final class LocalSystemFuture implements Future<Integer> {
        private final static int TRY_TIMES = 100;
        
        private final @Nullable Process mProcess;
        private final Future<Void> mErrTask, mOutTask;
        private volatile boolean mCancelled = false;
        private LocalSystemFuture(String aCommand, final @NotNull AbstractSystemExecutor.IWritelnSupplier aWriteln) {
            // 执行指令
            Process tProcess;
            try {
                // 这里对于一般情况改为更加稳定的 processBuilder
                String[] tProgramAndArgs = programAndArgs_();
                String[] tCommands = tProgramAndArgs.length==0 ? UT.Texts.splitBlank(aCommand) : NewCollections.merge(tProgramAndArgs, aCommand);
                tProcess = new ProcessBuilder(tCommands).start();
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
            mErrTask = UT.Par.runAsync(() -> {
                boolean tERROutPut = !noERROutput();
                try (BufferedReader tErrReader = UT.IO.toReader(mProcess.getErrorStream(), charset_())) {
                    // 对于 Process，由于内部已经有 buffered 输出流，因此必须要获取输出流并遍历，避免发生流死锁
                    String tLine;
                    while ((tLine = tErrReader.readLine()) != null) {
                        if (tERROutPut) System.err.println(tLine);
                    }
                } catch (Exception e) {
                    printStackTrace(e);
                }
            });
            // 读取执行的输出
            mOutTask = UT.Par.runAsync(() -> {
                boolean tSTDOutPut = !noSTDOutput();
                try (BufferedReader tOutReader = UT.IO.toReader(mProcess.getInputStream(), charset_()); UT.IO.IWriteln tWriteln = tSTDOutPut ? aWriteln.get() : NUL_PRINTLN) {
                    // 对于 Process，由于内部已经有 buffered 输出流，因此必须要获取输出流并遍历，避免发生流死锁
                    String tLine;
                    while ((tLine = tOutReader.readLine()) != null) {
                        if (tSTDOutPut) tWriteln.writeln(tLine);
                    }
                } catch (Exception e) {
                    printStackTrace(e);
                }
            });
        }
        
        @Override public boolean cancel(boolean mayInterruptIfRunning) {
            if (mProcess == null) return false;
            if (mayInterruptIfRunning && mProcess.isAlive()) {
                mProcess.destroyForcibly();
                for (int i = 0; i < TRY_TIMES; ++i) {
                    if (!mProcess.isAlive()) {mCancelled = true; return true;}
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
            return tExitValue;
        }
    }
    
    
    /** 对于本地的不需要同步输入输出文件 */
    public final void putFiles(Iterable<String> aFiles) {/**/}
    public final void getFiles(Iterable<String> aFiles) {/**/}
    public final boolean needSyncIOFiles() {return false;}
}
