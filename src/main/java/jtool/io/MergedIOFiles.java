package jtool.io;


import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static jtool.code.CS.IFILE_KEY;
import static jtool.code.CS.OFILE_KEY;

/**
 * @author liqa
 * <p> 已经合并的 IIOFiles 的实现，只有两个 key: {@code "<o>", "<i>"}，排除相同的文件 </p>
 */
public class MergedIOFiles implements IIOFiles {
    /** 提供额外的合并 IOFiles 的接口 */
    public void merge(IIOFiles aIOFiles) {
        for (String tIFile : aIOFiles.getIFiles()) mIFiles.add(tIFile);
        for (String tOFile : aIOFiles.getOFiles()) mOFiles.add(tOFile);
    }
    
    /** 全部遍历一次保证一定会值拷贝，String 也不会被修改因此不用考虑进一步值拷贝 */
    @Override public final MergedIOFiles copy() {
        MergedIOFiles rIOFiles = new MergedIOFiles();
        rIOFiles.mIFiles.addAll(mIFiles);
        rIOFiles.mOFiles.addAll(mOFiles);
        return rIOFiles;
    }
    
    
    private final Set<String> mIFiles;
    private final Set<String> mOFiles;
    
    public MergedIOFiles() {
        mIFiles = new LinkedHashSet<>();
        mOFiles = new LinkedHashSet<>();
    }
    
    
    @Override public Collection<String> getIFiles(String aIFileKey) {return aIFileKey.equals(IFILE_KEY) ? mIFiles : null;}
    @Override public Collection<String> getOFiles(String aOFileKey) {return aOFileKey.equals(OFILE_KEY) ? mOFiles : null;}
    @Override public Iterable<String> getIFiles() {return mIFiles;}
    @Override public Iterable<String> getOFiles() {return mOFiles;}
    @Override public Iterable<String> getIFileKeys() {return Collections.singletonList(IFILE_KEY);}
    @Override public Iterable<String> getOFileKeys() {return Collections.singletonList(OFILE_KEY);}
    
    
    
    @Override public final MergedIOFiles putIFiles(String aIFileKey, String aIFilePath                      ) {mIFiles.add(aIFilePath); return this;}
    @Override public final MergedIOFiles putIFiles(String aIFileKey, String aIFilePath, int aMultiple       ) {for (int i = 0; i < aMultiple; ++i) mIFiles.add(aIFilePath+"-"+i); return this;}
    @Override public final MergedIOFiles putIFiles(String aIFileKey, String aIFilePath, int aStart, int aEnd) {for (int i = aStart; i < aEnd; ++i) mIFiles.add(aIFilePath+"-"+i); return this;}
    @Override public final MergedIOFiles putOFiles(String aOFileKey, String aOFilePath                      ) {mOFiles.add(aOFilePath); return this;}
    @Override public final MergedIOFiles putOFiles(String aOFileKey, String aOFilePath, int aMultiple       ) {for (int i = 0; i < aMultiple; ++i) mOFiles.add(aOFilePath+"-"+i); return this;}
    @Override public final MergedIOFiles putOFiles(String aOFileKey, String aOFilePath, int aStart, int aEnd) {for (int i = aStart; i < aEnd; ++i) mOFiles.add(aOFilePath+"-"+i); return this;}
    
    
    @VisibleForTesting @Override public final MergedIOFiles i(String aIFileKey, String aIFilePath                      ) {return putIFiles(aIFileKey, aIFilePath              );}
    @VisibleForTesting @Override public final MergedIOFiles i(String aIFileKey, String aIFilePath, int aMultiple       ) {return putIFiles(aIFileKey, aIFilePath, aMultiple   );}
    @VisibleForTesting @Override public final MergedIOFiles i(String aIFileKey, String aIFilePath, int aStart, int aEnd) {return putIFiles(aIFileKey, aIFilePath, aStart, aEnd);}
    @VisibleForTesting @Override public final MergedIOFiles o(String aOFileKey, String aOFilePath                      ) {return putOFiles(aOFileKey, aOFilePath              );}
    @VisibleForTesting @Override public final MergedIOFiles o(String aOFileKey, String aOFilePath, int aMultiple       ) {return putOFiles(aOFileKey, aOFilePath, aMultiple   );}
    @VisibleForTesting @Override public final MergedIOFiles o(String aOFileKey, String aOFilePath, int aStart, int aEnd) {return putOFiles(aOFileKey, aOFilePath, aStart, aEnd);}
}
