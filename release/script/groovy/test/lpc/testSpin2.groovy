package test.lpc

import jtool.atom.Atom
import jtool.atom.AtomData
import jtool.atom.IAtom
import jtool.lmp.Dump

import static jtool.code.CS.*

/**
 * 测试 lammps 的 spin 包；
 * 将 dump 转为赝原子的形式方便 ovito 绘制
 */

dump = Dump.read('lmp/.Co/Co500-cooldown.lammpstrj');

for (i in 0..<dump.size()) {
    def data = dump[i];
    def spins = data.asTable().refSlicer().get(ALL, ['c_outsp[1]', 'c_outsp[2]', 'c_outsp[3]']).asMatrix();
    
    def atomsFull = new ArrayList<IAtom>(data.atomNum()*2);
    def atoms = data.asList()
    for (j in 0..<atoms.size()) {
        def atom = atoms[j];
        atomsFull.add(new Atom(atom));
        atomsFull.add(new Atom(atom.x()+spins[j][0], atom.y()+spins[j][1], atom.z()+spins[j][2], atom.id()+atoms.size(), 2));
    }
    
    dump[i] = new AtomData(atomsFull, 2, data.box());
}

dump.write('lmp/.Co/Co500-cooldown-fake.lammpstrj');

