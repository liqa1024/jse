- [任务提交](system.md)
    - [基本功能](#基本功能)
    - [输出控制](#输出控制)
    - [后台任务提交](#后台任务提交)
    - [自定义任务提交器种类](#自定义任务提交器种类)
    - [SSH 任务提交](#ssh-任务提交)
        - [基本使用](#基本使用)
        - [免密连接](#免密连接)
        - [SSH 参数 API](#ssh-参数-api)
    - [srun/yhrun 任务提交](#srun-任务提交)
        - [基本使用](#e59fbae69cace4bdbfe794a8-1)
        - [多任务提交](#多任务提交)
        - [slurm 环境变量获取](#slurm-环境变量获取)
- [**⟶ 目录**](contents.md)

# 任务提交

jse 通过提交系统指令的方式来提交任务，类似
[matlab 的 system 方法](https://ww2.mathworks.cn/help/matlab/ref/system.html)。
例如对于提交 lammps 任务，则通过类似方法
`system('lmp_mpi -in path/to/in/file')` 即可执行系统指令来运行 lammps。


## 基本功能

一般使用需要导入 [`jse.code.OS`](../src/main/java/jse/code/OS.java)
中的静态方法，然后通过 `system()` 方法来执行系统指令。

在 linux 系统下会将指令解释为 [bash 指令](https://wiki.archlinux.org/title/Bash)，
在 windows 下则会解释为 [powershell 指令](https://learn.microsoft.com/en-us/powershell/scripting/overview?view=powershell-7.4)。
一般来说两者可以使用相同的指令。

- 输入脚本（`jse code/system/basic`
  [⤤](../example/code/system/basic.groovy)）：
    
    ```groovy
    import static jse.code.OS.*
    
    def exitCode = system('echo 123456')
    println("exitCode: $exitCode")
    ```
    
- 输出：
    
    ```
    123456
    exitCode: 0
    ```

> `jse.code.OS` 中的静态方法在 shell 模式下是默认导入的，
> 因此在 shell 模式下可以直接执行 `system()` 方法而不需要导入。
> 

### 修改工作目录

默认情况下，任务提交器的工作目录和 jse 的工作目录一致，
如果希望修改工作目录，可以通过 `EXEC.setWorkingDir()`
来修改工作目录：

```groovy
EXEC.setWorkingDir('lmp') // 修改工作目录到 lmp 文件夹
EXEC.setWorkingDir(null) // 清空工作目录修改，现在工作目录和 jse 的工作目录一致
```


## 输出控制

默认情况下，一般的任务提交器会将指令的输出直接输出到控制台，
这里支持将其输出到指定文件（不通过系统的重定向功能），
或者作为程序中的字符串使用，以及关闭输出流。

### 输出到文件

如果希望指令输出到指定路径的文件中，则需要在输入参数中指定输出路径：

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

有时不希望保留指令的输出，可以通过 `EXEC.setNoSTDOutput()` 和
`EXEC.setNoERROutput()` 来分别关闭标准输出和错误输出：

```groovy
EXEC.setNoSTDOutput() // 关闭标准输出
EXEC.setNoERROutput() // 关闭错误输出
EXEC.setNoSTDOutput(false).setNoERROutput(false) // 重新打开标准输出和错误输出，支持链式调用
```

--------------------------------

关于输出控制的实例，可以参看脚本 `example/system/output`
[⤤](../example/code/system/output.groovy)。


## 后台任务提交

jse 支持将任务提交到后台运行，而后继续进行后续运算（异步执行），
这里提供 `submitSystem()` 方法来实现这个功能：

- 输入脚本（`jse code/system/submit1`
  [⤤](../example/code/system/submit1.groovy)）：
    
    ```groovy
    import static jse.code.OS.*
    
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
> [⤤](../example/code/system/submit2.groovy)。
> 


## 自定义任务提交器种类

所有的任务提交器都位于包 `jse.system`，可以根据需要创建一个自定义的任务提交器，
而不是 jse 内部使用的全局任务提交器。

例如在 windows 下，如果不希望使用默认情况下的 powershell 来执行指令，
可以创建一个更加经典的 cmd 类型的任务提交器：

- 输入脚本（`jse code/system/custom`
  [⤤](../example/code/system/custom.groovy)）：
    
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

### 基本使用

这里需要通过 [`jse.system.SSH`](../src/main/java/jse/system/SSH.java)
来创建一个 ssh 任务提交器。
由于可选的输入参数较多，这里使用一个 `Map` 作为输入参数，
将 ip 地址，用户名等参数构造成 `Map` 后传入 ssh；
然后使用类似 [自定义任务提交器种类](#自定义任务提交器种类)
的方式创建一个 ssh 任务提交器，并执行任务即可：


- 输入脚本（`jse code/system/ssh1`
  [⤤](../example/code/system/ssh1.groovy)）：
    
    ```groovy
    import jse.system.SSH
    
    // 替换成所需连接的远程服务器的 ip，用户名，以及密码
    try (def ssh = new SSH(
        hostname: '127.0.0.1',
        username: 'admin',
        password: '123456'
    )) {
        ssh.system('echo 123456')
        ssh.system('hostname')
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
> [⤤](../example/code/system/ssh3.groovy)。
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


## srun 任务提交

jse 通过 srun 的任务提交器
[`jse.system.SRUN`](../src/main/java/jse/system/SRUN.java)
和 slurm 的相关环境变量
[jse.code.OS.Slurm](../src/main/java/jse/code/OS.java)
的结合使用来实现系统的 srun 任务提交。

### 基本使用

jse 对于在 slurm 环境中提交任务做了专门适配，
主要针对通过 `sbatch` 提交运行 *jse 脚本*，
然后通过 jse 执行 `srun` 来运行其他脚本的情况
（可以避免编写 shell 脚本）：

- 一般情况（提交 shell 脚本，然后 shell 脚本中嵌套运行 jse 脚本）
    
    `sbatch` 的提交脚本（`job.sh`，提交指令 `sbatch job.sh`）：
    
    ```shell
    #!/bin/bash
    #SBATCH -p test -n 4
    
    jse main.groovy
    ```
    
    实际运行的 jse 脚本（`main.groovy`）：
    
    ```groovy
    import static jse.code.OS.*
    
    system('srun lmp_mpi -in path/to/lmp/in/file')
    ```
    
- 通过 *shebang line* 直接提交 jse 脚本的情况：
    
    `sbatch` 的提交脚本（`job.groovy`，提交指令 `sbatch -p test -N 1 job.groovy`）：
    
    ```groovy
    #!/usr/bin/env jse
    import static jse.code.OS.*
    
    system('srun -n 4 lmp_mpi -in path/to/lmp/in/file')
    ```
    
    > 由于 groovy 中使用 `//` 表示行注释，而不是 `#`，因此不能直接使用
    > `#SBATCH -p test -n 4` 注释来指定分区和并行数。
    >

> *jse 脚本*：指通过 jse 运行的 groovy 脚本或者 python 脚本
> 

可以（并且**强烈建议**）使用 [`jse.system.SRUN`](../src/main/java/jse/system/SRUN.java)
来省略指令中的 `srun` / `srun -n 4` 部分：

```groovy
#!/usr/bin/env jse
import jse.system.SRUN

try (def srun = new SRUN(4)) { // #1. #2.
    srun.system('lmp_mpi -in path/to/lmp/in/file')
}
```

> 脚本名称为 `job.groovy` 时，提交指令 `sbatch -p test -N 1 job.groovy`。
> 
> 1. 获取一个使用 4 核心的 `srun` 任务提交器。
> 
> 2. 会全局申请使用的 slrum 资源并检测资源是否足够，
> 使用 *try-with-resources* 写法在使用完成后自动回收资源，以供后续使用。
>  

这种写法可以在 *绝大情况下* 自动检测到资源分配错误的问题，
并避免触发 slurm 系统自带的 nodebusy。

> *绝大情况下*：需要仅使用 jse 的 `srun` 任务提交器 `jse.system.SRUN` /
> `jse.system.SRUNSystemExecutor` 来提交 `srun` 任务，并且不出现嵌套情况。
> 

### 多任务提交

有时希望一个 `sbatch` 任务中包含多个并行的 `srun` 子任务，
例如通过 `sbatch -N 4 job.groovy` 来提交一个使用 4 个节点的任务，
而后每个子任务使用一个节点并且并行执行。

使用原始的 `srun` 指令提交一般来说不会是希望的结果：

- 直接使用 `srun -n 20`（假设每节点核数为 20）提交任务，默认 slurm
会将负载**平均分配到所有节点上**，也就是这个任务会使用所有节点，而每个节点使用 5 个核；
所有任务都变成了跨节点任务，严重影响性能。

- 根据 slurm 版本不同，一般需要在 `srun` 中手动指定使用的节点列表，
即需要使用 `srun -n 20 -N 1 --nodelist cnxxx` 来提交任务，
否则这几个子任务很可能会抢占同一个节点，导致触发 slurm 系统自带的 nodebusy；
而节点列表的获取是个较为困难的任务，特别是在 shell 脚本下。

jse 的 `srun` 任务提交器 `jse.system.SRUN` 内部自动处理好了上述问题，
因此只需要通过 `submitSystem` 提交后台任务即可实现多个并行的 `srun` 子任务的提交：

```groovy
#!/usr/bin/env jse
import jse.system.SRUN

try (def srun = new SRUN(20, 4)) { // #1. #2.
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file1 -log path/to/lmp/log1 -screen none') // #3.
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file2 -log path/to/lmp/log2 -screen none')
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file3 -log path/to/lmp/log3 -screen none')
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file4 -log path/to/lmp/log4 -screen none')
} // #4.
println('ALL JOBS FINISHED')
```

> 脚本名称为 `job.groovy` 时，提交指令 `sbatch -p test -N 4 job.groovy`。
> 
> 1. 获取一个使用 20 核心并行数为 4 的 `srun` 任务提交器。
> 
> 2. 会全局申请使用的 slrum 资源（申请 4 次 20 核心的任务资源，优先保证在同一个节点上）
> 并检测资源是否足够，使用 *try-with-resources* 写法在使用完成后自动回收资源，以供后续使用。
>
> 3. 提交一个 20 核心的 `lmp_mpi` 任务并放在后台，会优先保证运行在同一个节点上。
>
> 4. 代码块结束后自动等待 `srun` 中的后台任务完成，完成后回收资源。
>

### slurm 环境变量获取

一般希望通过获取 slurm 的环境变量，从而编写更加鲁棒的代码；
例如，上述的 [多任务提交](#多任务提交) 可能会希望获取到每个节点的核心数，
以及 `sbatch` 提交时具体分配了多少节点数，从而避免资源分配失败
（当然此时首先会通过 jse 检测能否分配资源，因此不会是致命的）：

```groovy
#!/usr/bin/env jse
import jse.system.SRUN
import static jse.code.OS.*

if (Slurm.NODE_LIST.size() != 4) {
    System.err.println('NODE NUMBER MUST BE 4!')
    return
}
try (def srun = new SRUN(Slurm.CORES_PER_NODE, 4)) {
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file1 -log path/to/lmp/log1 -screen none')
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file2 -log path/to/lmp/log2 -screen none')
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file3 -log path/to/lmp/log3 -screen none')
    srun.submitSystem('lmp_mpi -in path/to/lmp/in/file4 -log path/to/lmp/log4 -screen none')
}
println('ALL JOBS FINISHED')
```

下面列出 `jse.code.OS.Slurm` 已经存在的环境变量，如果需要其余变量可以通过
`OS.env("$nameOfEnv")` 来获取：

- **`IS_SLURM`**：
    
    类型：`boolean`
    
    描述：是否是 slurm 环境。
    
    例子：`true`
    
    -----------------------------
    
- **`IS_SRUN`**：
    
    类型：`boolean`
    
    描述：是否是 `srun` 执行的环境；
    直接 `sbatch` 提交运行下，此值为
    `false`，而通过 `srun` 运行时则为 `true`；
    可以用来检测是否发生了嵌套提交 `srun`，
    对此为了保证兼容 jse 内部只会进行警告而不会中断运行。
    
    例子：`false`
    
    -----------------------------
    
- **`PROCID`**：
    
    类型：`int`
    
    描述：`SLURM_PROCID`，当前 mpi 秩号。
    
    例子：`0`
    
    -----------------------------
    
- **`NTASKS`**：
    
    类型：`int`
    
    描述：`SLURM_NTASKS`，任务总数。
    
    例子：`20`
    
    -----------------------------
    
- **`CORES_PER_NODE`**：
    
    类型：`int`
    
    描述：`SLURM_JOB_CPUS_PER_NODE`，每个节点上的 cpu 核心数，
    一般情况下可能会出现不同节点 cpu 核心数不同的情况，这里简单处理认为都相同，
    会选取最小值保证兼容。
    
    例子：`20`
    
    -----------------------------
    
- **`CORES_PER_TASK`**：
    
    类型：`int`
    
    描述：`SLURM_CPUS_PER_TASK`，每个任务的 cpu 核心数，
    只有 `-c` / `--cpus-per-task` 选项设定时才存在。
    
    例子：`20`
    
    -----------------------------
    
- **`JOB_ID`**：
    
    类型：`int`
    
    描述：`SLURM_JOB_ID`，作业号。
    
    例子：`2180019`
    
    -----------------------------
    
- **`STEP_ID`**：
    
    类型：`int`
    
    描述：`SLURM_STEP_ID`，当前作业的作业步号。
    
    例子：`0`
    
    -----------------------------
    
- **`NODEID`**：
    
    类型：`int`
    
    描述：`SLURM_NODEID`，当前节点的相对节点号。
    
    例子：`0`
    
    -----------------------------
    
- **`NODENAME`**：
    
    类型：`String`
    
    描述：`SLURMD_NODENAME`，任务运行的节点名。
    
    例子：`'cn0'`
    
    -----------------------------
    
- **`NODE_LIST`**：
    
    类型：`List<String>`
    
    描述：`SLURM_NODELIST`，分配的节点列表。
    
    例子：`['cn0', 'cn1']`

