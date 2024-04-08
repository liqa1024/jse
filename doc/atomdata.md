- [通用原子结构操作](atomdata.md)
    - [结构创建](#结构创建)
    - [类型转换](#类型转换)
- [**⟶ 目录**](contents.md)

# 通用原子结构操作

jse 为所有的原子结构数据（`Lmpdat`，`Lammpstrj`，`POSCAR`）
提供了一种通用的原子数据结构类
[`jse.atom.IAtomData`](../src/main/java/jse/atom/IAtomData.java)，
从而实现通用的原子结构操作（如相互转换，修改类型，修改位置等）。


## 结构创建

对于晶体结构，可以使用
[`jse.atom.AbstractAtoms`](../src/main/java/jse/atom/AbstractAtoms.java) /
[`jse.atom.Structures`](../src/main/java/jse/atom/Structures.java)
直接创建：

```groovy
import jse.atom.Structures

// 晶格常数为 3.0，x 方向重复 2 次，y 3 次，z 4 次，BCC 结构
def data = Structures.BCC(3.0, 2, 3, 4) // #1. #3.

println('atom number: ' + data.natoms()) // #2.
// atom number: 48
println('atom at 10: ' + data.atom(10))
// atom at 10: {id: 11, type: 1, xyz: (3.000, 6.000, 0.000)}
println('box: ' + data.box())
// box: (6.000, 9.000, 12.00)
```

> 脚本位置：`jse example/atoms/init1`
> [⤤](../release/script/groovy/example/atoms/init1.groovy)
> 
> 1. 如果三个方向重复次数一样（如 4），则三个参数 `2, 3, 4`
> 可以简写只保留一个 `4`
> 
> 2. BCC 原胞有 2 个原子，因此一共有 `2 x 2x3x4 = 48` 个原子
> 
> 3. 预制的晶体结构有 BCC，FCC，HCP；其中 BCC 原胞有 2 个原子，
> FCC 和 HCP 为 4 个原子，其中 HCP 经过了正交化且可以单独设置高度
> （默认为密堆的高度）
>

> **注意**：
> 可以使用 `jse.atom.AbstractAtoms` 替换 `jse.atom.Structures`，
> 两者使用方法完全相同；使用 `AbstractAtoms` 主要用来强调返回的原子数据
> `IAtomData` 是一个抽象的引用数据，其没有经过值拷贝，并且不能直接修改。
> 

如果希望创建自定义的晶体结构，可以使用
`Structures.from` 方法，从给定的原胞进行创建：


```groovy
import jse.atom.Structures

// #1.
def cell = new AtomData(
    [new Atom(0.0, 0.0, 0.0, 1, 1)],
    new Box(2.0, 2.0, 2.0)
)

// #2.
def data = Structures.from(cell, 10)

println('atom number: ' + data.natoms())
// atom number: 1000
println('atom at 10: ' + data.atom(10))
// atom at 10: {id: 11, type: 1, xyz: (0.000, 2.000, 0.000)}
println('box: ' + data.box())
// box: (20.00, 20.00, 20.00)
```

> 脚本位置：`jse example/atoms/init2`
> [⤤](../release/script/groovy/example/atoms/init2.groovy)
> 
> 1. 手动创建一个简单立方的原胞，只有一个原子，位置为
> `(0.0, 0.0, 0.0)`，id 为 `1`，type 为 `1`；
> 模拟盒大小为 `(2.0, 2.0, 2.0)`
> 
> 2. 根据给定原胞扩展，xyz 方向都重复 10 次
> 

当然也可以使用各种数据库上下载得到的原胞，例如直接从
[Materials Project](https://next-gen.materialsproject.org/materials)
下载 POSCAR，或者利用其它软件将晶体结构转为 POSCAR，
然后通过 `POSCAR.read` 方法读取：

```groovy
import jse.atom.Structures
import jse.vasp.POSCAR

def cell = POSCAR.read('path/to/cell.poscar')
def data = Structures.from(cell, 3, 4, 5)
```

也可以通过
[`jse.atom.IAtomDataOperation`](../src/main/java/jse/atom/IAtomDataOperation.java)
中的 `repeat` 方法来创建一个周期重复的 `IAtomData`：

```groovy
import jse.atom.Structures
import jse.vasp.POSCAR

def cell = POSCAR.read('path/to/cell.poscar')
def data1 = Structures.from(cell, 3, 4, 5)
def data2 = cell.opt().repeat(3, 4, 5) // #1. #2.
```

> 1. 通过方法 `operation()` / `opt()` 来获取到指定 `IAtomData`
> 的计算器 `IAtomDataOperation`
> 
> 2. `repeat` 和 `Structures.from` 使用方法类型，
> 三个参数分别控制三个方向的重复次数，只指定一个参数则表示三个方向重复相同次数。
> 

> **注意**：
> 和 `Structures.from` 不同，使用 `repeat` 会进行值拷贝，
> 从而返回可以直接修改的原子数据 `ISettableAtomData`，
> 当然也会占用更多内存，可以根据需要选择使用。
> 


## 类型转换

直接通过 `AbstractAtoms` / `Structures`
创建的原子数据实际是通用的 `IAtomData`，
并不属于任何现有的原子数据结构（如 `Lmpdat`，`Lammpstrj`，`POSCAR`），
如果希望转为这些结构并保存，或者进行相关运算，
则可以使用相应类中的 `of` 方法：

```groovy
import jse.atom.Structures
import jse.lmp.Data
import jse.lmp.Dump
import jse.vasp.POSCAR

// #1.
def data = Structures.FCC(4.0, 5)

// #2.
def lmpdat = Data.of(data)
lmpdat.write('.temp/example/atoms/lmpdat')

// #3.
def dump = Dump.of(data)
dump.write('.temp/example/atoms/dump')

// #4.
def poscar = POSCAR.of(data)
poscar.write('.temp/example/atoms/poscar')
```

> 脚本位置：`jse example/atoms/transform`
> [⤤](../release/script/groovy/example/atoms/transform.groovy)
> 
> 1. 创建一个 FCC 原子结构。
> 
> 2. 通过 `Data.of` 转为 lammps 的 data 类型，
> 然后可以使用其中的 `write` 方法写入到文件。
>
> 3. 通过 `Dump.of` 转为 lammps 的 dump 类型，
> 然后可以使用其中的 `write` 方法写入到文件。
>
> 4. 通过 `POSCAR.of` 转为 vasp 的 POSCAR 类型，
> 然后可以使用其中的 `write` 方法写入到文件。
> 

> **注意**：
> 这些原子数据结构一般都存在额外的可选数据，
> 例如 lammps data 可以包含原子的质量，而
> POSCAR 可以包含原子种类的具体名称，
> 这些一般可以通过 `of` 后增加参数来实现：
>
> ```groovy
> def lmpdat = Data.of(data, 63.546)
> def poscar = POSCAR.of(data, 'Cu')
> ```
>



