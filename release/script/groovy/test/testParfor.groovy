package test

import static com.jtool.code.UT.Code.*


/** 测试并行 for 循环 */

/** groovy 支持这种更加类似 for 循环的写法 */
parfor(10) {int i, int threadID ->
    println("i: $i, threadID: $threadID");
    if (i == 5) throw new RuntimeException(i as String);
}
