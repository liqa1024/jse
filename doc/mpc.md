- [原子结构参量计算](mpc.md)
    - [参量计算器初始化](#参量计算器初始化)
    - [RDF 和 SF 的计算](#rdf-和-sf-的计算)
    - [键角序参量的计算](#键角序参量的计算)
    - [Voronoi 分析](#voronoi-分析)
- [**⟶ 目录**](contents.md)

# 原子结构参量计算

jse 中使用 [`jse.atom.MPC`](../src/main/java/jse/atom/MPC.java) /
[`jse.atom.MonatomicParameterCalculator`](../src/main/java/jse/atom/MonatomicParameterCalculator.java)
来实现原子结构的参量计算，两者完全一致，
`MPC` 只是 `MonatomicParameterCalculator` 的简称。


## 参量计算器初始化

jse 中可以通过构造函数 `<init>` 直接创建一个参数计算器，
也可以通过静态方法 `withOf` 来使用一个自动关闭的参数计算器。

- **`<init>`**
    
    描述：`jse.atom.MPC` 的构造函数。
    
    输入1：`IAtomData`，jse 使用的任意的原子数据
    
    输入2（可选）：`int`，计算器使用的线程数，默认为 1（不开启并行）
    
    输入3（可选）：`double`，用于获取近邻列表进行分划 cell 的步长，
    默认为 `1.26`（更小的值会分划更细的 cell 从而提高近邻列表获取速度，
    但是会占用更多的内存）
    
    输出：`MPC`，创建的参量计算器对象
    
    例子：`example/mpc/rdf`
    [⤤](../release/script/groovy/example/mpc/rdf.groovy)，
    `example/mpc/boop`
    [⤤](../release/script/groovy/example/mpc/boop.groovy)
    
    > 注意：创建后记得在使用完成后显式调用 `shutdown()` 关闭 MPC 回收资源，
    > 或者使用 [*try-with-resources*](https://www.baeldung.com/java-try-with-resources)
    > 实现自动回收。
    > 
    
    -----------------------------
    
- **`withOf`**
    
    描述：根据输入参数构造一个 `jse.atom.MPC`，
    并将其作为一个闭包的输入，在闭包内进行任意计算后自动关闭此 MPC；
    对于只需要使用 MPC 计算单个参数的情况会更加简洁。
    
    输入1：`IAtomData`，jse 使用的任意的原子数据
    
    输入2（可选）：`int`，计算器使用的线程数，默认为 1（不开启并行）
    
    输入3（可选）：`double`，用于获取近邻列表进行分划 cell 的步长，
    默认为 `1.26`（更小的值会分划更细的 cell 从而提高近邻列表获取速度，
    但是会占用更多的内存）
    
    输入end：`IUnaryFullOperator<T, MPC>`，一个输入 MPC
    并输出任意结果（一般是计算结果）的闭包
    
    输出：`T`，通过闭包定义的输出结果，一般是使用 MPC 计算的结果
    
    例子：`example/mpc/rdf`
    [⤤](../release/script/groovy/example/mpc/rdf.groovy)，
    `example/mpc/rdfmulti`
    [⤤](../release/script/groovy/example/mpc/rdfmulti.groovy)

    
## RDF 和 SF 的计算

MPC 可以计算单个结构的 RDF（radial distribution function）
以及 SF（structural factor），并提供了相互转换的方法。

具体定义和应用可以
[参考文献 10.1088/0034-4885/69/1/R05](https://doi.org/10.1088/0034-4885/69/1/R05)。

对于有限温度需要进行时间平均的情况，可以参考 `example/mpc/rdfmulti`
[⤤](../release/script/groovy/example/mpc/rdfmulti.groovy)
的方法对所有帧进行计算并取平均。

- **`calRDF`**
    
    描述：计算 RDF (radial distribution function，即 g(r))，
    只计算一个固定结构的值，因此不包含温度信息。
    
    输入1（可选）：`int`，指定分划的份数（默认为 160）
    
    输入2（可选）：`double`，指定计算的最大半径（默认为 6 倍*单位长度*）
    
    输出：`IFunc1`，计算得到的 g(r)
    
    例子：`example/mpc/rdf`
    [⤤](../release/script/groovy/example/mpc/rdf.groovy)，
    `example/mpc/rdfmulti`
    [⤤](../release/script/groovy/example/mpc/rdfmulti.groovy)
    
    > 注意：会按照周期边界条件处理边界，
    > 理论上能够正确处理原子模拟盒小于输入最大半径的情况。
    > 
    > *单位长度*定义：$\text{uintLen} = (\text{volume} / \text{natoms})^{1/3}$，
    > 可以通过 `MPC.unitLen()` 获取。
    >
    
    -----------------------------
    
- **`calRDF_AB`**
    
    描述：计算自身与输入的原子坐标数据之间的 RDF，
    只计算一个固定结构的值，因此不包含温度信息；
    主要用于计算两种不同元素之间的 RDF。
    
    输入1：根据输入类型重载，具体为：
    
    - `Collection<? extends IXYZ> `，另一个种类的原子坐标数据 `IXYZ` 数组
    - `MPC`，另一个种类的原子坐标数据构建的 MPC
    
    输入2（可选）：`int`，指定分划的份数（默认为 160）
    
    输入3（可选）：`double`，指定计算的最大半径（默认为 6 倍*单位长度*）
    
    输出：`IFunc1`，计算得到的 g(r)
    
    例子：`example/mpc/rdf`
    [⤤](../release/script/groovy/example/mpc/rdf.groovy)
    
    -----------------------------
    
- **`calRDF_G`**
    
    描述：使用带有一定展宽的高斯分布代替直接计数来计算 RDF；
    用于获得更加连续光滑的函数。
    
    输入1（可选）：`int`，指定分划的份数（默认为 1000）
    
    输入2（可选）：`double`，指定计算的最大半径（默认为 6 倍*单位长度*）
    
    输入3（可选）：`int`，高斯分布的一个标准差宽度对应的分划份数（默认为 4）
    
    输出：`IFunc1`，计算得到的 g(r)
    
    例子：`example/mpc/rdf`
    [⤤](../release/script/groovy/example/mpc/rdf.groovy)
    
    -----------------------------
    
- **`calRDF_AB_G`**
    
    描述：使用带有一定展宽的高斯分布代替直接计数来计算 RDF；
    用于获得更加连续光滑的函数。
    
    输入1：根据输入类型重载，具体为：
    
    - `Collection<? extends IXYZ> `，另一个种类的原子坐标数据 `IXYZ` 数组
    - `MPC`，另一个种类的原子坐标数据构建的 MPC
    
    输入2（可选）：`int`，指定分划的份数（默认为 1000）
    
    输入3（可选）：`double`，指定计算的最大半径（默认为 6 倍*单位长度*）
    
    输入4（可选）：`int`，高斯分布的一个标准差宽度对应的分划份数（默认为 4）
    
    输出：`IFunc1`，计算得到的 g(r)
    
    例子：`example/mpc/rdf`
    [⤤](../release/script/groovy/example/mpc/rdf.groovy)
    
    -----------------------------
    
- **`calSF`**
    
    描述：计算 SF（structural factor，即 S(q)），
    只计算一个固定结构的值，因此不包含温度信息。
    
    输入1（可选）：`double`，额外指定最大计算的 q 的位置（默认为 6 倍*单位长度*）
    
    输入2（可选）：`int`，指定分划的份数（默认为 160）
    
    输入3（可选）：`double`，指定计算的最大半径（默认为 6 倍*单位长度*）
    
    输入4（可选）：`double`，指定最小的截断的 q（由于 pbc 的原因，过小的结果会发散，
    默认为 0.6 倍*单位长度*）
    
    输出：`IFunc1`，计算得到的 S(q)
    
    例子：`example/mpc/sf`
    [⤤](../release/script/groovy/example/mpc/sf.groovy)
    
    > 注意：会按照周期边界条件处理边界，
    > 理论上能够正确处理原子模拟盒小于输入最大半径的情况。
    > 
    > *单位长度*定义：$\text{uintLen} = (\text{volume} / \text{natoms})^{1/3}$，
    > 可以通过 `MPC.unitLen()` 获取；
    > 对于 q 值会取倒数，具体为：$\text{uintLenQ} = 2\pi / \text{uintLen}$。
    >
    > 直接计算耗时且收敛性较差，建议使用 `MPC.RDF2SF`
    > 通过傅里叶变换来将 RDF 转为 SF 来间接计算。
    >
    
    -----------------------------
    
- **`calSF_AB`**
    
    描述：计算自身与输入的原子坐标数据之间的 SF，
    只计算一个固定结构的值，因此不包含温度信息；
    主要用于计算两种不同元素之间的 SF。
    
    输入1：根据输入类型重载，具体为：
    
    - `Collection<? extends IXYZ> `，另一个种类的原子坐标数据 `IXYZ` 数组
    - `MPC`，另一个种类的原子坐标数据构建的 MPC
    
    输入2（可选）：`double`，额外指定最大计算的 q 的位置（默认为 6 倍*单位长度*）
    
    输入3（可选）：`int`，指定分划的份数（默认为 160）
    
    输入4（可选）：`double`，指定计算的最大半径（默认为 6 倍*单位长度*）
    
    输入5（可选）：`double`，指定最小的截断的 q（由于 pbc 的原因，过小的结果会发散，
    默认为 0.6 倍*单位长度*）
    
    输出：`IFunc1`，计算得到的 S(q)
    
    例子：`example/mpc/sf`
    [⤤](../release/script/groovy/example/mpc/sf.groovy)
    
    -----------------------------
    
- **`RDF2SF`**
    
    描述：转换 g(r) 到 S(q)，这是主要计算 S(q) 的方法。
    
    输入1：`IFunc1`，已经计算得到的 RDF
    
    输入2（可选）：`double`，原子数密度（默认会使用 MPC 存储的值）
    
    输入3（可选）：`int`，指定分划的份数（默认为 160）
    
    输入4（可选）：`double`，额外指定最大计算的 q 的位置（默认为 7.6 倍 g(r) 第一峰的距离）
    
    输入5（可选）：`double`，指定最小的截断的 q（默认为 0.5 倍 g(r) 第一峰的距离）
    
    输出：`IFunc1`，计算得到的 S(q)
    
    例子：`example/mpc/rdfsf`
    [⤤](../release/script/groovy/example/mpc/rdfsf.groovy)
    
    > 注意：在指定原子数密度后为静态方法，可以不创建 MPC 对象直接使用；
    > 对于两种不同类型的 RDF/SF，需要指定密度为 $\rho = \sqrt{\rho_A \rho_B}$
    > 才能得到正确的结果。
    >
    
    -----------------------------
    
- **`SF2RDF`**
    
    描述：转换 S(q) 到 g(r)。
    
    输入1：`IFunc1`，已经计算得到的 SF
    
    输入2（可选）：`double`，原子数密度（默认会使用 MPC 存储的值）
    
    输入3（可选）：`int`，指定分划的份数（默认为 160）
    
    输入4（可选）：`double`，额外指定最大计算的 r 的位置（默认为 7.6 倍 S(q) 第一峰的距离）
    
    输入5（可选）：`double`，指定最小的截断的 r（默认为 0.5 倍 S(q) 第一峰的距离）
    
    输出：`IFunc1`，计算得到的 S(q)
    
    例子：`example/mpc/rdfsf`
    [⤤](../release/script/groovy/example/mpc/rdfsf.groovy)
    
    > 注意：在指定原子数密度后为静态方法，可以不创建 MPC 对象直接使用；
    > 对于两种不同类型的 RDF/SF，需要指定密度为 $\rho = \sqrt{\rho_A \rho_B}$
    > 才能得到正确的结果。
    >
    

## 键角序参量的计算

MPC 可以计算每个原子的
BOOP（local Bond Orientational Order Parameters），
ABOOP（Averaged local Bond Orientational Order Parameters），
以及基于此的原子连接数目和判断原子是否是“类固体”。

具体定义和应用可以
[参考文献 10.1039/FD9960400093](https://doi.org/10.1039/FD9960400093)
和 [10.1063/1.2977970](https://doi.org/10.1063/1.2977970)。

- **`calBOOP`**
    
    描述：计算所有粒子的原始的 BOOP（local Bond Orientational Order Parameters, Ql），
    输出结果为按照输入原子顺序排列的向量；
    结果应当和 [lammps 中 `compute orientorder/atom command`](https://docs.lammps.org/compute_orientorder_atom.html)
    的结果一致。
    
    输入1：`int`，计算具体 Q 值的下标，即 Q4: l = 4, Q6: l = 6
    
    输入2（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入3（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`IVector`，计算得到的每个原子的 Ql，按照原子顺序排列
    
    例子：`example/mpc/boop`
    [⤤](../release/script/groovy/example/mpc/boop.groovy)
    
    > 注意：如果指定了最后一个参数（`Nnn`，Number of Nearest Neighbor list）
    > 则会限制最大的最近邻数目，一般会将此值设为 12 并设定一个足够大的最近邻半径
    > （如 2 倍*单位长度*）来保证一般都会对最近的 12 个原子进行计算。
    > 
    > 但是指定 `Nnn` 会极大的增加近邻列表获取的难度，
    > 并且会同时关闭遍历一半的优化，导致计算时间大大增加，
    > 因此默认情况下选取了 1.5 倍*单位长度* 并且不限制 `Nnn`
    > 从而选取大致 12 个近邻原子进行计算。
    >
    > *单位长度*定义：$\text{uintLen} = (\text{volume} / \text{natoms})^{1/3}$，
    > 可以通过 `MPC.unitLen()` 获取。
    >
    
    -----------------------------
    
- **`calBOOP3`**
    
    描述：计算所有粒子的三阶形式的 BOOP（Wl），
    输出结果为按照输入原子顺序排列的向量；
    结果应当和 [lammps 中 `compute orientorder/atom command`](https://docs.lammps.org/compute_orientorder_atom.html)
    的结果一致。
    
    输入1：`int`，计算具体 W 值的下标，即 W4: l = 4, W6: l = 6
    
    输入2（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入3（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`IVector`，计算得到的每个原子的 Wl，按照原子顺序排列
    
    例子：`example/mpc/boop`
    [⤤](../release/script/groovy/example/mpc/boop.groovy)
    
    -----------------------------
    
- **`calABOOP`**
    
    描述：计算所有粒子的原始的 ABOOP（Averaged local Bond Orientational Order Parameters, ql），
    输出结果为按照输入原子顺序排列的向量。
    
    输入1：`int`，计算具体 q 值的下标，即 q4: l = 4, q6: l = 6
    
    输入2（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入3（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`IVector`，计算得到的每个原子的 ql，按照原子顺序排列
    
    例子：`example/mpc/aboop`
    [⤤](../release/script/groovy/example/mpc/aboop.groovy)
    
    -----------------------------
    
- **`calABOOP3`**
    
    描述：计算所有粒子的三阶形式的 ABOOP（wl），
    输出结果为按照输入原子顺序排列的向量。
    
    输入1：`int`，计算具体 w 值的下标，即 w4: l = 4, w6: l = 6
    
    输入2（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入3（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`IVector`，计算得到的每个原子的 wl，按照原子顺序排列
    
    例子：`example/mpc/aboop`
    [⤤](../release/script/groovy/example/mpc/aboop.groovy)
    
    -----------------------------
    
- **`calConnectCountBOOP`**
    
    描述：通过 BOOP（Ql）来计算结构中每个原子的连接数目，
    输出结果为按照输入原子顺序排列的向量，数值为连接数目（整数）。
    
    输入1：`int`，计算具体 Q 值的下标，即 Q4: l = 4, Q6: l = 6
    
    输入2（可选）：`double`，用来判断两个原子是否是相连接的阈值
    
    输入3（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入4（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`IVector`，计算得到的每个原子的连接数目，按照原子顺序排列
    
    例子：`example/mpc/connectcount`
    [⤤](../release/script/groovy/example/mpc/connectcount.groovy)
    
    -----------------------------
    
- **`calConnectCountABOOP`**
    
    描述：通过 ABOOP（ql）来计算结构中每个原子的连接数目，
    输出结果为按照输入原子顺序排列的向量，数值为连接数目（整数）。
    
    输入1：`int`，计算具体 q 值的下标，即 q4: l = 4, q6: l = 6
    
    输入2（可选）：`double`，用来判断两个原子是否是相连接的阈值
    
    输入3（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入4（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`IVector`，计算得到的每个原子的连接数目，按照原子顺序排列
    
    例子：`example/mpc/connectcount`
    [⤤](../release/script/groovy/example/mpc/connectcount.groovy)
    
    -----------------------------
    
- **`checkSolidQ6`**
    
    描述：具体通过 Q6 来检测结构中类似固体的部分，
    输出结果为按照输入原子顺序排列的布尔向量，`true` 表示判断为类似固体；
    参数选取可以 [参考文献 10.1063/1.2977970](https://doi.org/10.1063/1.2977970)。
    
    输入1（可选）：`double`，用来判断两个原子是否是相连接的阈值（默认为 0.5）
    
    输入2（可选）：`int`，用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值（默认为 7）
    
    输入3（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入4（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`ILogicalVector`，计算得到的每个原子是否是“类固体”原子的逻辑值，按照原子顺序排列
    
    例子：`example/mpc/connectcount`
    [⤤](../release/script/groovy/example/mpc/connectcount.groovy)
    
    -----------------------------
    
- **`checkSolidQ4`**
    
    描述：具体通过 Q4 来检测结构中类似固体的部分，
    输出结果为按照输入原子顺序排列的布尔向量，`true` 表示判断为类似固体；
    参数选取可以 [参考文献 10.1063/1.1896348](https://doi.org/10.1063/1.1896348)。
    
    输入1（可选）：`double`，用来判断两个原子是否是相连接的阈值（默认为 0.35）
    
    输入2（可选）：`int`，用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值（默认为 6）
    
    输入3（可选）：`double`，用来搜索的最近邻半径（默认为 1.5 倍*单位长度*）
    
    输入4（可选）：`int`，限制最大的最近邻数目（默认不做限制）
    
    输出：`ILogicalVector`，计算得到的每个原子是否是“类固体”原子的逻辑值，按照原子顺序排列
    
    例子：`example/mpc/connectcount`
    [⤤](../release/script/groovy/example/mpc/connectcount.groovy)


## Voronoi 分析

MPC 可以简单的计算出原子坐标的 voronoi 多面体并进行分析，
从而给出每个原子的 voronoi 多面体的体积，面数，指数等，
具体定义和应用可以
[参考 ovito 的文档](https://docs.ovito.org/reference/pipelines/modifiers/voronoi_analysis.html)。

这里采用 [这个文章的方法](https://ieeexplore.ieee.org/document/4276112)
来实现简单的 3D voronoi 多面体的计算，并基于
[Hellblazer/Voronoi-3D](https://github.com/Hellblazer/Voronoi-3D)
的代码提供纯 java 的实现，从而可以在*任何地方*快速得到结果。

这里通过直接在最外围增加一层镜像原子的方法，实现周期边界条件下的 voronoi
多面体计算。

- **`calVoronoi`**
    
    描述：计算 voronoi 多面体并获取各种参数，
    由于内部实现是串行的，因此此方法不受线程数影响。
    
    输入1（可选）：`double`，外围周期边界增加的镜像粒子的半径（默认为 3 倍*单位长度*）
    
    输入2（可选）：`boolean`，是否关闭错误警告（默认为 `false`）
    
    输入3（可选）：`int`，voronoi indices 的存储长度（默认为 9）
    
    输入4（可选）：`double`，过小面积的阈值（相对值，默认为 0.0，不处理）
    
    输入4（可选）：`double`，过小长度的阈值（相对值，默认为 0.0，不处理）
    
    输出：`IVoronoiCalculator`，voronoi 多面体参数的计算器
    
    例子：`example/mpc/voronoi`
    [⤤](../release/script/groovy/example/mpc/voronoi.groovy)
    
    > 注意：`IVoronoiCalculator` 也提供了一系列的 set 方法来设置这些参数，
    > 因此更推荐直接使用 `calVoronoi()` 获取到 `IVoronoiCalculator`
    > 后，通过此对象来进行需要的设置，相比通过传参设置更加灵活。
    >



其中获取得到的 `IVoronoiCalculator` 继承了
`List<VoronoiBuilder.IVertex>`，而
[`VoronoiBuilder.IVertex`](../src/main/java/jsex/voronoi/VoronoiBuilder.java)
提供了系列接口来获取到各种常用的 voronoi 多面体参数：

- **`IVertex.coordination`**
    
    输出：`int`，此节点对应 voronoi 多面体的面的数目（配位数）
    
    例子：`example/mpc/voronoi`
    [⤤](../release/script/groovy/example/mpc/voronoi.groovy)
    
    -----------------------------
    
- **`IVertex.atomicVolume`**
    
    输出：`double`，此节点对应 voronoi 多面体的体积（原子体积）
    
    例子：`example/mpc/voronoi`
    [⤤](../release/script/groovy/example/mpc/voronoi.groovy)
    
    -----------------------------
    
- **`IVertex.cavityRadius`**
    
    输出：`double`，此节点距 voronoi 多面体顶点的最大距离（空腔距离）
    
    例子：`example/mpc/voronoi`
    [⤤](../release/script/groovy/example/mpc/voronoi.groovy)
    
    -----------------------------
    
- **`IVertex.index`**
    
    输出：`int[]`，记录此节点的 voronoi 多面体每个面的多边形边数统计（voronoi indices），
    `IVertex.index()[4] == 12` 代表拥有 `12` 个 `4+1` 边形
    
    例子：`example/mpc/voronoi`
    [⤤](../release/script/groovy/example/mpc/voronoi.groovy)
    
    > 注意：这里为了保证直接输出的 `IVertex.index()` 和 ovito 的结果一致，
    > 同样保持从 0 开始计数，因此如果希望获得 5 边形的数目应该使用
    > `IVertex.index()[4]`
    >
    
    -----------------------------
    
- **`IVertex.x`, `IVertex.y`, `IVertex.z`**
    
    输出：`double`，此节点的 x, y, z 坐标（即为原子坐标）
    
    > 这些方法主要用于计算上面没有给出的 voronoi 参数
    >
    
    -----------------------------
    
- **`IVertex.neighborVertex`**
    
    输出：`Collection<IVertex>`，此节点的 voronoi 近邻节点列表，
    voronoi 多面体的每个面对应一个中心节点和近邻节点的垂直平分面
    
    > 这些方法主要用于计算上面没有给出的 voronoi 参数
    >
    
    -----------------------------
    
- **`IVertex.neighborTetrahedron`**
    
    输出：`Collection<ITetrahedron>`，此节点的和近邻节点组成的四面体列表（Delaunay 四面体），
    voronoi 多面体的每个顶点对应一个近邻四面体的外接球球心
    
    > 这些方法主要用于计算上面没有给出的 voronoi 参数
    >


