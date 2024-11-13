package example.atoms

import jse.atom.Atom
import jse.atom.AtomData
import jse.atom.Box
import jse.atom.Structures


/**
 * 自定义晶体创建，这里创建一个简单立方晶体
 */

// 创建原胞，
// 包含一个在原点的原子，id 和 type 都为 1，
// 模拟盒三维都为 2.0（晶格常数为 2.0）
def cell = new AtomData(
    [new Atom(0.0, 0.0, 0.0, 1, 1)],
    new Box(2.0, 2.0, 2.0)
)

// 根据原胞扩展，xyz 方向都重复 10 次
def data = Structures.from(cell, 10)

println('atom number: ' + data.natoms())
println('atom at 10: ' + data.atom(10))
println('box: ' + data.box())

//OUTPUT:
// atom number: 1000
// atom at 10: {id: 11, type: 1, xyz: (0.000, 2.000, 0.000)}
// box: (20.00, 20.00, 20.00)
