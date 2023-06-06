package atom

import com.jtool.atom.Generator
import com.jtool.code.UT
import com.jtool.vasp.POSCAR

import static com.jtool.code.UT.Code.*

/** 全局参数 */
// 每层 z 的判据
final double z12 = 6.09;
final double z23 = 10.66;
final double z34 = 20.61;

// 输入结构路径
final def inDataPath = 'lmp/data/layer1-SC441';

// 输出结构的文件夹和名称
final def outDataDir = 'lmp/out/data/';
final def outDataName = 'layer1-SC441';

// 结构数目
final int batchSize = 10;


/** 具体生成的代码 */
// 需要先创建输出文件夹
UT.IO.mkdir(outDataDir);

// 读取初始结构（POSCAR 格式）
def oldPos = POSCAR.read(inDataPath);

// 获取 Bi 的数目
int BiNum = oldPos.atomNum('Bi');
// 获取每一层 Bi 的数目
int BiLayerNum = Math.round(BiNum / 4) as int;

// 获取结构生成器
def GEN = new Generator();

for (i in range(batchSize)) {
    // 获取上半部分是否是 Bi 的打乱序列
    final def isBiList = new LinkedList<Boolean>();
    for (_ in range(BiLayerNum)) isBiList.add(true);
    for (_ in range(BiLayerNum)) isBiList.add(false);
    // 打乱标记
    Collections.shuffle(isBiList);
    
    // 获取下半部分的种类打乱序列
    final def LaSmNbList = new LinkedList<Integer>();
    for (_ in range(BiLayerNum)) LaSmNbList.add(4); // La
    for (_ in range(BiLayerNum)) LaSmNbList.add(5); // Sm
    for (_ in range(BiLayerNum)) LaSmNbList.add(6); // Nb
    // 打乱标记
    Collections.shuffle(LaSmNbList);
    
    
    // 使用生成器获取替换原子种类的结构
    def filtered = GEN.typeFilterAtomData(oldPos, 6, {atom ->
        if (atom.type() == 1) {
            if (atom.z() > z23) {
                // 如果是前两层，保留一半的 Bi
                boolean isBi = isBiList.pollFirst();
                if (isBi) return 1;
                else return LaSmNbList.pollFirst();
            } else {
                // 如果是后两层，全部替换
                return LaSmNbList.pollFirst();
            }
        }
        return atom.type();
    });
    
    // 将结构转换成 POSCAR，标注元素种类
    def newPos = POSCAR.fromAtomData(filtered, 'Bi', 'O', 'Ti', 'La', 'Sm', 'Nb');
    
    newPos.write("${outDataDir}${outDataName}-$i");
}

// 最后关闭生成器
GEN.shutdown();

