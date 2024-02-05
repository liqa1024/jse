package obj

import jse.iofile.IOFiles
import jse.system.ISystemExecutor


/** Groovy UT */
class GUT {
    /** 在给定的 ISystemExecutor 位置初始化 jse 的环境，使得在对应环境能够使用 ./jse 指令。一般需要对方是远程的环境 */
    def static initjseEnv(ISystemExecutor exe) {
        // 构造输入输出文件，涉及到的输入文件为 jse 本身的库，jse 执行脚本，key 是随便起的
        var ioFiles = (new IOFiles())
                .i('lib', 'lib/jse-all.jar')
                .i('shell', 'jse')
                .i('bat', 'jse.bat');
        // 发送指令设置脚本的运行权限，执行此指令前会自动上传附加文件
        exe.system('chmod 777 jse', ioFiles);
    }
}
