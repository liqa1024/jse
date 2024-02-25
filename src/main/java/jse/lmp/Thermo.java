package jse.lmp;

import jse.code.UT;
import jse.math.table.ITable;
import jse.math.table.Table;
import jse.math.table.Tables;
import jse.math.vector.IVector;

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
        super(aTable.rowNumber(), aTable.internalHeads(), aTable.internalData());
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
        return new Thermo(UT.IO.csv2table(aPath));
    }
    
    /**
     * 从文件 lammps 输出的 log 文件中读取来实现初始化，
     * 对于 log 只实现其的读取，而输出则会使用通用方法输出成 csv 文件
     * @author liqa
     * @param aFilePath lammps 输出的 log 文件路径
     * @return 读取得到的 Thermo 对象，如果有多个 thermo 会尝试自动合并
     * @throws IOException 如果读取失败
     */
    public static Thermo read(String aFilePath) throws IOException {return read_(UT.IO.readAllLines(aFilePath));}
    static Thermo read_(List<String> aLines) {
        String[] aHeads = null;
        List<IVector> rDataRows = new ArrayList<>();
        
        int idx = 0, endIdx;
        while (idx < aLines.size()) {
            // 跳转到 "Per MPI rank memory allocation" 后面的 "Step" 行，也就是需要 thermo 中包含 step 项
            idx = UT.Text.findLineContaining(aLines, idx, "Per MPI rank memory allocation");
            idx = UT.Text.findLineContaining(aLines, idx, "Step", true);
            if (idx >= aLines.size()) break;
            // 获取种类的 key
            String[] tHeads = UT.Text.splitBlank(aLines.get(idx));
            if (aHeads == null) {
                aHeads = tHeads;
            } else {
                // 如果不匹配则终止读取
                if (headMatch(aHeads, tHeads)) break;
            }
            ++idx;
            // 获取结束的位置
            endIdx = UT.Text.findLineContaining(aLines, idx, "Loop time of");
            // 如果没有找到则跳过最后一行（不完整）
            if (endIdx >= aLines.size()) endIdx = aLines.size()-1;
            // 直接遍历读取数据
            for (; idx < endIdx; ++idx) {
                rDataRows.add(UT.Text.str2data(aLines.get(idx), aHeads.length));
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
        UT.IO.table2csv(this, aFilePath);
    }
}
