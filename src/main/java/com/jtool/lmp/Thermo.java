package com.jtool.lmp;

import com.jtool.code.UT;
import com.jtool.math.table.AbstractMultiFrameTable;
import com.jtool.math.table.Table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author liqa
 * <p> lammps 使用 thermo 写出到 log 的数据格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 本身是一个列表的 Table 方便外部使用 </p>
 */
public class Thermo extends AbstractMultiFrameTable<Table> {
    private final Table[] mTables;
    
    public Thermo(Table... aTables) {mTables = aTables==null ? new Table[0] : aTables;}
    public Thermo(Collection<Table> aTables) {mTables = aTables.toArray(new Table[0]);}
    
    /** AbstractList stuffs */
    @Override public Table get(int index) {return mTables[index];}
    @Override public int size() {return mTables.length;}
    
    
    
    /**
     * 从 csv 文件中读取 Thermo，自动根据输入路径选择，如果是文件夹则会读取多个 Thermo
     * @author liqa
     * @param aPath csv 文件路径或者文件夹名称
     * @return 读取得到的 Thermo 对象
     * @throws IOException 如果读取失败
     */
    public static Thermo fromCSV(String aPath) throws IOException {
        if (UT.IO.isDir(aPath)) {
            if (aPath.equals(".")) aPath = "";
            if (!aPath.isEmpty() && !aPath.endsWith("/") && !aPath.endsWith("\\")) aPath += "/";
            String[] tFiles = UT.IO.list(aPath);
            if (tFiles == null) return null;
            
            List<Table> rThermo = new ArrayList<>();
            for (String tName : tFiles) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tFileOrDir = aPath+tName;
                if (UT.IO.isFile(tFileOrDir)) {
                    rThermo.add(UT.IO.csv2table(tFileOrDir));
                }
            }
            return new Thermo(rThermo);
        } else {
            return new Thermo(UT.IO.csv2table(aPath));
        }
    }
    
    
    /**
     * 从文件 lammps 输出的 log 文件中读取来实现初始化，
     * 对于 log 只实现其的读取，而输出则会使用通用方法输出成 csv 文件
     * @author liqa
     * @param aFilePath lammps 输出的 log 文件路径
     * @return 读取得到的 Thermo 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     */
    public static Thermo read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    public static Thermo read_(String[] aLines) {
        List<Table> rThermo = new ArrayList<>();
        
        int idx = 0, endIdx;
        String[] tTokens;
        while (idx < aLines.length) {
            // 跳转到 "Per MPI rank memory allocation" 后面的 "Step" 行，也就是需要 thermo 中包含 step 项
            idx = UT.Texts.findLineContaining(aLines, idx, "Per MPI rank memory allocation");
            idx = UT.Texts.findLineContaining(aLines, idx, "Step");
            if (idx >= aLines.length) break;
            // 获取种类的 key
            tTokens = UT.Texts.splitBlank(aLines[idx]);
            String[] aHands = tTokens;
            ++idx;
            // 获取结束的位置
            endIdx = UT.Texts.findLineContaining(aLines, idx, "Loop time of");
            if (endIdx >= aLines.length) break;
            List<double[]> aData = new ArrayList<>(endIdx - idx);
            // 读取数据
            for (; idx < endIdx; ++idx) {
                tTokens = UT.Texts.splitBlank(aLines[idx]);
                aData.add(UT.IO.str2data(tTokens));
            }
            // 创建 Table 并附加到 rThermo 中
            rThermo.add(new Table(aHands, aData));
        }
        return new Thermo(rThermo);
    }
    
    /**
     * 将 Thermo 写入到 csv 文件，如果有多个会创建文件夹，否则直接写入文件
     * @author liqa
     * @param aFilePath 需要输出的路径或者文件夹名称
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {write(aFilePath, false);}
    public void write(String aFilePath, boolean aNoOutput) throws IOException {
        // 超过一个则写入文件夹，会自动分析路径名称创建合适的文件夹名称
        if (mTables.length > 1) {
            if (aFilePath.endsWith(".csv")) aFilePath = aFilePath.substring(0, aFilePath.length()-4);
            for (int i = 0; i < mTables.length; ++i) {
                UT.IO.table2csv(mTables[i], aFilePath+"/thermo-"+i+".csv");
            }
            if (!aNoOutput) System.out.println("Thermos have been saved to the directory: "+aFilePath);
        } else
        if (mTables.length > 0) {
            UT.IO.table2csv(mTables[0], aFilePath);
        }
    }
}
