package test

import com.guan.code.UT
import com.guan.system.SSH

/** 自动关闭带有线程池的一些资源，理论上这种写法更加严谨，但是这里不做要求 */

// 支持 java 的 try-with-resources 语法来使用这些类，可以在退出语句块时自动关闭
try (var ssh = new SSH(UT.IO.json2map('.SECRET/SSH_INFO.json').csrc as Map)) {
    // 任意的发送指令等等任务
    ssh.system('echo 123');
    
    // 读取不存在的文件，会在此抛出错误，原本的写法会导致关闭的语句无法到达，从而导致资源未释放，在这种写法下则不会
    UT.IO.readAllLines('123123/123123');
    
    // 标记是否有到达此处
    println('MARK');
    
    // 此时不再需要手动关闭
//    ssh.shutdown();
}

