// import jTool classes
import com.guan.atom.Generator;
import com.guan.lmp.Lmpdat;
import com.guan.lmp.Dump;

// 创建生成器，设置并行数为 4
GEN = new Generator(4);
// 使用 Cahn-Hilliard 方程生成随机的多孔结构，使用默认参数迭代 50000 步
result = GEN.porousCahnHilliard(50000);

// 生成初始 fcc 结构，每个方向重复 20 次（共 32000 个原子）
tAtomData = GEN.atomDataFCC(3.61, 20);
// 根据 result 来过滤 tAtomData，只保留 result>0 处的粒子
tAtomData = GEN.filterThresholdFunc3AtomData(result, tAtomData);
// 根据 AtomData 生成 Lmpdat，指定粒子质量为 63.546（Cu）
jLmpdat = Lmpdat.fromAtomData(tAtomData, 63.546);
// 生成 AtomData 生成 lammps 的 Dump
jDump = Dump.fromAtomData(tAtomData);

// 保存成文本
jLmpdat.write('porous.lmpdat');
jDump.write('porous.lammpstrj');

// 记得关闭生成器，释放资源
GEN.shutdown();
