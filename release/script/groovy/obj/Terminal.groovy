package obj

import com.jtool.code.UT
import com.jtool.system.ISystemExecutor
import com.jtool.system.Local

/**
 * 用来实现在 Groovy 中像调用方法一样调用系统指令
 * @author liqa
 */
class Terminal {
    ISystemExecutor exe;
    
    Terminal() {this.exe = new Local();}
    Terminal(ISystemExecutor exe) {this.exe = exe;}
    
    /** 调用方法则输出到控制台 */
    def invokeMethod(String name, args) {
        Object[] tArgs = (Object[])args;
        if (tArgs.length == 0) return exe.system(name);
        return exe.system(name+" "+String.join(" ", UT.Code.map(tArgs, v->v.toString())));
    }
    
    /** 调用成员则直接输出 */
    def getProperty(String propertyName) {
        String cmd = propertyName.replaceAll("_", " ");
        return exe.system_str(cmd);
    }
}
