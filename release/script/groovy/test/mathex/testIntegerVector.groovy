package test.mathex

import jtool.math.vector.Vectors

/**
 * 测试整数数组的排序和打乱功能
 */


def v = Vectors.range(10);
println(v);

v.shuffle();
println(v);

v.sort();
println(v);

v.sort(Comparator.reverseOrder());
println(v);


