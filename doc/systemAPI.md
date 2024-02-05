- [任务提交接口](system.md)
    - [快速开始](system.md#快速开始)
    - [指令输出控制](system.md#指令输出控制)
    - [后台任务提交](system.md#后台任务提交)
    - [其他种类的任务提交器](system.md#其他种类的任务提交器)
    - [关闭任务提交器](system.md#关闭任务提交器)
- [SSH 任务提交器](systemSSH.md)
- [API 文档](systemAPI.md)
- [**⟶ 目录**](contents.md)

# API 文档

目前通用的任务提交器（[ISystemExecutor](../src/main/java/com/jse/system/ISystemExecutor.java)）提供了这些接口：

| 方法 | 输入 | 输出 | 说明 |
| ---- | ---- | ---- | ---- |
| `system(command, [outFilePath], [IOFiles])` | `command`: String，系统指令<br>`outFilePath`: String，可选，输出到文件的路径<br>`IOFiles`: [IIOFiles](../src/main/java/com/jse/iofile/IIOFiles.java)，可选，指令附带的输入输出文件 | 退出值 `exitValue`，int | 使用此任务提交器运行一个系统指令，<br>系统指令不保证和直接在控制台中执行效果一致，<br>一般不指定输出文件则会将输出输出到控制台，<br>关于附加输入输出文件参考 [SSH 任务提交器](systemSSH.md) 中的说明。 |
| `submitSystem(command, [outFilePath], [IOFiles])` | 同 `system` | 异步计算结果 `Future<Integer>` | 使用此任务提交器提交一个后台运行的系统指令，<br>详见 [后台任务提交](system.md#后台任务提交)。 |
| `system_str(command, [IOFiles])` | `command`: String，系统指令<br>`IOFiles`: [IIOFiles](../src/main/java/com/jse/iofile/IIOFiles.java)，可选，指令附带的输入输出文件 | 指令输出 `List<String>`，<br>按照行来分隔 | 使用此任务提交器运行一个系统指令，<br>并将输出放入`List<String>`。<br>此语句执行完成时此指令一定已经运行结束。 |
| `submitSystem_str(command, [IOFiles])` | 同 `system_str` | 异步计算结果 `Future<List<String>>` | 使用此任务提交器提交一个后台运行的系统指令，<br>并将输出放入`List<String>`。<br>使用 `get()` 获取到 `Future<List<String>>` 结果时此指令一定已经运行结束。 |
| `setNoSTDOutput([noSTDOutput=true])` | `noSTDOutput`: boolean，是否关闭标准输出 | 任务提交器本身 [ISystemExecutor](../src/main/java/com/jse/system/ISystemExecutor.java)，<br>用于链式调用 | 设置此任务提交器是否会输出标准输出，<br>详见 [指令输出控制](system.md#指令输出控制)。 |
| `setNoERROutput([noERROutput=true])` | `noERROutput`: boolean，是否关闭错误输出 | 任务提交器本身 [ISystemExecutor](../src/main/java/com/jse/system/ISystemExecutor.java)，<br>用于链式调用 | 设置此任务提交器是否会输出错误输出，<br>详见 [指令输出控制](system.md#指令输出控制)。 |


--------------------------------

<br>

而 [ISystemExecutor](../src/main/java/com/jse/system/ISystemExecutor.java)
又继承了 [IThreadPool](../src/main/java/com/jse/parallel/IThreadPool.java)，
对应接口在这里的含义为：

| 方法                  | 说明 |
| --------------------- | ---- |
| `shutdown()`          | 关闭此任务提交器，禁止再次提交指令，等待提交的后台指令全部运行完成后释放资源，详见 [关闭任务提交器](system.md#关闭任务提交器) |
| `shutdownNow()`       | 关闭此任务提交器，尝试强制中止已经提交的后台指令，然后释放资源，禁止再次提交指令 |
| `isShutdown()`        | 是否已经关闭了任务提交器（如果是可能还存在后台指令正在运行） |
| `isTerminated()`      | 此任务提交器是否已经完全关闭（如果是则不会有后台指令正在运行） |
| `awaitTermination()`  | 等待直到此提交器完全关闭 |
| `waitUntilDone()`     | 等待直到此任务提交器的后台指令全部运行完成（可能此时任务提交器没有关闭） |
| `nJobs()`             | 正在运行（或排队）的指令数目 |
| `nThreads()`          | 无意义 |


--------------------------------

<br>

所有的任务提交器位于 [jse.system](../src/main/java/com/jse/system)，
目前的各种任务提交器和说明如下：

| 名称                       | 简写      | 参数 | 说明 |
| -------------------------- | --------- | ---- | ---- |
| `LocalSystemExecutor`      | `Local`   | 无   | 本地的任务提交器，相当于 `Runtime.exec(command)` |
| `MPISystemExecutor`        | `MPIEXEC` | `processNum`，int | 本地执行 MPI 的任务提交器，相当于 `Local.system("mpiexec -np $processNum $command")` |
| `PowerShellSystemExecutor` | `PWSH`    | 无   | 本地在 powershell 中执行指令的任务提交器，相当于 `Local.system("powershell $command")` |
| `WSLSystemExecutor`        | `WSL`     | 无   | 本地在 [WSL](https://learn.microsoft.com/en-us/windows/wsl/) 中执行指令的任务提交器，相当于 `Local.system("wsl $command")` |
| `SRUNSystemExecutor`       | `SRUN`    | `taskNum`，int<br>`maxParallelNum`，int | 本地执行 srun（slurm 系统）的任务提交器，相当于 `Local.system("srun -n $taskNum $command")`，考虑了整个任务分配的资源数来避免触及到 slurm 系统的限制。详见 [SRUN 任务提交器](systemSRUN.md) |
| `SSHSystemExecutor`        | `SSH`     | 见 [SSH 任务提交器](systemSSH.md) | 向指定 SSH 服务器执行指令的任务提交器 |
