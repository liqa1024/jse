- [通用原子结构操作](atomdata.md)
    - [结构创建](#结构创建)
    - [类型转换](#类型转换)
    - [通用运算](#通用运算)
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
> [⤤](../example/code/atoms/init1.groovy)
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
> [⤤](../example/code/atoms/init2.groovy)
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
> [⤤](../example/code/atoms/transform.groovy)
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


## 通用运算

可以通过 `operation` / `opt` 方法获取到此
`IAtomData` 对应的运算器，其中不可修改的 `IAtomData` 会返回
[`jse.atom.IAtomDataOperation`](../src/main/java/jse/atom/IAtomDataOperation.java)
，而可以修改的 `ISettableAtomData` 会返回
[`jse.atom.ISettableAtomDataOperation`](../src/main/java/jse/atom/ISettableAtomDataOperation.java)
。
相比 `IAtomDataOperation` 多提供了一系列带有 `2this` 的方法，
用于直接将修改应用到 `IAtomData` 自身而不经过冗余的值拷贝。

所有这些运算都是原子数据结构共有的，因此
`Lmpdat`，`Lammpstrj`，`POSCAR`
这些原子数据都可以直接使用。

- **`filter`**
    
    描述：根据通用的过滤器 `IFilter<IAtom>` 来过滤
    `IAtomData`，保留满足 `IFilter` 的原子；
    这里会在执行时就遍历过滤，并进行值拷贝。
    
    输入：`IFilter<IAtom>`，原子过滤器，一般为 lambda
    表达式，输入单个原子 `IAtom`，输出是否需要保留的 `boolean`
    
    输出：`ISettableAtomData`，新创建的过滤后的原子数据
    
    -----------------------------
    
- **`filterType`**
    
    描述：根据原子种类来过滤 `IAtomData`，只保留输入种类的原子。
    
    输入：`int`，选择保留的原子种类对应的数字
    
    输出：`ISettableAtomData`，新创建的过滤后的原子数据
    
    ```groovy
    // 两者等价：
    data.opt().filterType(type)
    data.opt().filter {atom -> atom.type()==type}
    ```
    
    -----------------------------
    
- **`refSlice`**
    
    描述：直接根据 `List<Integer>` 来引用切片 `IAtomData`。
    
    输入：根据输入类型重载，具体为：
    
    - `List<Integer>` / `int[]`，选择保留的原子的索引组成的数组
    - `IIndexFilter` 通用的索引过滤器，一般为 lambda 表达式，
    输入索引值，输出是否保留的 `boolean`
    
    输出：`IAtomData`，引用切片的原子数据
    
    -----------------------------
    
- **`map`**
    
    描述：根据通用的原子映射 `IUnaryFullOperator` 来遍历映射修改原子。
    
    输入1（可选）：`int` 希望的最小种类数目，
    默认会自动根据原本的种类数和映射后的原子种类设置
    
    输入2：`IUnaryFullOperator`，通用的原子映射，一般为 lambda 表达式，
    输入单个原子 `IAtom`，输出映射后的原子 `IAtom`
    
    输出：`ISettableAtomData`，新创建的修改后的原子数据
    
    -----------------------------
    
- **`mapType`**
    
    描述：根据原子种类映射 `IUnaryFullOperator` 来遍历映射修改原子种类。
    
    输入1（可选）：`int` 希望的最小种类数目，
    默认会自动根据原本的种类数和映射后的原子种类设置
    
    输入2：`IUnaryFullOperator`，原子种类映射，一般为 lambda 表达式，
    输入单个原子 `IAtom`，输出映射后的种类 `int`
    
    输出：`ISettableAtomData`，新创建的修改后的原子数据
    
    ```groovy
    // 两者等价：
    data.opt().mapType {atom -> atom.type() + 1}
    data.opt().map {atom -> new Atom(atom).setType(atom.type() + 1)}
    ```
    
    -----------------------------
    
- **`mapTypeRandom`**
    
    描述：根据给定的权重来随机分配原子种类，主要用于创建合金的初始结构。
    
    输入1（可选）：`Random` 自定义的随机数生成器，用于控制随机流
    
    输入2：`IVector` / `double...`，每个种类的对应的权重数组
    
    输出：`ISettableAtomData`，新创建的修改后的原子数据
    
    例子：`example/atoms/array`
    [⤤](../example/code/atoms/array.groovy)
    
    -----------------------------
    
- **`perturbXYZGaussian` / `perturbXYZ`**
    
    描述：使用高斯分布来随机扰动原子位置，
    如果有出边界的原子会自动根据周期边界条件移动到模拟盒内部。
    
    输入1（可选）：`Random` 自定义的随机数生成器，用于控制随机流
    
    输入2：`double`，高斯分布的标准差
    
    输出：`ISettableAtomData`，新创建的扰动后的原子数据
    
    -----------------------------
    
- **`wrapPBC` / `wrap`**
    
    描述：使用周期边界条件将出界的原子移动回到盒内。
    
    输出：`ISettableAtomData`，新创建的 `wrap` 后的原子数据
    
    -----------------------------
    
- **`repeat`**
    
    描述：将结构重复指定次数，不会对出边界的原子作特殊处理。
    
    输入1：`int` x 方向的重复次数
    
    输入2（可选）：`int`，y 方向的重复次数，默认为*输入1*
    
    输入3（可选）：`int`，z 方向的重复次数，默认为*输入1*
    
    输出：`ISettableAtomData`，新创建的重复后的原子数据
    
    -----------------------------
    
- **`slice`**
    
    描述：将结构切分成小块，会直接移除掉出边界的原子；
    结果按照 `x, y, z (i, j, k)` 的顺序依次遍历，
    也就是说，如果需要访问给定 `(i, j, k) ` 位置的切片结果，需要使用：
    
    ```groovy
    def list = data. opt().slice(Nx, Ny, Nz)
    int idx = i + j*Nx + k*Nx*Ny
    def subData = list[idx]
    ```
    
    来获取，同理对于给定的列表位置 `idx`， 需要使用：
    
    ```groovy
    int i = idx % Nx
    int j = idx / Nx % Ny
    int k = idx / Nx / Ny
    ```
    
    来获取相应的空间位置 `(i, j, k)`。
    
    输入1：`int` x 方向的切片次数
    
    输入2（可选）：`int`，y 方向的切片次数，默认为*输入1*
    
    输入3（可选）：`int`，z 方向的切片次数，默认为*输入1*
    
    输出：`List<ISettableAtomData>`，新创建的切片后的原子数据组成的列表

    -----------------------------

**以下这些 `2this` 方法仅 `ISettableAtomData` 可以使用**

> **注意**：
> 虽然 `POSCAR` 也支持这些方法，但 `POSCAR`
> 的原子种类修改会改变原子位置，因此不能在 `map2this`
> 中修改原子种类，以及 `mapType2this`
> 和 `mapTypeRandom2this` 实际上都不能使用。
> 

- **`map2this`**
    
    描述：根据通用的原子映射 `IUnaryFullOperator` 来遍历映射修改原子，
    并将修改的结果设置到自身。
    
    输入1（可选）：`int` 希望的最小种类数目，
    默认会自动根据原本的种类数和映射后的原子种类设置
    
    输入2：`IUnaryFullOperator`，通用的原子映射，一般为 lambda 表达式，
    输入单个原子 `IAtom`，输出映射后的原子 `IAtom`
    
    -----------------------------
    
- **`mapType2this`**
    
    描述：根据原子种类映射 `IUnaryFullOperator` 来遍历映射修改原子种类，
    并将修改的结果设置到自身。
    
    输入1（可选）：`int` 希望的最小种类数目，
    默认会自动根据原本的种类数和映射后的原子种类设置
    
    输入2：`IUnaryFullOperator`，原子种类映射，一般为 lambda 表达式，
    输入单个原子 `IAtom`，输出映射后的种类 `int`
    
    -----------------------------
    
- **`mapTypeRandom2this`**
    
    描述：根据给定的权重来随机分配原子种类，
    并将修改的结果设置到自身。
    
    输入1（可选）：`Random` 自定义的随机数生成器，用于控制随机流
    
    输入2：`IVector` / `double...`，每个种类的对应的权重数组
    
    -----------------------------
    
- **`perturbXYZGaussian2this` / `perturbXYZ2this`**
    
    描述：使用高斯分布来随机扰动原子位置，
    如果有出边界的原子会自动根据周期边界条件移动到模拟盒内部。
    
    输入1（可选）：`Random` 自定义的随机数生成器，用于控制随机流
    
    输入2：`double`，高斯分布的标准差
    
    -----------------------------
    
- **`wrapPBC2this` / `wrap2this`**
    
    描述：使用周期边界条件将出界的原子移动回到盒内。

