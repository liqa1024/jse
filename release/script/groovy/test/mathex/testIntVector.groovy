package test.mathex

import static jtool.code.UT.Math.*

/**
 * 测试整数数组的排序和打乱功能
 */


def r = linsequence(0, 1, 10).subVec(2, 10);
def v = rand(10).opt().refReverse().subVec(2, 10);
println(r);
println(v);

v.opt().bisort(r);
println(r);
println(v);

