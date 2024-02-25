package jse.lmp;

import jse.code.UT;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.NewCollections;
import jse.math.table.ITable;
import jse.math.table.Tables;
import jse.math.vector.IVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static jse.code.CS.ZL_STR;

/**
 * @author liqa
 * <p> lammps 使用 thermo 写出到 log 的数据格式 </p>
 * <p> 最为一般情况下会包含多个 Thermo 信息，因此需要此类 </p>
 * <p> 现在不再继承 {@link List}，因为 List 的接口太脏了 </p>
 */
public class MultiThermo extends AbstractListWrapper<ITable, ITable, ITable> {
    
    MultiThermo(ITable... aTableList) {super(NewCollections.from(aTableList));}
    MultiThermo(List<ITable> aTableList) {super(aTableList);}
    
    /** AbstractListWrapper stuffs */
    @Override protected final ITable toInternal_(ITable aTable) {return aTable.copy();}
    @Override protected final ITable toOutput_(ITable aTable) {return aTable;}
    
    
    public MultiThermo copy() {
        List<ITable> rThermo = new ArrayList<>();
        for (ITable tTable : mList) rThermo.add(tTable.copy());
        return new MultiThermo(rThermo);
    }
    
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    public MultiThermo append(ITable aTable) {
        mList.add(aTable.copy());
        return this;
    }
    public MultiThermo appendList(Iterable<? extends ITable> aTableList) {
        for (ITable tTable : aTableList) mList.add(tTable.copy());
        return this;
    }
    public MultiThermo appendFile(String aFilePath) throws IOException {
        mList.addAll(read(aFilePath).mList);
        return this;
    }
    public MultiThermo appendCSV(String aPath) throws IOException {
        mList.addAll(readCSV(aPath).mList);
        return this;
    }
    
    private static boolean headMatch(List<String> aSrcHeads, List<String> aNextHeads) {
        if (aNextHeads.size() < aSrcHeads.size()) return false;
        for (int i = 0; i < aSrcHeads.size(); ++i) {
            if (!aSrcHeads.get(i).equals(aNextHeads.get(i))) return false;
        }
        return true;
    }
    
    /** 合并多个表格到单个 {@link Thermo} */
    public Thermo merge() {
        List<String> aHeads = null;
        List<IVector> rDataRows = new ArrayList<>();
        for (ITable tTable : mList) {
            if (aHeads == null) {
                aHeads = tTable.heads();
            } else {
                // 如果不匹配则终止合并
                if (headMatch(aHeads, tTable.heads())) break;
            }
            rDataRows.addAll(tTable.rows());
        }
        if (aHeads == null) return null;
        return new Thermo(Tables.fromRows(rDataRows, aHeads.toArray(ZL_STR)));
    }
    
    
    /**
     * 直接从 {@link ITable} 来创建
     * @author liqa
     */
    public static MultiThermo fromTable(ITable aTable) {
        return new MultiThermo(aTable.copy());
    }
    public static MultiThermo fromTableList(Iterable<? extends ITable> aTableList) {
        List<ITable> rThermo = new ArrayList<>();
        for (ITable tTable : aTableList) rThermo.add(tTable.copy());
        return new MultiThermo(rThermo);
    }
    public static MultiThermo fromTableList(Collection<? extends ITable> aTableList) {
        List<ITable> rThermo = new ArrayList<>(aTableList.size());
        for (ITable tTable : aTableList) rThermo.add(tTable.copy());
        return new MultiThermo(rThermo);
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static MultiThermo zl() {return new MultiThermo();}
    public static MultiThermo of(ITable aTable) {return fromTable(aTable);}
    public static MultiThermo of(Iterable<? extends ITable> aTableList) {return fromTableList(aTableList);}
    public static MultiThermo of(Collection<? extends ITable> aTableList) {return fromTableList(aTableList);}
    
    
    /**
     * 从 csv 文件中读取 Thermo，自动根据输入路径选择，如果是文件夹则会读取多个 Thermo
     * @author liqa
     * @param aPath csv 文件路径或者文件夹名称
     * @return 读取得到的 Thermo 对象
     * @throws IOException 如果读取失败
     */
    public static MultiThermo readCSV(String aPath) throws IOException {
        if (UT.IO.isDir(aPath)) {
            if (aPath.equals(".")) aPath = "";
            aPath = UT.IO.toInternalValidDir(aPath);
            String[] tFiles = UT.IO.list(aPath);
            List<ITable> rThermo = new ArrayList<>(tFiles.length);
            for (String tName : tFiles) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tFileOrDir = aPath+tName;
                if (UT.IO.isFile(tFileOrDir)) {
                    rThermo.add(UT.IO.csv2table(tFileOrDir));
                }
            }
            return new MultiThermo(rThermo);
        } else {
            return new MultiThermo(UT.IO.csv2table(aPath));
        }
    }
    
    /**
     * 从文件 lammps 输出的 log 文件中读取来实现初始化，
     * 对于 log 只实现其的读取，而输出则会使用通用方法输出成 csv 文件
     * @author liqa
     * @param aFilePath lammps 输出的 log 文件路径
     * @return 读取得到的 Thermo 对象，如果文件不完整的帧会尝试读取已有的部分
     * @throws IOException 如果读取失败
     */
    public static MultiThermo read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    static MultiThermo read_(List<String> aLines) {
        List<ITable> rThermo = new ArrayList<>();
        
        int idx = 0, endIdx;
        String[] tTokens;
        while (idx < aLines.size()) {
            // 跳转到 "Per MPI rank memory allocation" 后面的 "Step" 行，也就是需要 thermo 中包含 step 项
            idx = UT.Text.findLineContaining(aLines, idx, "Per MPI rank memory allocation");
            idx = UT.Text.findLineContaining(aLines, idx, "Step", true);
            if (idx >= aLines.size()) break;
            // 获取种类的 key
            tTokens = UT.Text.splitBlank(aLines.get(idx));
            String[] tHeads = tTokens;
            ++idx;
            // 获取结束的位置
            endIdx = UT.Text.findLineContaining(aLines, idx, "Loop time of");
            // 如果没有找到则跳过最后一行（不完整）
            if (endIdx >= aLines.size()) endIdx = aLines.size()-1;
            ITable rTable = Tables.zeros(endIdx-idx, tHeads);
            // 读取数据
            for (IVector tRow : rTable.rows()) {
                tRow.fill(UT.Text.str2data(aLines.get(idx), tHeads.length));
                ++idx;
            }
            // 创建 Table 并附加到 rThermo 中
            rThermo.add(rTable);
        }
        return new MultiThermo(rThermo);
    }
    
    /**
     * 将 Thermo 写入到文件夹中的 csv 文件，会自动创建多个文件
     * @author liqa
     * @param aDir 需要输出的文件夹名称
     * @throws IOException 如果写入文件失败
     */
    public void write(String aDir) throws IOException {
        aDir = UT.IO.toInternalValidDir(aDir);
        for (int i = 0; i < mList.size(); ++i) {
            UT.IO.table2csv(mList.get(i), aDir+"thermo-"+i+".csv");
        }
    }
}
