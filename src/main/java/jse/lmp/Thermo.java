package jse.lmp;

import jse.code.IO;
import jse.math.table.ITable;
import jse.math.table.Table;
import jse.math.table.Tables;
import jse.math.vector.IVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liqa
 * <p> lammps 使用 thermo 写出到 log 的数据格式 </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 本身是一个 {@link Table} 从而方便外部使用 </p>
 */
public class Thermo extends Table {
    
    Thermo(Table aTable) {
        super(aTable.nrows(), aTable.internalData(), aTable.internalHeads());
    }
    
    @Override public Thermo copy() {
        return new Thermo(super.copy());
    }
    
    /**
     * 直接从 {@link ITable} 来创建
     * @author liqa
     */
    public static Thermo fromTable(ITable aTable) {
        return new Thermo(Tables.from(aTable));
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static Thermo of(ITable aTable) {return fromTable(aTable);}
    
    
    private static boolean headMatch(String[] aSrcHeads, String[] aNextHeads) {
        if (aNextHeads.length < aSrcHeads.length) return false;
        for (int i = 0; i < aSrcHeads.length; ++i) {
            if (!aSrcHeads[i].equals(aNextHeads[i])) return false;
        }
        return true;
    }
    
    /**
     * 从 csv 文件中读取 Thermo
     * @author liqa
     * @param aPath csv 文件路径名称
     * @return 读取得到的 Thermo 对象
     * @throws IOException 如果读取失败
     */
    public static Thermo readCSV(String aPath) throws IOException {
        Table tTable = IO.csv2table(aPath);
        return tTable==null ? null : new Thermo(tTable);
    }
    
    /**
     * 从文件 lammps 输出的 log 文件中读取来实现初始化，
     * 对于 log 只实现其的读取，而输出则会使用通用方法输出成 csv 文件
     * @author liqa
     * @param aFilePath lammps 输出的 log 文件路径
     * @return 读取得到的 Thermo 对象，如果有多个 thermo 会尝试自动合并，理论上文件不完整的帧会尝试读取已有的部分
     * @throws IOException 如果读取失败
     */
    public static Thermo read(String aFilePath) throws IOException {try (BufferedReader tReader = IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static Thermo read_(BufferedReader aReader) throws IOException {
        String tLine;
        String[] aHeads = null;
        List<IVector> rDataRows = new ArrayList<>();
        
        while (true) {
            // 跳转到 "Per MPI rank memory allocation" 后面的 "Step" 行，也就是需要 thermo 中包含 step 项
            tLine = IO.Text.findLineContaining(aReader, "Per MPI rank memory allocation"); if (tLine == null) break;
            tLine = IO.Text.findLineContaining(aReader, "Step", true); if (tLine == null) break;
            // 获取种类的 key
            String[] tHeads = IO.Text.splitBlank(tLine);
            if (aHeads == null) {
                aHeads = tHeads;
            } else {
                // 如果不匹配则终止读取
                if (!headMatch(aHeads, tHeads)) break;
            }
            // 直接遍历读取数据
            while ((tLine = aReader.readLine()) != null) {
                // 现在改为字符串判断而不是靠不稳定的 str2data 来判断结束
                if (IO.Text.containsIgnoreCase(tLine, "Loop time of")) break;
                rDataRows.add(IO.Text.str2data(tLine, tHeads.length));
            }
        }
        if (aHeads == null) return null;
        return new Thermo(Tables.fromRows(rDataRows, aHeads));
    }
    
    /**
     * 将 Thermo 写入到 csv 文件
     * @author liqa
     * @param aFilePath 需要输出的路径或者文件夹名称
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {
        IO.table2csv(this, aFilePath);
    }
}
