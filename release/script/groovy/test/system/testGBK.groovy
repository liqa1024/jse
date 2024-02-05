package test.system

import jse.code.UT
import jse.system.CMD

import static jse.code.UT.Exec.*



UT.IO.write('.temp/testGBK', '卷的序列号是');
UT.IO.toWriteln('.temp/testGBK2').withCloseable {it.writeln('卷的序列号是')}

println('卷的序列号是');
system('echo 卷的序列号是');

new CMD().withCloseable {it.system('dir');}
