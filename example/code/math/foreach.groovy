package code.math

import static jse.code.UT.Math.*

def vec = linsequence(0.0d, 1.0d, 3)

// 索引遍历
for (i in 0..<vec.size()) {
    println(vec[i])
}
// 转为 Iterable 遍历
for (v in vec.iterable()) {
    println(v)
}
// 转为 List 遍历
for (v in vec.asList()) {
    println(v)
}
// for-each 遍历
vec.forEach {v ->
    println(v)
}

