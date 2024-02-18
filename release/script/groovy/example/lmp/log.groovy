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
// volume: 21-length Vector:
//    1270   1503   1473   1481   1515   1468   1452   1499   1499   1509   1495   1508   1467   1511   1449   1492   1544   1510   1470   1507   1527

