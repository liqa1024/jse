"""
Fast neighbor list wrapper over the matscipy's C
implemention with fallback to the ASE's pure python one.
"""

import numpy as np
from ase.neighborlist import first_neighbors
try:
    from matscipy.neighbours import neighbour_list as neighbor_list
except ImportError:
    from ase.neighborlist import neighbor_list

class NeighborList:
    def __init__(self, cutoffs, skin=0.3):
        self.cutoffs = np.asarray(cutoffs) + skin
        self.skin = skin
        self.nupdates = 0
        self.nneighbors = 0
        self.npbcneighbors = 0

    def update(self, atoms):
        if self.nupdates == 0:
            self.build(atoms)
            return True

        pbc = atoms.pbc
        cell = atoms.get_cell(complete=True)
        positions = atoms.positions
        if ((self.pbc != pbc).any() or (self.cell != cell).any() or
            ((self.positions - positions)**2).sum(1).max() > self.skin**2):
            self.build(atoms)
            return True

        return False

    def build(self, atoms):
        self.pbc = atoms.pbc.copy()
        self.cell = atoms.get_cell(complete=True).copy()
        self.positions = atoms.positions.copy()
        self.pair_first, self.pair_second, self.offset = \
            neighbor_list('ijS', atoms, self.cutoffs)

        self.first_neigh = first_neighbors(len(atoms), self.pair_first)

        self.nupdates += 1

    def get_neighbors(self, a):
        ia = self.first_neigh[a]
        ib = self.first_neigh[a + 1]
        return self.pair_second[ia:ib], self.offset[ia:ib]


if __name__ == "__main__":
    from ase.build import bulk

    atoms = bulk("Au", cubic=True) * (3, 3, 3)

    nl = NeighborList([1.5]*len(atoms))
    nl.update(atoms)
    print("Natoms = {}".format(len(atoms)))
    print(sorted(nl.get_neighbors(0)[0]))
