package obj

import com.guan.io.IOFiles
import com.guan.system.ISystemExecutor


/** Groovy UT */
class GUT {
    /** 在给定的 ISystemExecutor 位置初始化 jTool 的环境，使得在对应环境能够使用 ./jTool 指令。一般需要对方是远程的环境 */
    def static initJToolEnv(ISystemExecutor exe) {
        // 构造输入输出文件，涉及到的输入文件为 jTool 本身的库，jTool 执行脚本，key 是随便起的
        var ioFiles = (new IOFiles()).setIFiles('lib', 'lib/jTool-all.jar', 'shell', 'jTool', 'bat', 'jTool.bat');
        // 发送指令设置脚本的运行权限，不需要输出，执行此指令前会自动上传附加文件
        exe.system_NO('chmod 777 jTool', ioFiles);
    }
}
