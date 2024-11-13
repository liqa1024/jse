- [VASP 相关](vasp.md)
    - [POSCAR 文件读写](#poscar-文件读写)
    - [XDATCAR 文件读写](#xdatcar-文件读写)
    - [原子数据类型转换](#原子数据类型转换)
    - [原子数据修改](#原子数据修改)
- [**⟶ 目录**](contents.md)

# VASP 相关

几乎所有 vasp 相关的功能都位于 `jse.vasp` 包中，
涉及读写 vasp 输入的 POSCAR 和输出的 XDATCAR 文件。


## POSCAR 文件读写

jse 中使用 [`jse.vasp.POSCAR`](../src/main/java/jse/vasp/POSCAR.java)
来实现 vasp 的 POSCAR 文件读写：

- 输入脚本（`jse example/vasp/poscar`
  [⤤](../example/code/vasp/poscar.groovy)）：
    
    ```groovy
    import jse.vasp.POSCAR
    
    def poscar = POSCAR.read('vasp/poscar/CuFCC108.poscar')
    
    println('atom number: ' + poscar.natoms())
    println('type names: ' + poscar.typeNames())
    println('atom at 10: ' + poscar.atom(10))
    
    poscar.write('.temp/example/vasp/poscarFCC')
    ```
    
- 输出：
    
    ```
    atom number: 108
    type names: [Cu, Zr]
    atom at 10: {id: 11, type: 1, xyz: (9.025, 0.000, 1.805)}
    ```

> 这里不会自动通过 POSCAR 第一行的 comment
> 来读取原子种类信息，但是会保留此信息。
> 
> 可以通过 `atomNumber(type)` / `natoms(type)`
> 来获取指定 `type` 的原子数量，以及 `typeName(type)`
> 来获取指定 `type` 的种类名称；
> 注意这里的 `type` 值和 lammps 保持一致统一从 **1** 
> 开始索引，而 `typeNames()` 获取的数组依旧是从 **0** 开始索引，
> 因此有：
>
> ```groovy
> assert poscar.typeName(1) == poscar.typeNames()[0]
> ```
> 
> `POSCAR` 也支持通过 `mass(type)` 来获取指定 `type` 的质量，
> 这里实际会通过内部的数据库 `jse.code.CS.MASS` 来查询指定 `type`
> 对应名称的质量，如果没有设置名称或者没有找到对应的元素则会得到 
> `Double.NaN`。
> 


## XDATCAR 文件读写

jse 中使用 [`jse.vasp.XDATCAR`](../src/main/java/jse/vasp/XDATCAR.java)
来实现 vasp 的 XDATCAR 文件读写：

- 输入脚本（`jse example/vasp/xdatcar`
  [⤤](../example/code/vasp/xdatcar.groovy)）：
    
    ```groovy
    import jse.vasp.XDATCAR
    
    def xdatcar = XDATCAR.read('vasp/xdatcar/CuFCC108.xdatcar')
    
    println('frame number: ' + xdatcar.size())
    def frame = xdatcar[4]
    println('atom number: ' + frame.natoms())
    println('type names: ' + frame.typeNames())
    println('atom at 10: ' + frame.atom(10))
    
    xdatcar.write('.temp/example/vasp/xdatcarFCC')
    ```
    
- 输出：
    
    ```
    frame number: 21
    atom number: 108
    type names: [Cu, Zr]
    atom at 10: {id: 11, type: 1, xyz: (5.211, 8.424, 1.354)}
    ```

> 其中 `xdatcar[i]` 会直接获取到一个 `POSCAR` 的引用，
> 类型和一般的 `POSCAR` 一致，
> 但是在进行部分涉及数据修改操作时会提示先进行一次 `copy()`
> 再进行操作，以免对整个 `XDATCAR` 进行了意外的操作。
> 


## 原子数据类型转换

`jse.vasp.POSCAR` 类以及 `jse.vasp.XDATCAR` 类都继承了
jse 中通用的原子数据接口
[`jse.atom.IAtomData`](../src/main/java/jse/atom/IAtomData.java)，
而 `POSCAR` 和 `XDATCAR` 都实现了通过 `IAtomData`
来初始化的方法 `of`，从而可以以此来实现相互转换：

- 输入脚本（`jse example/vasp/transform`
  [⤤](../example/code/vasp/transform.groovy)）：
    
    ```groovy
    import jse.vasp.XDATCAR
    import jse.lmp.Dump
    
    def xdatcar = XDATCAR.read('vasp/xdatcar/CuFCC108.xdatcar')
    // 通过 `of` 来转换
    def dump = Dump.of(xdatcar)
    dump.write('.temp/example/vasp/xdatcar2dump')
    
    dump = Dump.read('lmp/dump/CuFCC108.lammpstrj')
    // 通过 `of` 来转换，转为 XDATCAR/POSCAR 时可以这样来指定种类名称
    xdatcar = XDATCAR.of(dump, 'Cu', 'Zr')
    xdatcar.write('.temp/example/vasp/dump2xdatcar')
    ```

> `Dump` 可能会发生原子顺序改变，并存储了一个 `id` 属性来标记原子，
> 而 `XDATCAR` 中不会发生这个情况，因此在 `Dump` 转为 `XDATCAR`
> 时会自动按照最开始的帧的 `id` 调整原子的排序来保证一致。
>
> 由于 `XDATCAR` 只存储一个模拟盒信息，因此会直接忽略掉后续的模拟盒信息。
> 


## 原子数据修改

`jse.vasp.POSCAR` 类以及 `jse.vasp.XDATCAR` 类都继承了
jse 中通用的可修改的原子数据接口
[`jse.atom.ISettableAtomData`](../src/main/java/jse/atom/ISettableAtomData.java)，
从而可以使用 `ISettableAtomData` 提供的接口来修改原子数据，
因此使用方法和 [lammps 原子数据修改](lammps.md#原子数据修改)
基本一致。

注意由于 `POSCAR`/`XDATCAR` 不直接存储每个原子对应的种类，
而是通过指定每个种类的原子数目间接确定原子种类，
因此在修改某个原子的种类时（jse 依旧支持进行修改）
会调整其他部分原子的种类，因此**不要**遍历修改原子种类，
可以通过转为 `jse.lmp.Data` 来进行此操作：

- **不要使用：**
    ```groovy
    for (atom in poscar.asList()) {
        atom.type = 1
    }
    ```
    
- **改为使用：**
    
    ```groovy
    def data = Data.of(poscar)
    for (atom in data.asList()) {
        atom.type = 1
    }
    poscar = POSCAR.of(data, 'name', 'of', 'types')
    ```


