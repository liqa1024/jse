- [任务提交](system.md)
    - [基本功能](#基本功能)
    - [输出控制](#输出控制)
    - [后台任务提交](#后台任务提交)
    - [自定义任务提交器种类](#自定义任务提交器种类)
    - [SSH 任务提交](#ssh-任务提交)
        - [免密连接](#免密连接)
        - [带有输入输出文件](#带有输入输出文件)
        - [SSH 参数 API](#ssh-参数-api)
- [**⟶ 目录**](contents.md)

# 任务提交

jse 通过提交系统指令的方式来提交任务，类似
[matlab 的 system 方法](https://ww2.mathworks.cn/help/matlab/ref/system.html)。
例如对于提交 lammps 任务，则通过类似方法
`system('lmp_mpi -in path/to/in/file')` 即可执行系统指令来运行 lammps。


## 基本功能

一般使用需要导入 [`jse.code.UT.Exec`](../src/main/java/jse/code/UT.java)
中的静态方法，然后通过 `system()` 方法来执行系统指令。

在 linux 系统下会将指令解释为 [bash 指令](https://wiki.archlinux.org/title/Bash)，
在 windows 下则会解释为 [powershell 指令](https://learn.microsoft.com/en-us/powershell/scripting/overview?view=powershell-7.4)。
一般来说两者可以使用相同的指令。

- 输入脚本（`jse example/system/basic`
  [⤤](../release/script/groovy/example/system/basic.groovy)）：
    
    ```groovy
    import static jse.code.UT.Exec.*
    
    def exitCode = system('echo 123456')
    println("exitCode: $exitCode")
    ```
    
- 输出：
    
    ```
    123456
    exitCode: 0
    ```

> `jse.code.UT.Exec` 中的静态方法在 shell 模式下是默认导入的，
> 因此在 shell 模式下可以直接执行 `system()` 方法而不需要导入。
> 


## 输出控制

默认情况下，一般的任务提交器会将指令的输出直接输出到控制台，
这里支持将其输出到指定文件（不通过系统的重定向功能），
或者作为程序中的字符串使用，以及关闭输出流。

### 输出到文本

如果希望指令输出到指定路径的文本中，则需要在输入参数中指定输出路径：

```groovy
system('echo 123456', 'path/to/output/file')
```

此时则会将指令的输出写入路径 `path/to/output/file`。

> 会自动创建文件夹使得路径合法，如果文件不存在则会创建文件，
> 如果已经存在相同的文件则会清空原本内容。
> 

### 输出为字符串

如果希望将指令的输出作为脚本中的一个变量，则可以使用 `system_str()` 指令：

```groovy
lines = system_str('echo 123456')
```

会将指令的输出作为方法的输出进行返回，具体为按行分隔的字符串列表 `List<String>`。

### 关闭输出

有时不希望保留指令的输出，首先需要通过 `exec()` 
获取到内部的指令执行器，然后调用 `setNoSTDOutput()` 和
`setNoERROutput()` 来分别关闭标准输出和错误输出：

```groovy
exec().setNoSTDOutput() // 关闭标准输出
exec().setNoERROutput() // 关闭错误输出
exec().setNoSTDOutput(false).setNoERROutput(false) // 重新打开标准输出和错误输出
```

--------------------------------

关于输出控制的实例，可以参看脚本 `example/system/output`
[⤤](../release/script/groovy/example/system/output.groovy)。


## 后台任务提交

jse 支持将任务提交到后台运行，而后继续进行后续运算（异步执行），
这里提供 `submitSystem()` 方法来实现这个功能：

- 输入脚本（`jse example/system/submit1`
  [⤤](../release/script/groovy/example/system/submit1.groovy)）：
    
    ```groovy
    import static jse.code.UT.Exec.*
    
    submitSystem('echo 111111') // 异步执行，会在 222222 后输出
    println('222222')
    system('echo 333333')
    println('444444')
    ```
    
- 输出：
    
    ```
    222222
    111111
    333333
    444444
    ```

> `submitSystem()` 会返回 java 的 
> [`Future<Integer>`](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html) 
> 用于管理这个异步任务，可以等待执行完成，取消任务等，
> 具体可参看脚本 `example/system/submit2`
> [⤤](../release/script/groovy/example/system/submit2.groovy)。
> 


## 自定义任务提交器种类

所有的任务提交器都位于包 `jse.system`，可以根据需要创建一个自定义的任务提交器，
而不是 jse 内部使用的全局任务提交器。

例如在 windows 下，如果不希望使用默认情况下的 powershell 来执行指令，
可以创建一个更加经典的 cmd 类型的任务提交器：

- 输入脚本（`jse example/system/custom`
  [⤤](../release/script/groovy/example/system/custom.groovy)）：
    
    ```groovy
    import jse.system.CMD
    
    try (def exec = new CMD()) {
        // 在 cmd 中需要使用 `cd` 来获取当前目录而不是 `pwd`
        exec.system('pwd')
        exec.system('cd')
    }
    ```
    
- 输出：
    
    ```
    'pwd' 不是内部或外部命令，也不是可运行的程序
    或批处理文件。
    ${你的当前目录}
    ```

> 手动创建新的任务提交器需要注意在使用完成后调用 `shutdown()` 方法关闭，
> 也可以像上述例子一样使用 
> [*try-with-resources*](https://www.baeldung.com/java-try-with-resources)
> 写法来实现自动关闭。
> 


## SSH 任务提交

jse 支持使用 ssh 向远程服务器来提交任务，这里基于
[jsch](http://www.jcraft.com/jsch/) 来实现这个功能。


这里需要通过 [`jse.system.SSH`](../src/main/java/jse/system/SSH.java)
来创建一个 ssh 任务提交器。
由于可选的输入参数较多，这里使用一个 `Map` 作为输入参数，
将 ip 地址，用户名等参数构造成 `Map` 后传入 ssh；
然后使用类似 [自定义任务提交器种类](#自定义任务提交器种类)
的方式创建一个 ssh 任务提交器，并执行任务即可：


- 输入脚本（`jse example/system/ssh1`
  [⤤](../release/script/groovy/example/system/ssh1.groovy)）：
    
    ```groovy
    import jse.system.SSH
    
    // 替换成所需连接的远程服务器的 ip，用户名，以及密码
    try (def ssh = new SSH(
        hostname: '127.0.0.1',
        username: 'admin',
        password: '123456'
    )) {
        ssh.system('echo 123456');
        ssh.system('hostname');
    }
    ```
    
- 输出：
    
    ```
    123456
    lon12
    ```

> 一般不希望在代码中出现明文的密码，为了避免这个问题可以将这些参数存储在
> json 文件中（如 `.SECRET/SSH_INFO.json`），然后使用读取 json
> 的方法来得到参数：
>
> ```groovy
> try (def ssh = new SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'))) {
>     /***/
> }
> ```
> 
> 具体实例以及 json 文件的写法可参看脚本 `example/system/ssh3`
> [⤤](../release/script/groovy/example/system/ssh3.groovy)。
> 

### 免密连接

这里同样支持免密连接 ssh，但是 jsch 只支持经典格式的 openSSH 密钥，
因此在生成密钥时需要加上 `-m pem` 参数，具体操作如下：

- **windows:**
    
    在 powershell 中输入：
    
    ```powershell
    ssh-keygen -m pem -t rsa -b 4096
    cat ~/.ssh/id_rsa.pub | ssh username@hostname "mkdir -p ~/.ssh && cat >> ~/.ssh/  authorized_keys && chmod 600 ~/.ssh/authorized_keys"
    ```
    
- **liunx:**
    
    直接在终端输入：
    
    ```shell
    ssh-keygen -m pem -t rsa -b 4096
    ssh-copy-id username@hostname
    ```
  
> **注意**: 需要将 `username` 和 `hostname` 分别替换成用户名以及服务器地址。
> 

如果已经有了密钥并且实现了免密连接，但是 jsch 不支持现在的密钥格式，
则可通过这个指令来修改密钥格式：

```shell
ssh-keygen -p -f .ssh/id_rsa -m pem
```

> 创建 ssh 任务提交器时不提供密码则会自动使用位于
> `~/.ssh/id_rsa` 的密钥进行验证。
> 

### 带有输入输出文件

对于存在输入输出文件的 ssh 任务，一般希望在任务开始之前先上传输入文件到远程服务器，
然后在任务完成后自动从远程服务器下载输出文件。

因此 jse 提供了 [`jse.io.IOFiles`](../src/main/java/jse/io/IOFiles.java)
类专门存储一个任务的输入输出文件（的路径），在执行系统指令时传入
`IOFiles` 即可将任务和这些输入输出文件进行绑定，
进而实现自动上传和下载文件。

具体实例可参看脚本 `example/system/ssh2`
[⤤](../release/script/groovy/example/system/ssh2.groovy)。

> 这里的输入输出文件**只支持相对路径**，而绝对路径通过上述
> `LocalWorkingDir` 和 `RemoteWorkingDir` 来调整，
> 这样可以减少代码中重复的绝对路径部分，增加可移植性；
> 而为了简化 ssh 任务提交器的实现，这里暂时**没有提供**对于绝对路径输入的提供支持。
> 

### SSH 参数 API

具体参数和解释如下：

- **`Username`/`username`/`user`/`u`**：
    
    类型：`String`
    
    描述：设置 ssh 连接使用的用户名。
    
    例子：`'admin'`
    
    默认行为：不得为空
    
    -----------------------------
    
- **`Hostname`/`hostname`/`host`/`h`**：
    
    类型：`String`
    
    描述：设置 ssh 连接主机的 ip。
    
    例子：`'127.0.0.1'`
    
    默认行为：不得为空
    
    -----------------------------
    
- **`Port`/`port`/`p`**：
    
    类型：`Integer`
    
    描述：设置 ssh 连接主机的端口。
    
    例子：`22`
    
    默认行为：22
    
    -----------------------------
    
- **`Password`/`password`/`pw`**：
    
    类型：`String`
    
    描述：设置 ssh 连接使用的密码。
    
    例子：`'123456abcdef'`
    
    默认行为：改为使用密钥验证
    
    -----------------------------
- **`KeyPath`/`keypath`/`key`/`k`**：
    
    类型：`String`
    
    描述：设置 ssh 连接使用的密钥的路径。
    
    例子：`'~/.ssh/id_rsa'`
    
    默认行为：使用默认密钥会保存的位置，如 `'~/.ssh/id_rsa'`
    
    -----------------------------
    
- **`CompressLevel`/`compresslevel`/`cl`**：
    
    类型：`Integer`
    
    描述：设置 ssh 传输的压缩等级，0~9
    （对于不支持压缩传输的服务器此项会自动失效）。
    
    例子：`6`
    
    默认行为：不开启压缩传输（`0`）
    
    -----------------------------
    
- **`LocalWorkingDir`/`localworkingdir`/`lwd`**：
    
    类型：`String`
    
    描述：设置本地的工作目录，所有输入输出文件会以此作为根目录。
    
    例子：`'./ssh'`
    
    默认行为：程序运行时使用的目录（`'.'`）
    
    -----------------------------
    
- **`RemoteWorkingDir`/`remoteworkingdir`/`rwd`/`wd`**：
    
    类型：`String`
    
    描述：设置远程的工作目录，所有输入输出文件会以此作为根目录。
    
    例子：`'~/path/to/project'`
    
    默认行为：ssh 连接后的目录，一般为用户目录（`'~'`）
    
    -----------------------------
    
- **`BeforeCommand`/`beforecommand`/`bcommand`/`bc`**：
    
    类型：`String`
    
    描述：设置所有 ssh 指令前添加的指令，一般是环境初始化。
    
    例子：`'module load jse'`
    
    默认行为：不添加此指令
    
    -----------------------------
    
- **`IOThreadNumber`/`iothreadnumber`/`IOThreadNum`/`iothreadnum`/`ion`**：
    
    类型：`Integer`
    
    描述：设置和 ssh 服务器传输文件时的并行线程数。
    
    例子：`4`
    
    默认行为：不开启并行传输
    
    > 注意：有些服务器禁止过高的并行数
    > 

