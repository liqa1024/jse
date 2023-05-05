package com.guan;


import com.guan.script.Groovy;

/**
 * @author liqa
 * <p> 直接运行 jTool 时的主要类，根据输入参数来决定具体操作 </p>
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // 默认执行 groovy 脚本
        Groovy.run(args[0]);
    }
}
