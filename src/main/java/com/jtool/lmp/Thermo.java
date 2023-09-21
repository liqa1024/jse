package com.jtool.lmp;

import com.jtool.code.UT;
import com.jtool.code.collection.AbstractCollections;
import com.jtool.code.collection.NewCollections;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.RowMatrix;
import com.jtool.math.table.AbstractMultiFrameTable;
import com.jtool.math.table.ITable;
import com.jtool.math.table.Table;
import com.jtool.math.table.Tables;
import com.jtool.math.vector.IVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.jtool.code.CS.ZL_STR;


/**
 * @author liqa
 * <p> lammps 使用 thermo 写出到 log 的数据格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 本身是一个列表的 Table 方便外部使用 </p>
 */
public class Thermo extends AbstractMultiFrameTable<ITable> {
    private List<ITable> mTableList;
    
    Thermo(ITable... aTableList) {mTableList = NewCollections.from(aTableList);}
    Thermo(List<ITable> aTableList) {mTableList = aTableList;}
    
    /** AbstractMultiFrameTable stuffs */
    @Override public Thermo copy() {
        List<ITable> rThermo = new ArrayList<>();
        for (ITable tTable : mTableList) rThermo.add(tTable.copy());
        return new Thermo(rThermo);
    }
    
    /** AbstractList stuffs */
    @Override public int size() {return mTableList.size();}
    @Override public ITable get(int index) {return mTableList.get(index);}
    @Override public ITable set(int index, ITable aTable) {return mTableList.set(index, aTable);}
    @Override public boolean add(ITable aTable) {return mTableList.add(aTable);}
    @Override public ITable remove(int aIndex) {return mTableList.remove(aIndex);}
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    public Thermo append(ITable aTable) {
        mTableList.add(aTable);
        return this;
    }
    public Thermo appendList(Iterable<ITable> aTableList) {
        for (ITable tTable : aTableList) mTableList.add(tTable);
        return this;
    }
    public Thermo appendFile(String aFilePath) throws IOException {
        mTableList.addAll(read(aFilePath).mTableList);
        return this;
    }
    public Thermo appendCSV(String aPath) throws IOException {
        if (UT.IO.isDir(aPath)) {
            if (aPath.equals(".")) aPath = "";
            if (!aPath.isEmpty() && !aPath.endsWith("/") && !aPath.endsWith("\\")) aPath += "/";
            String[] tFiles = UT.IO.list(aPath);
            if (tFiles == null) return this;
            for (String tName : tFiles) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tFileOrDir = aPath+tName;
                if (UT.IO.isFile(tFileOrDir)) {
                    mTableList.add(UT.IO.csv2table(tFileOrDir));
                }
            }
        } else {
            mTableList.add(UT.IO.csv2table(aPath));
        }
        return this;
    }
    
    /** 合并内部的多个表格，不做返回表明这个方法直接修改自身而不是创建新对象 */
    public void merge() {
        List<ITable> oTableList = mTableList;
        mTableList = new ArrayList<>();
        List<String> rHeads = null;
        List<IVector> rTable = null;
        for (ITable tTable : oTableList) {
            if (rHeads == null) {
                rHeads = tTable.heads();
                rTable = new ArrayList<>(tTable.rows());
            } else {
                // 直接遍历比较是否相同，目前必须顺序完全相同才可以（后排可以更宽但是多余数据会抹除）
                final Iterator<String> li = rHeads.iterator();
                final Iterator<String> ri = tTable.heads().iterator();
                boolean rEqual = true;
                while (li.hasNext() && ri.hasNext()) {
                    if (!li.next().equals(ri.next())) {rEqual = false; break;}
                }
                if (rEqual) {
                    rTable.addAll(tTable.rows());
                } else {
                    // 否则则保存合并的结果，并开始下一步
                    mTableList.add(Tables.fromRows(AbstractCollections.map(rTable, IVector::asList), rHeads.toArray(ZL_STR)));
                    rHeads = tTable.heads();
                    rTable = new ArrayList<>(tTable.rows());
                }
            }
        }
        // 最后保存最后一步的结果
        if (rHeads != null) {
            mTableList.add(Tables.fromRows(AbstractCollections.map(rTable, IVector::asList), rHeads.toArray(ZL_STR)));
        }
    }
    
    
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
            
            List<ITable> rThermo = new ArrayList<>();
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
     * 直接从 {@link ITable} 来创建
     * @author liqa
     */
    public static Thermo fromTable(ITable aTable) {
        return new Thermo(aTable);
    }
    public static Thermo fromTableList(Iterable<? extends ITable> aTableList) {
        return new Thermo(NewCollections.from(aTableList));
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
    public static Thermo read_(List<String> aLines) {
        List<ITable> rThermo = new ArrayList<>();
        
        int idx = 0, endIdx;
        String[] tTokens;
        while (idx < aLines.size()) {
            // 跳转到 "Per MPI rank memory allocation" 后面的 "Step" 行，也就是需要 thermo 中包含 step 项
            idx = UT.Texts.findLineContaining(aLines, idx, "Per MPI rank memory allocation");
            idx = UT.Texts.findLineContaining(aLines, idx, "Step", true);
            if (idx >= aLines.size()) break;
            // 获取种类的 key
            tTokens = UT.Texts.splitBlank(aLines.get(idx));
            String[] aHands = tTokens;
            ++idx;
            // 获取结束的位置
            endIdx = UT.Texts.findLineContaining(aLines, idx, "Loop time of");
            if (endIdx >= aLines.size()) break;
            IMatrix aData = RowMatrix.zeros(endIdx-idx, aHands.length);
            // 读取数据
            for (IVector tRow : aData.rows()) {
                tRow.fill(UT.Texts.str2data(aLines.get(idx), aHands.length));
                ++idx;
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
        if (mTableList.size() > 1) {
            if (aFilePath.endsWith(".csv")) aFilePath = aFilePath.substring(0, aFilePath.length()-4);
            for (int i = 0; i < mTableList.size(); ++i) {
                UT.IO.table2csv(mTableList.get(i), aFilePath+"/thermo-"+i+".csv");
            }
            if (!aNoOutput) System.out.println("Thermos have been saved to the directory: "+aFilePath);
        } else
        if (!mTableList.isEmpty()) {
            UT.IO.table2csv(mTableList.get(0), aFilePath);
        }
    }
}
