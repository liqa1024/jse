package example.system

import static jse.code.UT.Exec.*

// 接收到 Future<Integer> 异步任务
def task1 = submitSystem('echo 111111')
def task2 = submitSystem('echo 222222')

// 此时这个异步任务依旧在运行，因此这个结果先输出
println("isDone: ${task1.isDone()}, ${task2.isDone()}")
println('333333')
println("isDone: ${task1.isDone()}, ${task2.isDone()}")

// 直接取消任务2，传入 true 表明即使已经在运行了也会尝试中断
task2.cancel(true)
println("isCancelled: ${task1.isCancelled()}, ${task2.isCancelled()}")
// 此时任务已经被取消，因此不会有输出
println("isDone: ${task1.isDone()}, ${task2.isDone()}")

// 等待任务1 完成并获取结果
println("exitValue: ${task1.get()}")

// 此时任务已经运行完成，因此这个结果后输出
println("isDone: ${task1.isDone()}, ${task2.isDone()}")
println('444444')

//OUTPUT:
// isDone: false, false
// 333333
// isDone: false, false
// isCancelled: false, true
// isDone: false, true
// 111111
// exitValue: 0
// isDone: true, true
// 444444

