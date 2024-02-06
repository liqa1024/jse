package example.system

import jse.system.CMD

try (def exec = new CMD()) {
    // 在 cmd 中需要使用 `cd` 来获取当前目录而不是 `pwd`
    exec.system('pwd')
    exec.system('cd')
}

//OUTPUT
// 'pwd' 不是内部或外部命令，也不是可运行的程序
// 或批处理文件。
// ${your current directory}

