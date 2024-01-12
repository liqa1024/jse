package jtool.io;

import jtool.code.UT;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * 支持使用 \r 来 refresh 的 {@link PrintStream}，
 * 主要用于进度条使用
 * @author liqa
 */
public class RefreshableFilePrintStream extends PrintStream {
    /**
     * @param  aFilePath
     *         The name of the file to use as the destination of this print
     *         stream.  If the file exists, then it will be truncated to
     *         zero size; otherwise, a new file will be created.  The output
     *         will be written to the file and is buffered.
     *
     * @throws  FileNotFoundException
     *          If the given file object does not denote an existing, writable
     *          regular file and a new regular file of that name cannot be
     *          created, or if some other error occurs while opening or
     *          creating the file
     *
     * @throws  SecurityException
     *          If a security manager is present and {@link
     *          SecurityManager#checkWrite checkWrite(fileName)} denies write
     *          access to the file
     *
     * @since  2.3.8
     */
    public RefreshableFilePrintStream(@NotNull String aFilePath) throws IOException {
        super(new RefreshableFileOutputStream(aFilePath));
    }
    public RefreshableFilePrintStream(@NotNull String aFilePath, String aEncoding) throws IOException {
        super(new RefreshableFileOutputStream(aFilePath), false, aEncoding);
    }
    
    // 直接重写所有 println 方法强制 LF 格式
    private void newLine() {
        try {
            synchronized (this) {
                if (out == null) throw new IOException("Stream closed");
                out.write('\n');
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            setError();
        }
    }
    @Override public void println() {
        newLine();
    }
    @Override public void println(boolean x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(char x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(int x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(long x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(float x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(double x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(char @NotNull[] x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(String x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
    @Override public void println(Object x) {
        String s = String.valueOf(x);
        synchronized (this) {
            print(s);
            newLine();
        }
    }
    
    
    static class RefreshableFileOutputStream extends OutputStream {
        
        private final RandomAccessFile mFile;
        private long mRefreshTo = 0;
        RefreshableFileOutputStream(@NotNull String aFilePath) throws IOException {
            mFile = new RandomAccessFile(UT.IO.toFile(aFilePath), "rw");
            mFile.setLength(0);
        }
        
        private byte[] mBytes = null;
        private byte[] mByte1 = null;
        @Override public synchronized void write(int aByte) throws IOException {
            if (mByte1 == null) mByte1 = new byte[1];
            mByte1[0] = (byte)aByte;
            write(mByte1, 0, 1);
        }
        @Override public synchronized void write(byte @NotNull[] aBytes, int aOff, int aLen) throws IOException {
            if (mBytes==null || mBytes.length<aLen) mBytes = new byte[aLen];
            // 先遍历 bytes 计算结果，然后一次性写入，可以减少文件的操作
            final long oFileLen = mFile.length();
            int tBytesRefreshTo = -1;
            int tLen = 0;
            final int tEnd = aOff + aLen;
            for (int i = aOff, j = 0; i < tEnd; ++i) {
                byte tByte = aBytes[i];
                if (tByte == '\r') {
                    if (tBytesRefreshTo < 0) {
                        mFile.seek(mRefreshTo);
                        j = 0;
                    } else {
                        j = tBytesRefreshTo;
                    }
                    continue;
                }
                if (tByte == '\n') {
                    j = tLen; // 换行需要增加一个 j 的移动，移动到末尾
                    tBytesRefreshTo = j + 1;
                    mRefreshTo = oFileLen + j + 1;
                }
                mBytes[j] = tByte;
                ++j;
                if (j > tLen) tLen = j;
            }
            mFile.write(mBytes, 0, tLen);
            mFile.seek(mFile.length());
        }
        
        @Override public void close() throws IOException {
            mFile.close();
        }
    }
}
