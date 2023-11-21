package test.code

import jtool.code.UT
import jtool.parallel.ParforThreadPool

import static jtool.code.UT.Par.*


/** 测试并行 for 循环 */

/** groovy 支持这种更加类似 for 循环的写法 */
UT.Timer.tic();
parfor(10, 4) {int i, int threadID ->
    println("i: $i, threadID: $threadID");
}
UT.Timer.toc();


ParforThreadPool.DEFAULT_IS_COMPETITIVE = false;
UT.Timer.tic();
parfor(10, 4) {int i, int threadID ->
    println("i: $i, threadID: $threadID");
}
UT.Timer.toc();

