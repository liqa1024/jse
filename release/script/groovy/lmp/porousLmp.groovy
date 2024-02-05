package lmp

// import jse classes
import jse.code.UT;
import jse.atom.*;
import jse.lmp.*;

import static jse.code.CS.MASS;


/** 通用的生成多孔原子结构的方法 */
static IAtomData genAtomData(double cellSize=3.61, int replicate=20, double meshSize=0.1, int steps=50000, int nThreads=4) {
    // 创建生成器，设置并行数
    var GEN = new Generator(nThreads);
    // 使用 Cahn-Hilliard 方程生成随机的多孔结构
    var result = GEN.porousCahnHilliard(50, meshSize, steps);
    
    // 生成初始 fcc 结构，每个方向重复 20 次（共 32000 个原子）
    var atomData = GEN.atomDataFCC(cellSize, replicate);
    // 根据 result 来过滤 tAtomData，只保留 result>0 处的粒子
    atomData = GEN.filterThresholdFunc3AtomData(atomData, result);
    
    // 记得关闭生成器，释放资源
    GEN.shutdown();
    
    return atomData;
}
static Lmpdat genLmpdat(double mess=MASS.get('Cu'), double cellSize=3.61, int replicate=20, double meshSize=0.1, int steps=50000, int nThreads=4) {
    var atomData = genAtomData(cellSize, replicate, meshSize, steps, nThreads);
    return Lmpdat.fromAtomData(atomData, [mess]);
}
static Dump genDump(double cellSize=3.61, int replicate=20, double meshSize=0.1, int steps=50000, int nThreads=4) {
    var atomData = genAtomData(cellSize, replicate, meshSize, steps, nThreads);
    return Dump.fromAtomData(atomData);
}


/** 执行此脚本时的默认操作，lmpdat 和 dump 都进行输出 */
static void main(String[] args) {
    var atomData = genAtomData();
    // 根据 AtomData 生成 Lmpdat，指定粒子质量为 Cu 的原子质量
    var lmpdat = Lmpdat.fromAtomData(atomData, [MASS.get('Cu')]);
    // 生成 AtomData 生成 lammps 的 Dump
    var dump = Dump.fromAtomData(atomData);
    
    // 保存之前先创建文件夹
    UT.IO.mkdir('lmp/.temp');
    // 保存成文本
    lmpdat.write('lmp/.temp/porous.lmpdat'); println('The porous Lmpdat has been saved to lmp/.temp/porous.lmpdat');
    dump.write('lmp/.temp/porous.lammpstrj'); println('The porous Dump has been saved to lmp/.temp/porous.lammpstrj');
}

