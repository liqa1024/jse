import numpy as np

from ase import Atoms
from ase.calculators.singlepoint import SinglePointCalculator
from ase.calculators.calculator import Calculator, all_changes, PropertyNotImplementedError

from jse.ase import AseAtoms
from jse.math.vector import Vectors
from jse.math.matrix import Matrices


def _jse2ase(jseobj):
    if jseobj is None:
        return None
    if hasattr(jseobj, 'numpy'):
        return jseobj.numpy()
    if np.iterable(jseobj):
        return np.array(jseobj)
    return jseobj

def _ase2jse(aseobj):
    if aseobj is None:
        return None
    if isinstance(aseobj, np.ndarray):
        if np.issubdtype(aseobj.dtype, np.integer):
            # convert to int32 anyway
            aseobj = aseobj.astype(np.int32, casting='unsafe', copy=False)
            if aseobj.ndim == 0:
                return int(aseobj)
        elif np.issubdtype(aseobj.dtype, np.floating):
            # convert to float64 anyway
            aseobj = aseobj.astype(np.float64, casting='unsafe', copy=False)
            if aseobj.ndim == 0:
                return float(aseobj)
        elif np.isdtype(aseobj.dtype, np.bool):
            if aseobj.ndim == 0:
                return bool(aseobj)
        else:
            return aseobj.tolist()
        if aseobj.ndim == 1:
            return Vectors.fromNumpy(aseobj)
        if aseobj.ndim == 2:
            return Matrices.fromNumpy(aseobj)
        return aseobj.copy()
    return aseobj

def convertAseAtomsToPyObject(jatoms, limited=False):
    _cell = jatoms.box()
    _numbers = jatoms.atomicNumbers()
    if _numbers is None:
        _numbers = np.zeros(jatoms.natoms(), int)
    else:
        _numbers = _numbers.numpy()
    _positions = jatoms.positions()
    if _positions is None:
        _positions = np.zeros((jatoms.natoms(), 3))
    else:
        _positions = _positions.numpy()
    _info = None
    if not limited:
        _info = {}
        for k, v in jatoms.infos().items():
            _info[k] = _jse2ase(v)
    
    pyatoms = Atoms(
        cell=[[_cell.ax(), _cell.ay(), _cell.az()], [_cell.bx(), _cell.by(), _cell.bz()], [_cell.cx(), _cell.cy(), _cell.cz()]],
        pbc=True,
        numbers=_numbers,
        positions=_positions,
        info=_info
    )
    if limited:
        return pyatoms
    
    for k, v in jatoms.arrays().items():
        if k!='numbers' and k!='positions':
            pyatoms.arrays[k] = _jse2ase(v)
    _results = {}
    for k, v in jatoms.calcResults().items():
        _results[k] = _jse2ase(v)
    if _results:
        pyatoms.calc = SinglePointCalculator(pyatoms, **_results)
    
    return pyatoms

def convertPyObjectToAseAtoms(pyatoms, limited=False):
    _cell = pyatoms.cell
    _infos = {}
    _arrays = {}
    _results = {}
    if limited:
        _arrays['numbers'] = _ase2jse(pyatoms.numbers)
        _arrays['positions'] = _ase2jse(pyatoms.positions)
    else:
        for k, v in pyatoms.info.items():
            _infos[k] = _ase2jse(v)
        for k, v in pyatoms.arrays.items():
            _arrays[k] = _ase2jse(v)
        if pyatoms.calc:
            for k, v in pyatoms.calc.results.items():
                _results[k] = _ase2jse(v)
    
    return AseAtoms.newAseAtoms_(
        len(pyatoms),
        _cell[0, 0], _cell[0, 1], _cell[0, 2],
        _cell[1, 0], _cell[1, 1], _cell[1, 2],
        _cell[2, 0], _cell[2, 1], _cell[2, 2],
        _infos, _arrays, _results
    )


class PotentialCalculator(Calculator):
    implemented_properties = ['energy', 'energies', 'forces', 'stress', 'stresses']
    name = 'jse'
    
    def __init__(self, pot, **kwargs):
        super().__init__(**kwargs)
        self.pot = pot
    
    def release(self):
        self.pot.shutdown()
    def shutdown(self):
        self.pot.shutdown()
    
    def calculate(self, atoms=None, properties=['energy'], system_changes=all_changes):
        super().calculate(atoms, properties, system_changes)
        for p in properties:
            if p not in self.implemented_properties:
                raise PropertyNotImplementedError(p)
            if not self.pot.perAtomEnergySupport() and p=='energies':
                raise PropertyNotImplementedError(p)
            if not self.pot.perAtomStressSupport() and p=='stresses':
                raise PropertyNotImplementedError(p)
        self.results = dict(self.pot.calculate_(self.results, self.atoms, properties, len(system_changes)>0))

