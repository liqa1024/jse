package example.io

import java.nio.ByteBuffer

import static jse.code.UT.IO.*


// 先创建一个二进制文件数据，jse 目前没有提供专门的方法，
// 因此这里使用 java 中通用的方法
double[] data = [1.1111, 2.2222, 3.3333]
def buf = ByteBuffer.allocate(data.size()*Double.BYTES)
for (d in data) buf.putDouble(d)
def bytes = buf.array();
// 写入数据到文件
write('.temp/example/io/1.bin', bytes)

// 读取
def bytes1 = readAllBytes('.temp/example/io/1.bin')
// 转为 double[]
def buf1 = ByteBuffer.wrap(bytes1)
double[] data1 = new double[bytes1.size().intdiv(Double.BYTES)]
for (i in 0..<data1.size()) data1[i] = buf1.getDouble()

println('origin data:')
println(data)
println('read data:')
println(data1)


//OUTPUT:
// origin data:
// [1.1111, 2.2222, 3.3333]
// read data:
// [1.1111, 2.2222, 3.3333]

