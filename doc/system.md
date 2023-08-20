- [任务提交接口](system.md)
  - [快速开始](#快速开始)
  - [指令输出控制](#指令输出控制)
  - [后台任务提交](#后台任务提交)
  - [其他种类的任务提交器](#其他种类的任务提交器)
  - [关闭任务提交器](#关闭任务提交器)
- [SSH 任务提交器](systemSSH.md)
- [SRUN 任务提交器](systemSRUN.md)
- [API 文档](systemAPI.md)

# 任务提交接口

## 快速开始

一般情况可以直接使用程序内部预置的任务提交器，在 Groovy 脚本中，可以使用类似语句：
```groovy
import static com.jtool.code.CS.Exec.EXE;
```
会导入存储在 [com.jtool.code.CS.Exec](../src/main/java/com/jtool/code/CS.java)
中的全局任务提交器。

> 在 matlab 脚本中，则为：
> ```matlab
> import com.jtool.code.CS.Exec.*
> ```
> 在 python 脚本中，则需要使用类似这种方式：
> ```python
> from py4j.java_gateway import JavaGateway
> GATEWAY = JavaGateway.launch_gateway(classpath='lib/jTool-all.jar')
> EXE = GATEWAY.jvm.com.jtool.code.CS.Exec.EXE
> ```
> 
> 使用全局任务提交器的好处是不需要手动使用 `shutdown()` 来关闭，程序内部会在结束后自动关闭。

--------------------------------

语法上和 matlab 类似，使用 `system()` 方法来执行系统指令从而提交任务，基本流程为（下只提供 Groovy 代码）：
```groovy
// 全局任务提交器
import static com.jtool.code.CS.Exec.EXE;

// 使用 system() 指令来执行系统指令，返回指令的退出值
def exitValue = EXE.system('echo 123456');

// 程序正常退出会返回 0
println("exitValue: $exitValue");
```


## 指令输出控制

默认情况下，一般的任务提交器会将指令的输出直接输出到控制台（`java.lang.System.out`）。

> 如上述代码则会在控制台中输出：
> ```
> 123456
> exitValue: 0
> ```
> 

--------------------------------

如果希望指令输出到指定路径的文本中，则需要在输入参数中增加一项路径：
```groovy
import static com.jtool.code.CS.Exec.EXE;

EXE.system('echo 123456', 'path/to/output/file');
```
此时则会将指令的输出写入路径 `path/to/output/file`。注意会自动创建文件夹使得路径合法，并且如果已经存在相同的文件则会直接覆盖。

--------------------------------

如果希望将指令的输出作为脚本中的一个变量，则可以使用 `system_str()` 指令：
```groovy
import static com.jtool.code.CS.Exec.EXE;

out = EXE.system_str('echo 123456');
// 返回值类型为 List<String>，按照输出的行来分隔
println(out.getClass());
// 大小为输出的行数
println(out.size());
// 直接打印整个 List
println(out);
// 可以通过 get() 方法来获取具体某一行的信息
println(out.get(0));
```

> 输出为：
> ```
> class java.util.ArrayList
> 1
> [123456]
> 123456
> ```
> 

--------------------------------

有时不希望保留指令的输出，则可以通过 `setNoSTDOutput()` 和 `setNoERROutput()` 来分别关闭标准输出和错误输出：
```groovy
import static com.jtool.code.CS.Exec.EXE;

// 一般情况
EXE.system('echo 111111');

// 关闭标准输出
EXE.setNoSTDOutput();
EXE.system('echo 222222');
EXE.system('echoecho 333333'); // 不存在的指令报错，错误输出依旧保留

// 开启标准输出，关闭错误输出；支持链式调用在一行中设置
EXE.setNoSTDOutput(false).setNoERROutput();
EXE.system('echo 444444');
EXE.system('echoecho 555555');

// 由于这个任务提交器是全局的，记得设置回原本的值
EXE.setNoSTDOutput(false).setNoERROutput(false);
```

> 输出为（注意 Windows 下错误报告会有所不同）：
> ```
> 111111
> /bin/bash: echoecho: command not found
> 444444
> ```
> 

## 后台任务提交

有时可能需要提交一个长期运行的任务并挂到后台（异步执行），然后继续进行其他运算。
这里提供了 `submitSystem()` 方法：
```groovy
import static com.jtool.code.CS.Exec.EXE;

EXE.submitSystem('echo 111111'); // 异步执行，会在 222222 后输出
println('222222');
EXE.system('echo 333333');
println('444444');
```

> 输出为：
> ```
> 222222
> 111111
> 333333
> 444444
> ```
> 

--------------------------------

这里使用 java 的 [Future](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html) 接口来管理这种异步任务：
```groovy
import static com.jtool.code.CS.Exec.EXE;

// 接收到一个 Future<Integer> 异步任务
def task = EXE.submitSystem('echo 111111');

// 此时这个异步任务依旧在运行，因此这个结果先输出
println("isDone: ${task.isDone()}");
println('222222');
println("isDone: ${task.isDone()}");

// 等待完成并获取结果
println("exitValue: ${task.get()}");

// 此时任务已经运行完成，因此这个结果后输出
println("isDone: ${task.isDone()}");
println('333333');
```

> 输出为：
> ```
> isDone: false
> 222222
> isDone: false
> 111111
> exitValue: 0
> isDone: true
> 333333
> ```
> 

--------------------------------

支持通过 `Future` 的 `cancel()` 方法来取消任务：
```groovy
import static com.jtool.code.CS.Exec.EXE;

// 接收到一个 Future<Integer> 异步任务
def task = EXE.submitSystem('echo 111111');

// 此时这个异步任务依旧在运行，因此这个结果先输出
println("isDone: ${task.isDone()}");
println('222222');
println("isDone: ${task.isDone()}");

// 直接取消这个任务，传入 true 表明即使已经在运行了也会尝试中断
task.cancel(true);
println("isCancelled: ${task.isCancelled()}");

// 此时任务已经被取消，因此不会有输出
println("isDone: ${task.isDone()}");
println('333333');
```

> 输出为：
> ```
> isDone: false
> 222222
> isDone: false
> isCancelled: true
> isDone: true
> 333333
> ```
> 


## 其他种类的任务提交器

所有的任务提交器位于 [com.jtool.system](../src/main/java/com/jtool/system)，详细说明可以查看 [API 文档](systemAPI.md)：

> 上述默认的全局任务提交器 `com.jtool.code.CS.Exec.EXE` 在 windows 下为
> `com.jtool.system.PowerShellSystemExecutor`，其余情况为 `com.jtool.system.LocalSystemExecutor`。

要使用这些任务提交器，只需要直接创建对应的实例即可，例如：
```groovy
import com.jtool.system.PS;

// 创建一个 PowerShell 提交器实例
def exe = new PS();

// 使用 system() 指令来执行系统指令，返回指令的退出值
def exitValue = exe.system('echo 123456');

// 程序正常退出会返回 0
println("exitValue: $exitValue");

// 对于自己创建新实例，需要在使用完成后手动关闭
exe.shutdown();
```


## 关闭任务提交器

由于任务提交器内部都存在线程池来管理后台任务，因此在使用结束后需要调用 `shutdown()` 来手动关闭这个任务提交器，例如：
```groovy
import com.jtool.system.PS;

def exe = new PS();
exe.system('echo 123456');

exe.shutdown();
```

但是如果在 `exe.shutdown();` 语句到达之前，程序抛出了一些错误中断了，此时任务提交器不会正常关闭，在一些时候程序会卡死。在 groovy 脚本中，支持 java 原生的 [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) 语法来自动关闭这个实例：
```groovy
import com.jtool.system.PS;

try (def exe = new PS()) {
    exe.system('echo 123456');
}
```

此时即使 `try` 语句中抛出了错误，也会正常关闭这个任务提交器。

> **注意**：由于全局的任务提交器 `com.jtool.code.CS.Exec.EXE` 在程序中其他地方也有使用，因此**不能**手动关闭。

