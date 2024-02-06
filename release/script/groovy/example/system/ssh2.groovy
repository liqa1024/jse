package example.system

import jse.code.UT
import jse.io.IOFiles
import jse.system.SSH

// 替换成所需连接的远程服务器的 ip，用户名，以及密码
def sshInfo = [
    hostname: '127.0.0.1',
    username: 'admin',
    password: '123456'
]

// 设置输入输出文件
def iofiles = IOFiles.create() // 通过 create() 来创建一个空的 IOFiles
    .i('in', '.temp/example/system/ssh-in') // 链式调用指定输入文件的 key 以及路径
    .o('out', '.temp/example/system/ssh-out') // 链式调用指定输出文件的 key 以及路径

// 创建一个输入文件
UT.IO.write(iofiles.i('in'), '111111') // 可以直接通过 key 来获取存储的路径

try (def ssh = new SSH(sshInfo)) {
    // 提交任务，将输入文件移动到输出文件，并追加一行 222222
    ssh.system("mv ${iofiles.i('in')} ${iofiles.o('out')}; echo '222222' >> ${iofiles.o('out')}", iofiles);
}

// 读取输出文件，此时已经从 ssh 下载完毕
println(UT.IO.readAllLines(iofiles.o('out')));


//OUTPUT:
// [111111, 222222]

