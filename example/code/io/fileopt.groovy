package example.io

import static jse.code.UT.IO.*

// 创建目录，支持直接递归创建子目录
mkdir('.temp/example/io/a/b/c/d')
println('.temp/example/io/a/b/c/d created')
// 创建文件
write('.temp/example/io/a/1.txt', '12345654321')


// 检测目录存在
println('.temp/example/io/a/b exists: ' + exists('.temp/example/io/a/b'))
// 检测是否是目录
println('.temp/example/io/a/b/c is dir: ' + isdir('.temp/example/io/a/b/c'))
// 检测是否是文件
println('.temp/example/io/a/b/c/d is file: ' + isfile('.temp/example/io/a/b/c/d'))
println('.temp/example/io/a/1.txt is file: ' + isfile('.temp/example/io/a/1.txt'))

// 移动目录
move('.temp/example/io/a/b/c', '.temp/example/io/a/c')
println('move .temp/example/io/a/b/c to .temp/example/io/a/c')
// 复制文件
copy('.temp/example/io/a/1.txt', '.temp/example/io/a/e/1.txt')
println('copy .temp/example/io/a/1.txt to .temp/example/io/a/e/1.txt')
// 映射文件
map('.temp/example/io/a/1.txt', '.temp/example/io/a/2.txt') {line -> line.replace('3', 'AAA')}
println('.temp/example/io/a/2.txt: ' + readAllLines('.temp/example/io/a/2.txt'))

// 列出目录内容
println('.temp/example/io/a list: ' + list('.temp/example/io/a'))


// 使用 delete 删除，不支持删除有内容的目录
try {
    delete('.temp/example/io/a')
    println('.temp/example/io/a deleted')
} catch (any) {
    println('delete .temp/example/io/a fail: ' + any)
}
try {
    delete('.temp/example/io/a/c/d')
    println('.temp/example/io/a/c/d deleted')
} catch (any) {
    println('delete .temp/example/io/a/c/d fail: ' + any)
}

// 移除目录，支持直接递归移除子目录
rmdir('.temp/example/io/a')
println('.temp/example/io/a removed')
println('.temp/example/io/a exists: ' + exists('.temp/example/io/a'))


// 检测路径是否相同
println('a/b/c a/b same path: ' + samePath('a/b/c', 'a/b'))
println('a/b/c/.. a/b same path: ' + samePath('a/b/c/..', 'a/b'))
println('a/b\\c a/b/c same path: ' + samePath('a/b\\c', 'a/b/c'))


//OUTPUT:
// .temp/example/io/a/b/c/d created
// .temp/example/io/a/b exists: true
// .temp/example/io/a/b/c is dir: true
// .temp/example/io/a/b/c/d is file: false
// .temp/example/io/a/1.txt is file: true
// move .temp/example/io/a/b/c to .temp/example/io/a/c
// copy .temp/example/io/a/1.txt to .temp/example/io/a/e/1.txt
// .temp/example/io/a/2.txt: [12AAA45654AAA21]
// .temp/example/io/a list: [1.txt, 2.txt, b, c, e]
// delete .temp/example/io/a fail: java.nio.file.DirectoryNotEmptyException: ${WorkingDir}\.temp\example\io\a
// .temp/example/io/a/c/d deleted
// .temp/example/io/a removed
// .temp/example/io/a exists: false
// a/b/c a/b same path: false
// a/b/c/.. a/b same path: true
// a/b\c a/b/c same path: true

