package test.system

import jse.code.UT
import jse.lmp.Dump

// 读取测试数据，1000 帧的 dump
UT.Timer.tic();
def dump = Dump.read('lmp/.temp/dump-1000');
UT.Timer.toc('read dump');

