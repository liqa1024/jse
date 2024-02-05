package test.mathex

import jse.math.ComplexDouble

/** 测试复数 */


// 构造复数并输出模量和相位
def a = new ComplexDouble();
a.setNormPhase(2, 0.5);

println("$a, norm: ${a.norm()}, phase: ${a.phase()}");
