package example.io

import static jse.code.UT.IO.*

write('.temp/example/io/a/1.txt', '''\
111111
222222
333333\
''')

// 将文件夹转为 zip，会消去文件夹这层，相当于将 a/1.txt 转为 a1.zip/1.txt
dir2zip('.temp/example/io/a', '.temp/example/io/a1.zip')
// 将文件夹当作文件添加到 zip 中，则会保留文件夹这层，相当于将 a/1.txt 转为 a2.zip/a/1.txt
files2zip(['.temp/example/io/a'], '.temp/example/io/a2.zip')

// 解压只有这一种逻辑
zip2dir('.temp/example/io/a1.zip', '.temp/example/io/a1')
zip2dir('.temp/example/io/a2.zip', '.temp/example/io/a2')

println(readAllText('.temp/example/io/a1/1.txt'))
println(readAllText('.temp/example/io/a2/a/1.txt'))

rmdir('.temp/example/io/a')
rmdir('.temp/example/io/a1')
rmdir('.temp/example/io/a2')

//OUTPUT:
// 111111
// 222222
// 333333
//
// 111111
// 222222
// 333333
//

