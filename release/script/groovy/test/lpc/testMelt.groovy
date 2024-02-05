package test.lpc

import jse.lmp.LmpExecutor
import jse.lmp.LmpIn
import jse.system.WSL

/**
 * 直接本地跑熔融，快速得到此温度下的结构
 */

final def lmpExe = '~/.local/bin/lmp';
final int lmpCores = 12;

//final def lmpExe = 'lmp_ann';
//final int lmpCores = 10;

try (def lmp = new LmpExecutor(new WSL(), "mpiexec -np ${lmpCores} ${lmpExe}")) {
    def spinIn = LmpIn.of('lmp/in/CuZr-data-melt')
        .i('vInDataPath', 'lmp/.temp/data-in-Cu108')
        .o('vOutDataPath', 'lmp/.temp/data-out-Cu108')
    ;
    spinIn.vT = 2000;
    spinIn.vRunStep = 20000;
    spinIn.vThermoStep = 100;
    
    spinIn.vInDataPath = 'lmp/data/CuFCC108.lmpdat';
    lmp.run(spinIn);
}


