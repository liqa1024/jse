package test.system

import jse.code.UT
import jse.io.RefreshableFilePrintStream

println('987卷4\r序3456789\n7894561列\r\n789456\r987\n1');

UT.Timer.USE_ASCII = true;
System.setOut(new RefreshableFilePrintStream('.temp/testPBar'));

println('987卷4\r序3456789\n7894561列\r\n789456\r987\n1'); // 输出行为和 linux bash 以及 powershell 一致，注意和 idea 不一样

UT.Timer.pbar(50);
for (i in 0..<50) {
    sleep(100);
    UT.Timer.pbar();
}

