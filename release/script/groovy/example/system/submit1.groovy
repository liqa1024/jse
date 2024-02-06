package example.system

import static jse.code.UT.Exec.*

submitSystem('echo 111111') // 异步执行，会在 222222 后输出
println('222222')
system('echo 333333')
println('444444')

//OUTPUT:
// 222222
// 111111
// 333333
// 444444

