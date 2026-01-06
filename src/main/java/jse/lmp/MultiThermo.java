package jse.lmp;

import jse.code.IO;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.NewCollections;
import jse.code.functional.IFilter;
import jse.math.table.ITable;
import jse.math.table.Tables;
import jse.math.vector.IVector;

import java.io.BufferedReader;
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
    public MultiThermo append(ITable aTable) {return (MultiThermo)super.append(aTable);}
    public MultiThermo appendAll(Collection<? extends ITable> aTableList) {return (MultiThermo)super.appendAll(aTableList);}
    public MultiThermo appendFile(String aFilePath) throws IOException {
        mList.addAll(read(aFilePath).mList);
        return this;
    }
    public MultiThermo appendCSV(String aPath) throws IOException {
        mList.addAll(readCSV(aPath).mList);
        return this;
    }
    /** groovy stuffs */
    @Override public MultiThermo leftShift(ITable aTable) {return (MultiThermo)super.leftShift(aTable);}
    @Override public MultiThermo leftShift(Collection<? extends ITable> aTableList) {return (MultiThermo)super.leftShift(aTableList);}
    
    
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
                if (!headMatch(aHeads, tTable.heads())) break;
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
        return readCSV(aPath, name -> name.endsWith(".csv"));
    }
    public static MultiThermo readCSV(String aPath, IFilter<String> aFilter) throws IOException {
        if (IO.isDir(aPath)) {
            if (aPath.equals(".")) aPath = "";
            aPath = IO.toInternalValidDir(aPath);
            String[] tFiles = IO.list(aPath);
            List<ITable> rThermo = new ArrayList<>(tFiles.length);
            for (String tName : tFiles) if (aFilter.accept(tName)) {
                rThermo.add(IO.csv2table(aPath+tName));
            }
            return new MultiThermo(rThermo);
        } else {
            return new MultiThermo(IO.csv2table(aPath));
        }
    }
    
    /**
     * 从文件 lammps 输出的 log 文件中读取来实现初始化，
     * 对于 log 只实现其的读取，而输出则会使用通用方法输出成 csv 文件
     * @author liqa
     * @param aFilePath lammps 输出的 log 文件路径
     * @return 读取得到的 Thermo 对象，理论上文件不完整的帧会尝试读取已有的部分
     * @throws IOException 如果读取失败
     */
    public static MultiThermo read(String aFilePath) throws IOException {try (BufferedReader tReader = IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static MultiThermo read_(BufferedReader aReader) throws IOException {
        String tLine;
        List<ITable> rThermo = new ArrayList<>();
        
        while (true) {
            // 跳转到 "Per MPI rank memory allocation" 后面的 "Step" 行，也就是需要 thermo 中包含 step 项
            tLine = IO.Text.findLineContaining(aReader, "Per MPI rank memory allocation"); if (tLine == null) break;
            tLine = IO.Text.findLineContaining(aReader, "Step", true); if (tLine == null) break;
            // 获取种类的 key
            String[] tHeads = IO.Text.splitBlank(tLine);
            // 直接遍历读取数据
            List<IVector> rDataRows = new ArrayList<>();
            while ((tLine = aReader.readLine()) != null) {
                // 现在改为字符串判断而不是靠不稳定的 str2data 来判断结束
                if (IO.Text.containsIgnoreCase(tLine, "Loop time of")) break;
                rDataRows.add(IO.Text.str2data(tLine, tHeads.length));
            }
            // 创建 Table 并附加到 rThermo 中
            rThermo.add(Tables.fromRows(rDataRows, tHeads));
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
        aDir = IO.toInternalValidDir(aDir);
        for (int i = 0; i < mList.size(); ++i) {
            IO.table2csv(mList.get(i), aDir+"thermo-"+i+".csv");
        }
    }
}
