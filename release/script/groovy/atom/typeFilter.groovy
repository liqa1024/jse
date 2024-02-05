package atom

import jse.atom.Generator
import jse.code.UT
import jse.vasp.POSCAR

import static jse.code.UT.Code.*

/** 全局参数 */
// 每层 z 的判据
final double z12 = 4.0;
final double z23 = 8.0;
final double z34 = 12.0;
final double z45 = 16.0;
final double z56 = 20.0;
final double z67 = 25.0;
final double z78 = 28.0;

// 输入结构路径
final def inDataPath = 'lmp/data/BTO-SC221.vasp';

// 输出结构的文件夹和名称
final def outDataDir = 'lmp/out/data/';
final def outDataName = 'BTO-SC221';

// 结构数目
final int batchSize = 50;


/** 具体生成的代码 */
// 需要先创建输出文件夹
UT.IO.mkdir(outDataDir);

// 读取初始结构（POSCAR 格式）
def oldPos = POSCAR.read(inDataPath);

// 获取 Bi 的数目
int BiNum = oldPos.atomNum('Bi');
// 获取每一层 Bi 的数目
int BiLayerNum = BiNum.intdiv(8);

// 获取结构生成器
def GEN = new Generator();

for (i in range(batchSize)) {
    // 获取上半部分是否是 Bi 的打乱序列
    final def isBiList = new LinkedList<Boolean>();
    for (_ in range(BiLayerNum*2)) isBiList.add(true);
    for (_ in range(BiLayerNum*2)) isBiList.add(false);
    // 打乱标记
    Collections.shuffle(isBiList);
    
    // 获取下半部分的种类打乱序列
    final def LaSmNbList = new LinkedList<Integer>();
    for (_ in range(BiLayerNum*2)) LaSmNbList.add(4); // La
    for (_ in range(BiLayerNum*2)) LaSmNbList.add(5); // Sm
    for (_ in range(BiLayerNum*2)) LaSmNbList.add(6); // Nb
    // 打乱标记
    Collections.shuffle(LaSmNbList);
    
    
    // 使用生成器获取替换原子种类的结构
    def filtered = GEN.typeFilterAtomData(oldPos, 6, {atom ->
        if (atom.type() == 1) {
            if ((atom.z() < z34 && atom.z() > z12) || (atom.z() < z78 && atom.z() > z56)) {
                // 如果是 23 层或 67 层，保留一半的 Bi
                boolean isBi = isBiList.removeFirst();
                if (isBi) return 1;
                else return LaSmNbList.removeFirst();
            } else {
                // 如果是其他，全部替换
                return LaSmNbList.removeFirst();
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

