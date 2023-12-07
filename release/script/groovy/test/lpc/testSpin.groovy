package test.lpc

import jtool.atom.Structures
import jtool.lmp.LmpExecutor
import jtool.lmp.LmpIn
import jtool.lmp.Lmpdat
import jtool.system.WSL

import static jtool.code.CS.*

/**
 * 测试 lammps 的 spin 包
 */

// 创建初始 fcc 结构
//Lmpdat.fromAtomData(Structures.FCC(3.54, 5), MASS.Co).write('lmp/.temp/data-in-Co500');


final def lmpExe = '~/.local/bin/lmp';
final int lmpCores = 4;

// 创建 lmp 并运行
try (def lmp = new LmpExecutor(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}")) {
    def spinIn = LmpIn.of('lmp/in/Co-data-spin')
        .i('vInDataPath', 'lmp/.temp/data-in-Co500')
        .o('vOutDataPath', 'lmp/.temp/data-out-Co500')
    ;
    spinIn.vRunStep = 20000;
    lmp.run(spinIn);
}

