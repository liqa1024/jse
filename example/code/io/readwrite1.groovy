package example.io

import static jse.code.UT.IO.*

// 写入多行字符串到文件
write('.temp/example/io/1.txt', ['111111', '222222', '333333'])

// 直接写入字符串到文件
write('.temp/example/io/2.txt', '''\
444444
555555
666666\
''')

// 读取
def lines1 = readAllLines('.temp/example/io/1.txt')
def text2 = readAllText('.temp/example/io/2.txt')
println('lines of .temp/example/io/1.txt:')
println(lines1)
println('text of .temp/example/io/2.txt:')
println(text2)


//OUTPUT:
// lines of .temp/example/io/1.txt:
// [111111, 222222, 333333]
// text of .temp/example/io/2.txt:
// 444444
// 555555
// 666666
//

