- [Lammps 相关](lammps.md)
    - [data 文件读写](#data-文件读写)
    - [dump 文件读写](#dump-文件读写)
    - [原子数据类型转换](#原子数据类型转换)
    - [原子数据修改](#原子数据修改)
    - [log 文件读取](#log-文件读取)
    - [原生运行 lammps](#原生运行-lammps)
        - [环境要求](#环境要求)
        - [使用方法](#使用方法)
        - [可选配置](#可选配置)
- [**⟶ 目录**](contents.md)

# Lammps 相关

几乎所有 lammps 相关的功能都位于 `jse.lmp` 包中，
涉及读写 lammps 输出的 data 和 dump 文件，
读取 lammps 输出的 log 文件，
以及原生运行 lammps 的接口。


## data 文件读写

jse 中使用 [`jse.lmp.Lmpdat`](../src/main/java/jse/lmp/Lmpdat.java) /
[`jse.lmp.Data`](../src/main/java/jse/lmp/Data.java)
来实现 lammps 的 data 文件读写：

- 输入脚本（`jse example/lmp/data`
  [⤤](../release/script/groovy/example/lmp/data.groovy)）：
    
    ```groovy
    import jse.lmp.Data
    
    def data = Data.read('lmp/data/CuFCC108.lmpdat')
    
    println('atom number: ' + data.natoms())
    println('masses: ' + data.masses())
    println('atom at 10: ' + data.atom(10))
    
    data.write('.temp/example/lmp/dataFCC')
    ```
    
- 输出：
    
    ```
    atom number: 108
    masses: 2-length Vector:
       63.55   91.22
    atom at 10: {id: 11, type: 1, xyz: (9.025, 0.000, 1.805)}
    ```

> 可以使用 `jse.lmp.Lmpdat` 替换 `jse.lmp.Data`，
> 两者使用方法完全相同；使用 `Lmpdat` 可以指定为 lammps 的数据，
> 用于区分其他的 `Data` 类。
>
> 可以通过 `mass(type)` 来获取指定 `type` 的质量，
> 如果没有设置质量则会得到 `Double.NaN`；
> 注意这里的 `type` 值和 lammps 保持一致统一从 **1** 
> 开始索引，而 `masses()` 获取的数组依旧是从 **0** 开始索引，
> 因此有：
>
> ```groovy
> assert data.mass(1) == data.masses()[0]
> ```
> 

-----------------------------

对于 lammps 的原子数据，其中的模拟盒还会存在一个下边界（`xlo`, `ylo`, `zlo`），
但是这对于计算是不必要的，因此这里通过 `atom(index)` 获取到的原子坐标，
以及通过 `box()` 获取到的模拟盒大小都是经过平移的（将 `xlo`, `ylo`, `zlo` 设为 0 ），
因此可能会和文件中的数据有所不同。

如果希望获得到原始的未经平移的数据，可以通过 `lmpBox()` 获得 lammps 格式的模拟盒信息，
以及通过 `positions()` 来直接获得存储原子位置的矩阵：

```groovy
println('box: ' + data.box())
println('lmpBox: ' + data.lmpBox())
```


## dump 文件读写

jse 中使用 [`jse.lmp.Lammpstrj`](../src/main/java/jse/lmp/Lammpstrj.java) /
[`jse.lmp.Dump`](../src/main/java/jse/lmp/Dump.java)
来实现 lammps 的 dump 文件读写：

- 输入脚本（`jse example/lmp/dump`
  [⤤](../release/script/groovy/example/lmp/dump.groovy)）：
    
    ```groovy
    import jse.lmp.Dump
    
    def dump = Dump.read('lmp/dump/CuFCC108.lammpstrj')
    
    println('frame number: ' + dump.size())
    def frame = dump[4]
    println('atom number: ' + frame.natoms())
    println('time step: ' + frame.timeStep())
    println('atom at 10: ' + frame.atom(10))
    
    dump.write('.temp/example/lmp/dumpFCC')
    ```
    
- 输出：
    
    ```
    frame number: 51
    atom number: 108
    time step: 40000
    atom at 10: {id: 47, type: 1, xyz: (10.65, 2.333, 0.9837), vxvyvz: (-3.044, -2.046, 3.698)}
    ```

> 可以使用 `jse.lmp.Lammpstrj` 替换 `jse.lmp.Dump`，
> 两者使用方法完全相同；使用 `Lammpstrj` 可以指定为 lammps 的数据，
> 用于区分其他的 `Dump` 类。
> 

-----------------------------

一般来说 lammps 的 dump 应该是一个表格数据而不一定包含原子数据，
这里可以通过 `dump[i].asTable()` 方法来将其转换成
jse 表格 `ITable`，从而使用表格的操作方式来处理 dump 数据：

- 输入脚本（`jse example/lmp/dumptable`
  [⤤](../release/script/groovy/example/lmp/dumptable.groovy)）：
    
    ```groovy
    import jse.lmp.Dump
    import static jse.code.UT.Math.*
    
    def dump = Dump.read('lmp/dump/CuFCC108.lammpstrj')
    def table = dump[4].asTable();
    
    println('origin x: ' + table['x'])
    table['x'].plus2this(10)
    println('new x: ' + table['x'])
    
    println('origin heads: ' + table.heads())
    table['rand'] = rand(table.nrows())
    println('new heads: ' + table.heads())
    
    dump.write('.temp/example/lmp/dumpSet')
    ```
    
- 输出：
    
    ```
    origin x: 108-length Vector:
       2.472   5.456   3.543   7.534  ...  1.653   2.865   1.121   8.152
    new x: 108-length Vector:
       12.47   15.46   13.54   17.53   ...   11.65   12.87   11.12   18.15
    origin heads: [id, type, x, y, z, vx, vy, vz]
    new heads: [id, type, x, y, z, vx, vy, vz, rand]
    ```

> 其中 `dump[i].asTable()` 会获取到表格数据的引用，
> 因此对获取到的表格进行修改会同时反应到 `dump` 中。
> 


## 原子数据类型转换

`jse.lmp.Lmpdat`/`jse.lmp.Data` 类以及
`jse.lmp.Lammpstrj`/`jse.lmp.Dump` 类都继承了
jse 中通用的原子数据接口
[`jse.atom.IAtomData`](../src/main/java/jse/atom/IAtomData.java)，
而 `Lmpdat` 和 `Lammpstrj` 都实现了通过 `IAtomData`
来初始化的方法 `of`，从而可以以此来实现相互转换：

- 输入脚本（`jse example/lmp/transform`
  [⤤](../release/script/groovy/example/lmp/transform.groovy)）：
    
    ```groovy
    import jse.lmp.Data
    import jse.lmp.Dump
    import static jse.code.CS.MASS
    
    def dump = Dump.read('lmp/dump/CuFCC108.lammpstrj')
    
    // 通过 `of` 来转换，转为 data 时可以这样来指定每个原子种类的质量
    def data = Data.of(dump[4], [MASS.Cu, MASS.Zr])
    println('atom number: ' + data.natoms())
    println('masses: ' + data.masses())
    
    data.write('.temp/example/lmp/dump2data')
    
    // 同样通过 `of` 来将单个或多个 data 转为 dump
    def dump1 = Dump.of(data)
    println('frame number of dump1: ' + dump1.size())
    def dump4 = Dump.of([data, dump[3], dump[0], dump.last()])
    println('frame number of dump4: ' + dump4.size())
    
    dump4.write('.temp/example/lmp/data2dump')
    ```
    
- 输出：
    
    ```
    atom number: 108
    masses: 2-length Vector:
       63.55   91.22
    frame number of dump1: 1
    frame number of dump4: 4
    ```

> `of` 方法直接接收 `IAtomData`，因此任何类型的原子数据（如 POSCAR，XDATCAR），
> 只要继承了 `IAtomData` 就可以通过此方法来转换。
> 


## 原子数据修改

`jse.lmp.Lmpdat`/`jse.lmp.Data` 类以及
`jse.lmp.Lammpstrj`/`jse.lmp.Dump` 类都继承了
jse 中通用的可修改的原子数据接口
[`jse.atom.ISettableAtomData`](../src/main/java/jse/atom/ISettableAtomData.java)，
从而可以使用 `ISettableAtomData` 提供的接口来修改原子数据：

- 输入脚本（`jse example/lmp/setatom`
  [⤤](../release/script/groovy/example/lmp/setatom.groovy)）：
    
    ```groovy
    import jse.lmp.Data
    
    def data = Data.read('lmp/data/CuFCC108.lmpdat')
    
    // 使用 `atom` 获取一个原子
    def atom10 = data.atom(10)
    println('origin atom10: ' + atom10)
    atom10.type = 2
    atom10.x = 3.14
    println('new atom at 10: ' + data.atom(10))
    
    // 使用 `atoms` 转为 List<IAtom> 后遍历所有原子
    for (atom in data.atoms()) atom.y += 10
    println('new atom10: ' + atom10)
    
    data.write('.temp/example/lmp/dataSet')
    ```
    
- 输出：
    
    ```
    origin atom10: {id: 11, type: 1, xyz: (9.025, 0.000, 1.805)}
    new atom at 10: {id: 11, type: 2, xyz: (3.140, 0.000, 1.805)}
    new atom10: {id: 11, type: 2, xyz: (3.140, 10.00, 1.805)}
    ```

> - **注意1:**
>   
>   这里通过 `atom(index)` 获取到的原子实际为 `data` 中原子数据的引用，
>   对任意一方的修改都会同时反应在对方（实际只有一份原子数据）。
>   
> - **注意2:**
>   
>   遍历所有原子需要先调用 `atoms()` 方法将原子数据转为 `List<IAtom>`。
>   
> - **注意3:**
>   
>   这里为了让代码简洁，使用了类似 `atom10.type = 2` 以及 `atom.y += 10`
>   之类的写法，实际 groovy 运行时会分别调用 `atom10.setType(2)` 以及
>   `atom.setY(atom.getY() + 10)`（groovy 会自动将 getter/setter
>   转换成成员变量的形式）。
>   
>   但是注意到 jse 内部并不是使用此标准（也就是所谓的 Java Bean 命名约定，
>   具体可以参考 [jse java 部分的代码规范](codeJava.md#gettersetter)），
>   因此 jse 不一定提供了完整的 getter/setter，因此许多属性依旧通过调用方法
>   （如 `data.natoms()`）来获取而不是调用成员变量（如 `data.natoms`）
>   的方式来获取。
>   
>   一般来说，对于简单的属性，jse 中也会提供一套 getter/setter
>   来方便 groovy 中的使用。
> 


## log 文件读取

jse 中使用 [`jse.lmp.Thermo`](../src/main/java/jse/lmp/Thermo.java) /
[`jse.lmp.Log`](../src/main/java/jse/lmp/Log.java)
来实现 lammps 的 log 文件读取：

- 输入脚本（`jse example/lmp/log`
  [⤤](../release/script/groovy/example/lmp/log.groovy)）：
    
    ```groovy
    import jse.lmp.Log
    
    def log = Log.read('lmp/log/CuFCC108.thermo')
    
    println('heads: ' + log.heads())
    println('volume: ' + log['Volume'])
    
    // 会保存成 csv 文件
    log.write('.temp/example/lmp/logFCC.csv')
    ```
    
- 输出：
    
    ```
    heads: [Step, Temp, Press, Volume, PotEng, KinEng, TotEng]
    volume: 51-length Vector:
       1270   1464   1484   1546  ...  1507   1498   1508   1475
    ```

> 可以使用 `jse.lmp.Thermo` 替换 `jse.lmp.Log`，
> 两者使用方法完全相同；使用 `Thermo` 可以指定为 lammps 的数据，
> 用于区分其他的 `Log` 类。
> 


## 原生运行 lammps

在 jse 中，除了通过 [任务提交](system.md) 的方式来运行 lammps，
还支持直接原生运行 lammps。
具体为通过 [jni](https://www.baeldung.com/jni) 
来调用 lammps 的动态库（如 `liblammps.so`），
然后通过类似 python 中的 lammps 包的方式来直接原生调用 lammps。

> **注意**：这里直接基于 jni 来调用 lammps
> 的动态库，而没有经过 python，因此效率和兼容性都会更高。
> 

### 环境要求

- **C & C++ 编译器**
    
    需要系统拥有 C/C++ 编译器，这里需要两者都有。
    
    例如对于 windows 需要
    [MSVC](https://visualstudio.microsoft.com/zh-hans/vs/features/cplusplus/)
    ，对于 linux 需要
    [GCC](https://gcc.gnu.org/)
    （当然也可以选择其他的编译器）
    
- [**CMake**](https://cmake.org/) (`>= 3.14`)
    
    为了实现跨平台，jse 将调用 lammps 库需要的 jni 源码直接封装到
    `jse-all.jar` 中，并在调用到原生方法时自动编译。
    为了保证不同平台下编译流程一致，以及能使用
    [CLion](https://www.jetbrains.com/clion/) 进行调试，
    这里使用 cmake 来管理这部分项目。
    
    这里借助了 cmake 的 `find_package` 方法来自动找到 jni 和 mpi 依赖，
    此功能至少需要 cmake 版本为 `3.14`。
    
- [**MPI**](mpi.md)（可选，建议）
    
    一般来说，为了实现高性能的 lammps 计算，都会使用 mpi 版本的 lammps，
    这里和 lammps 一样提供串行版本的支持。
    
    jse 会通过 cmake 的 `find_package(MPI)` 来自动检测是否存在 MPI 环境，
    并在成功检测到 MPI 时自动开启 MPI。
    
    例如对于 windows 可以使用
    [Microsoft MPI](https://www.microsoft.com/download/details.aspx?id=105289)
    （`msmpisdk.msi` 和 `msmpisetup.exe` 都需要安装），对于 linux 可以使用
    [MPICH](https://www.mpich.org/)
    （建议直接通过 `apt-get install mpich` 安装）
    
- **网络环境或已编译的 lammps 动态库**
    
    jse 会自动检测 `lib/lmp/native/build` 目录是否存在 lammps 动态库环境，
    并在检测失败后自动从网络下载 
    [`stable_2Aug2023_update2` 版本的 lammps](https://github.com/lammps/lammps/releases/tag/stable_2Aug2023_update2)
    并使用 cmake 编译（这当然会花费很多时间）。
    
    对于没有网络的环境，可以手动编译出 lammps 动态库，并将动态库放在
    `lib/lmp/native/build/lib`，头文件放在 `lib/lmp/native/build/includes`，
    具体为：
    
    ```
    build
        ├─lib
        │   └─liblammps.so
        └─includes
            └─lammps
                └─library.h
    ```
    
    > 可以参考 [lammps 官方文档](https://docs.lammps.org/Build_basics.html#build-the-lammps-executable-and-library) 
    > 来实现 lammps 动态库的编译
    > 


### 使用方法

在上述环境配置完成后，第一次调用 `jse.lmp.NativeLmp`
相关方法时会自动编译需要的 jni 库。
而后可以使用类似 `mpiexec -np 4 jse path/to/script`
的方法来通过 MPI 运行脚本中的 lammps。

> **注意**：
> 
> 1. 在 MPI 环境下编译 jni 库会导致多个进程同时访问相同目录，
> 最终导致编译失败（并且这是不可能从程序中预先知道的，因为这时还没有 MPI 环境！），
> 因此第一次运行 MPI 代码时需要在“串行”环境下运行，保证相关
> jni 库正常编译。
>
> 2. 在 windows 下，可能需要手动指定指定的脚本才能正常使用 mpiexec 运行，例如
> `mpiexec -np 4 jse.bat path/to/script`
> 

原生 lammps 类位于 [`jse.lmp.NativeLmp`](../src/main/java/jse/lmp/NativeLmp.java)，
其接口基本和 [python 的 lammps 包](https://docs.lammps.org/Python_module.html) 一致：

- **`<init>`**
    
    描述：`jse.lmp.NativeLmp` 的构造函数。
    
    输入1（可选）：`String[]`，传入 lammps 的参数，默认为
    `['-log', 'none']`
    
    输入2（可选）：`MPI.Comm`，lammps 使用的 MPI 通讯器，
    默认为 `MPI.Comm.WORLD`（当存在 MPI 环境时）
    
    输出：`NativeLmp`，创建的 lammps 对象
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    > 注意：*输入1* 和 *输入2* 的顺序可以调换，当 `String[]`
    > 作为最末尾的参数时可以使用可变参数 `String...`。
    >
    > 创建后记得在使用完成后手动调用 `shutdown()` 关闭 lammps 回收资源，
    > 或者使用 [*try-with-resources*](https://www.baeldung.com/java-try-with-resources)
    > 实现自动回收。
    > 
    > 由于 lammps 的特性，`NativeLmp` 线程不安全，并且要求所有方法都由相同的线程调用。
    >
    > jse 不会去尝试解析这个输入参数，因此如果手动设置了 log 的路径，
    > 可能需要手动调用 `UT.IO.validPath` 来将此路径合法化
    >（创建需要的文件夹）。
    > 
    
    -----------------------------
    
- **`version`**
    
    输出：`int`，使用的 lammps 的版本组成的整数
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`comm`**
    
    输出：`MPI.Comm`，此实例使用的 MPI 通讯器
    
    > 注意：对于没有 MPI 环境时会返回 `null`
    > 
    
    -----------------------------
    
- **`file`**
    
    描述：读取输入文件。
    
    输入：`String`，字符串表示的输入文件路径
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`command`**
    
    描述：执行单个 lammps 指令。
    
    输入：`String`，字符串表示的 lammps 指令
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`commands`**
    
    描述：执行多个 lammps 指令。
    
    输入：根据输入类型重载，具体为：
    
    - `String...`，使用可变参数参数输入的多个指令
    - `String`，由换行符（`\n`）分隔的多个指令组成的单个字符串
    
    -----------------------------
    
- **`atomNumber`/`natoms`**
    
    输出：`int`，此时的原子总数目
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`atomTypeNumber`/`ntypes`**
    
    输出：`int`，此时的原子种类数目
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`localAtomNumber`/`nlocal`**
    
    输出：`int`，此进程的原子数目
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`box`**
    
    输出：`jse.lmp.Box`，此时的模拟盒数据（带有下边界 `xlo`, `ylo`, `zlo`）
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`resetBox`**
    
    描述：重设模拟盒信息
    
    输入：各种设置模拟盒边界的参数（可以设置下边界 `xlo`, `ylo`, `zlo`）
    
    -----------------------------
    
- **`thermoOf`**
    
    描述：获取指定名称的热力学信息
    
    输入：`String`，字符串表示的热力学信息名称
    
    输出：`double`，此热力学信息的值
    
    -----------------------------
    
- **`settingOf`**
    
    描述：获取指定名称的 lammps 设置
    
    输入：`String`，字符串表示的设置名称，具体可以参考
    [lammps 官方文档](https://docs.lammps.org/Library_properties.html#_CPPv422lammps_extract_settingPvPKc)
    
    输出：`int`，此设置名称对应的值
    
    -----------------------------
    
- **`atomDataOf`**
    
    描述：获取指定名称的原子数据
    
    输入：`String`，字符串表示的原子数据类型，具体可以参考
    [lammps 官方文档](https://docs.lammps.org/Classes_atom.html#_CPPv4N9LAMMPS_NS4Atom7extractEPKc)
    
    输出：`RowMatrix`，此类型原子数据组成的行矩阵
    
    -----------------------------

- **`setAtomDataOf`**
    
    描述：设置获取指定名称的原子数据
    
    输入1：`String`，字符串表示的原子数据类型，具体可以参考
    [lammps 官方文档](https://docs.lammps.org/Classes_atom.html#_CPPv4N9LAMMPS_NS4Atom7extractEPKc)
    
    输入2：`RowMatrix`，此类型原子数据组成的行矩阵
    
    -----------------------------
    
- **`masses`**
    
    输出：`IVector`，每个种类原子质量组成的向量
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`masse`**
    
    获取指定种类的原子的质量
    
    输入：`int`，原子种类（从 1 开始）
    
    输出：`double`，此种类对应的质量
    
    -----------------------------
    
- **`lmpdat`/`data`**
    
    描述：获取此时的完成原子数据
    
    输入（可选）：`boolean`，是否关闭速度信息的输出，`true` 表示不输出速度，
    默认为 `false`（输出速度）
    
    输出：`Lmpdat`，此时的完整原子数据
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`loadLmpdat`/`loadData`**
    
    描述：加载输入的原子数据到 lammps 中
    
    输入：`Lmpdat`，需要加载的原子数据；
    对于 `loadData` 还支持 `IAtomData` 输入
    
    例子：`example/lmp/native`
    [⤤](../release/script/groovy/example/lmp/native.groovy)
    
    -----------------------------
    
- **`shutdown`**
    
    描述：关闭此 lammps 实例


### 可选配置

对于自定义 lammps 动态库的情况下，不同版本的 lammps 其接口的使用和支持程度也会有所区别，
jse 在 [`jse.lmp.NativeLmp.Conf`](../src/main/java/jse/lmp/NativeLmp.java)
中提供了一些可选的配置来适应这些情况。

可以在脚本中手动设置这些配置从而实现不同脚本使用不同的配置，
也可以通过环境变量的方式进行设置，从而实现全局的默认配置。

- **`LMP_HOME`**
    
    描述：检测 lammps 动态库的目录。
    
    类型：`String`
    
    默认值：`'lib/lmp/native/build'`
    
    环境变量名称：`JSE_LMP_HOME`
    
    -----------------------------
    
- **`LMP_TAG`**
    
    描述：自动下载的 lammps 的版本。
    
    类型：`String`
    
    默认值：`'stable_2Aug2023_update2'`
    
    环境变量名称：`JSE_LMP_TAG`
    
    -----------------------------
    
- **`CMAKE_SETTING`**
    
    描述：自定义构建 lammps 的 cmake 参数设置，
    会在构建时使用 `-D ${key}=${value}` 传入。
    
    类型：`Map<String, String>`
    
    默认值：`{}`
    
    环境变量名称：无
    
    -----------------------------
    
- **`CMAKE_C_COMPILER`**
    
    描述：自定义使用 cmake 构建 lammps 以及 jni 部分使用的 C 编译器。
    
    类型：`String`
    
    默认值：`null`（cmake 自动检测）
    
    环境变量名称：`JSE_CMAKE_C_COMPILER_LMP` / `JSE_CMAKE_C_COMPILER`
    
    -----------------------------
    
- **`CMAKE_CXX_COMPILER`**
    
    描述：自定义使用 cmake 构建 lammps 以及 jni 部分使用的 C++ 编译器。
    
    类型：`String`
    
    默认值：`null`（cmake 自动检测）
    
    环境变量名称：`JSE_CMAKE_CXX_COMPILER_LMP` / `JSE_CMAKE_CXX_COMPILER`
    
    -----------------------------
    
- **`CMAKE_C_FLAGS`**
    
    描述：自定义使用 cmake 构建 lammps 以及 jni 部分使用的 C 的 flags。
    
    类型：`String`
    
    默认值：`null`（不添加 flags）
    
    环境变量名称：`JSE_CMAKE_C_FLAGS_LMP` / `JSE_CMAKE_C_FLAGS`
    
    -----------------------------
    
- **`CMAKE_CXX_FLAGS`**
    
    描述：自定义使用 cmake 构建 lammps 以及 jni 部分使用的 C++ 的 flags。
    
    类型：`String`
    
    默认值：`null`（不添加 flags）
    
    环境变量名称：`JSE_CMAKE_CXX_FLAGS_LMP` / `JSE_CMAKE_CXX_FLAGS`
    
    -----------------------------
    
- **`CMAKE_C_COMPILER_LMPJNI`**
    
    描述：自定义使用 cmake 构建 jni 部分使用的 C 编译器，
    会覆盖 `CMAKE_C_COMPILER`。
    
    类型：`String`
    
    默认值：`null`（cmake 自动检测）
    
    环境变量名称：`JSE_CMAKE_C_COMPILER_LMPJNI`
    
    -----------------------------
    
- **`CMAKE_CXX_COMPILER_LMPJNI`**
    
    描述：自定义使用 cmake 构建 jni 部分使用的 C++ 编译器，
    会覆盖 `CMAKE_CXX_COMPILER`。
    
    类型：`String`
    
    默认值：`null`（cmake 自动检测）
    
    环境变量名称：`JSE_CMAKE_CXX_COMPILER_LMPJNI`
    
    -----------------------------
    
- **`CMAKE_C_FLAGS_LMPJNI`**
    
    描述：自定义使用 cmake 构建 jni 部分使用的 C 的 flags。
    会覆盖 `CMAKE_C_FLAGS`。
    
    类型：`String`
    
    默认值：`null`（不添加 flags）
    
    环境变量名称：`JSE_CMAKE_C_FLAGS_LMPJNI`
    
    -----------------------------
    
- **`CMAKE_CXX_FLAGS_LMPJNI`**
    
    描述：自定义使用 cmake 构建 jni 部分使用的 C++ 的 flags。
    会覆盖 `CMAKE_CXX_FLAGS`。
    
    类型：`String`
    
    默认值：`null`（不添加 flags）
    
    环境变量名称：`JSE_CMAKE_CXX_FLAGS_LMPJNI`
    
    -----------------------------
    
- **`USE_MIMALLOC`**
    
    描述：对于 jni 部分，是否使用 [mimalloc](https://github.com/microsoft/mimalloc)
    来加速 C 的内存分配，这对于 java 数组和 C 数组的转换很有效。
    
    类型：`boolean`
    
    默认值：`true`
    
    环境变量名称：`JSE_USE_MIMALLOC_LMP` / `JSE_USE_MIMALLOC`
    
    > **注意**：mimalloc 的编译需要较高版本的编译器，在一些旧平台上可能会编译失败
    > 
    
    -----------------------------
    
- **`REDIRECT_LMP_LIB`**
    
    描述：重定向 lammps 动态库的路径，
    设置后会完全关闭 jse 关于 lammps 动态库的查找和构建并直接使用此动态库。
    
    类型：`String`
    
    默认值：`null`
    
    环境变量名称：`JSE_REDIRECT_LMP_LIB`
    
    -----------------------------
    
- **`REDIRECT_LMP_LLIB`**
    
    描述：重定向编译 lammps jni 部分用来连接 lammps 库的路径，
    默认为 `REDIRECT_LMP_LIB`。
    
    类型：`String`
    
    默认值：`null`
    
    环境变量名称：`JSE_REDIRECT_LMP_LLIB`
    
    -----------------------------
    
- **`REDIRECT_LMPJNI_LIB`**
    
    描述：重定向 lammps jni 动态库的路径，
    设置后会完全关闭 jse 关于 lammps jni 动态库的查找和构建并直接使用此动态库。
    
    类型：`String`
    
    默认值：`null`
    
    环境变量名称：`JSE_REDIRECT_LMPJNI_LIB`
    
    -----------------------------
    
- **`REBUILD`**
    
    描述：是否在检测到库文件时依旧重新编译 lammps，在需要修改 lammps 包时很有用。
    
    类型：`boolean`
    
    默认值：`false`
    
    环境变量名称：无
    
    -----------------------------
    
- **`CLEAN`**
    
    描述：在编译 lammps 之前是否进行 clean 操作。
    
    类型：`boolean`
    
    默认值：`false`
    
    环境变量名称：无
    
    -----------------------------
    
- **`IS_OLD`**
    
    描述：是否是旧版本的 lammps，具体来说大致为 18Sep2020 之前版本的 lammps，
    开启后会使用更老的 api。
    
    类型：`boolean`
    
    默认值：`false`
    
    环境变量名称：`JSE_LMP_IS_OLD`
    
    -----------------------------
    
- **`HAS_EXCEPTIONS`**
    
    描述：lammps 是否有 exception 相关接口，对于新版的 lammps 总是存在，
    但对于旧版可能不会存在，关闭后可以保证编译通过。
    
    类型：`boolean`
    
    默认值：`true`
    
    环境变量名称：`JSE_LMP_HAS_EXCEPTIONS`
    
    > **注意**：关闭后如果出现 lammps 的错误则不会重定向为 java 的错误，
    > 从而不会有 java 的栈信息，使得调试更加困难。
    > 
    > 当然由于 lammps 部分依旧是 C/C++ 的程序，实际大部分错误并不会抛出，
    > 而是直接导致程序崩溃，因此这个选项的实际意义并不算大。
    > 
    
    -----------------------------
    
- **`EXCEPTIONS_NULL_SUPPORT`**
    
    描述：lammps 的 lammps_has_error 接口是否有 NULL 支持，
    对于较旧的版本并不支持。
    
    类型：`boolean`
    
    默认值：`true`
    
    环境变量名称：`JSE_LMP_EXCEPTIONS_NULL_SUPPORT`
    
    > **注意**：即使在较新的 lammps 版本中，存在 exception 相关接口，
    > 但是不支持 `null` 输入（即使这在最近 lammps 官方文档中已经明确支持，
    > 并且没有给出支持的最小版本），
    > 因此可以顺利通过编译，但是在调用 `shutdown()`
    > 后会因为检测`null` 输入的错误导致崩溃，导致无法得到正确的报错信息。
    > 


