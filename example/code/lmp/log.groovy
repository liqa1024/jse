package example.lmp

import jse.lmp.Log

// 读取现有的 lammps log 文件
def log = Log.read('lmp/log/CuFCC108.thermo')

// 获取属性
println('heads: ' + log.heads())
println('volume: ' + log['Volume'])

// 保存 csv
log.write('.temp/example/lmp/logFCC.csv')


//OUTPUT:
// heads: [Step, Temp, Press, Volume, PotEng, KinEng, TotEng]
// volume: 51-length Vector:
//    1270   1464   1484   1546  ...  1507   1498   1508   1475

