package com.guan;


import com.guan.script.Groovy;

/**
 * @author liqa
 * <p> 直接运行 jTool 时的主要类，根据输入参数来决定具体操作 </p>
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // 没有输入时给出使用方法
        if (args.length == 0) {
            System.out.println("Usage: jTool path/to/script.groovy");
            System.out.println("Or you can also using another scripting language such as MATLAB or Python with Py4J and import jTool-*.jar");
            return;
        }
        // 默认执行 groovy 脚本
        Groovy.run(args[0]);
    }
}
