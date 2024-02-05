package test.lpc

import jse.atom.Structures
import jse.lmp.LmpExecutor
import jse.lmp.LmpIn
import jse.lmp.Lmpdat
import jse.system.WSL

import static jse.code.CS.*

/**
 * 测试 lammps 的 spin 包
 */

// 创建初始 fcc 结构
//Lmpdat.fromAtomData(Structures.FCC(3.54, 5), MASS.Co).write('lmp/.temp/data-in-Co500');


final def lmpExe = '~/.local/bin/lmp';
final int lmpCores = 2; // 由于 spin 的并行实现不同，过高的核数并没有用

// 创建 lmp 并运行
try (def lmp = new LmpExecutor(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}")) {
    def spinIn = LmpIn.of('lmp/in/Co-data-spin')
    .i('vInDataPath', 'lmp/.Co/data-in-Co500')
    .o('vOutDataPath', 'lmp/.Co/data-out-Co500')
    .o('vDumpPath', 'lmp/.Co/Co500.lammpstrj')
    ;
    spinIn.vT = 1;
    spinIn.vTimestep = 0.0002;
    spinIn.vRunStep = 40000;
    spinIn.vThermoStep = 100;
    spinIn.vDumpStep = 4;
    
    spinIn.vInDataPath = 'lmp/.Co/data-out-Co500-2800K';
    spinIn.vOutDataPath = 'lmp/.Co/data-out-Co500-glass';
    spinIn.vDumpPath = 'lmp/.Co/Co500-cooldown.lammpstrj';
    lmp.run(spinIn);
}

