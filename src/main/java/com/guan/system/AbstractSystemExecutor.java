package com.guan.system;

import com.guan.code.IHasIOFiles;
import com.guan.code.UT;
import com.guan.parallel.AbstractHasThreadPool;
import com.guan.parallel.IExecutorEX;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public abstract class AbstractSystemExecutor extends AbstractHasThreadPool<IExecutorEX> implements ISystemExecutor {
    protected AbstractSystemExecutor(int aThreadNum) {super(newPool(aThreadNum));}
    
    
    @Override public int system_NO(String aCommand                                           ) {return system(aCommand, (PrintStream)null);} // No Output
    @Override public int system   (String aCommand                                           ) {return system(aCommand, System.out);}
    @Override public int system   (String aCommand, String aOutFilePath                      ) throws IOException {try (PrintStream tFilePS = UT.IO.toPrintStream(aOutFilePath)) {return system(aCommand, tFilePS);}}
    @Override public int system_NO(String aCommand                     , IHasIOFiles aIOFiles) {return system(aCommand, (PrintStream)null, aIOFiles);} // No Output
    @Override public int system   (String aCommand                     , IHasIOFiles aIOFiles) {return system(aCommand, System.out, aIOFiles);}
    @Override public int system   (String aCommand, String aOutFilePath, IHasIOFiles aIOFiles) throws IOException {try (PrintStream tFilePS = UT.IO.toPrintStream(aOutFilePath)) {return system(aCommand, tFilePS, aIOFiles);}}
    
    
    @Override public Future<Integer> submitSystem_NO(final String aCommand                                                                         ) {return pool().submit(() -> system_NO(aCommand));}
    @Override public Future<Integer> submitSystem   (final String aCommand                                                                         ) {return pool().submit(() -> system   (aCommand));}
    @Override public Future<Integer> submitSystem   (final String aCommand, final String aOutFilePath                                              ) {return pool().submit(() -> system   (aCommand, aOutFilePath));}
    @Override public Future<Integer> submitSystem   (final String aCommand, final @Nullable PrintStream aOutPrintStream                            ) {return pool().submit(() -> system   (aCommand, aOutPrintStream));}
    @Override public Future<Integer> submitSystem_NO(final String aCommand                                             , final IHasIOFiles aIOFiles) {return pool().submit(() -> system_NO(aCommand, aIOFiles));}
    @Override public Future<Integer> submitSystem   (final String aCommand                                             , final IHasIOFiles aIOFiles) {return pool().submit(() -> system   (aCommand, aIOFiles));}
    @Override public Future<Integer> submitSystem   (final String aCommand, final String      aOutFilePath             , final IHasIOFiles aIOFiles) {return pool().submit(() -> system   (aCommand, aOutFilePath, aIOFiles));}
    @Override public Future<Integer> submitSystem   (final String aCommand, final @Nullable PrintStream aOutPrintStream, final IHasIOFiles aIOFiles) {return pool().submit(() -> system   (aCommand, aOutPrintStream, aIOFiles));}
    
    @Override public List<String> system_str(String aCommand) {final List<String> rList = new ArrayList<>(); system(aCommand, toListPS(rList)); return rList;}
    @Override public List<String> system_str(String aCommand, IHasIOFiles aIOFiles) {final List<String> rList = new ArrayList<>(); system(aCommand, toListPS(rList), aIOFiles); return rList;}
    @Override public Future<List<String>> submitSystem_str(final String aCommand) {return pool().submit(() -> system_str(aCommand));}
    @Override public Future<List<String>> submitSystem_str(final String aCommand, IHasIOFiles aIOFiles) {return pool().submit(() -> system_str(aCommand, aIOFiles));}
    
    /** only support println */
    private static PrintStream toListPS(final List<String> rList) {return new PrintStream(NL_OUT) {@Override public void println(@Nullable String s) {rList.add(s);}};}
    private final static OutputStream NL_OUT = new OutputStream() {@Override public void write(int b) {/**/}};
    
    
    
    /** stuff to override */
    public abstract int system(String aCommand, @Nullable PrintStream aOutPrintStream);
    public abstract int system(String aCommand, @Nullable PrintStream aOutPrintStream, IHasIOFiles aIOFiles);
}
