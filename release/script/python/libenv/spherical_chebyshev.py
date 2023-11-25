import json
import warnings
import math
import numpy as np
from numba import njit
from libenv.neighbors import NeighborList
from libenv.cutoffs import polymial_cutoff
from libenv.chebyshev import chebyshev_polynomial
from libenv.spherical_functions import ylm_cartesian_all


class SphericalChebyshev:
    def __init__(self, atom_types, nmax, lmax, rcut):
        self.atom_types = atom_types
        self.nmax = nmax
        self.lmax = lmax
        self.rcut = rcut
        self.typeid = {atom_types[i]: i for i in range(len(atom_types))}
        self.typespin = np.empty(len(atom_types), dtype=int)
        self.nvalues = (self.nmax + 1)*(self.lmax + 1)
        if len(self.atom_types) > 1:
            s = -len(atom_types)//2
            for i in range(len(atom_types)):
                if s == 0 and len(atom_types) % 2 == 0:
                    s += 1
                self.typespin[i] = s
                s += 1
            self.nvalues *= 2
        self.nl = None

    def save(self, filename):
        parameters = {
            "atom_types": [i for i in self.atom_types],
            "nmax": self.nmax,
            "lmax": self.lmax,
            "rcut": self.rcut
        }
        with open(filename, "w") as o:
            json.dump(parameters, o)

        return

    @staticmethod
    def load(filename):
        parameters = json.loads(open(filename).read())
        return SphericalChebyshev(**parameters)

    def update(self, atoms):
        if self.nl is None or len(self.nl.cutoffs) != len(atoms):
            cutoffs = 0.5*self.rcut*np.ones(len(atoms))
            self.nl = NeighborList(cutoffs)
        self.nl.update(atoms)

    def evaluate(self, atoms):
        fingerprints = np.zeros((len(atoms), self.nvalues), dtype=np.float64)
        self.update(atoms)
        for i in range(len(atoms)):
            indices, offsets = self.nl.get_neighbors(i)
            typ_i = self.typeid[atoms[i].symbol]
            coo_i = atoms[i].position
            typ_j = []
            coo_j = []
            for j, offset in zip(indices, offsets):
                coo = atoms[j].position + \
                    np.dot(offset, atoms.get_cell(complete=True))
                d = np.linalg.norm(coo - coo_i)
                if d <= self.rcut:
                    typ_j.append(self.typeid[atoms[j].symbol])
                    coo_j.append(coo)
            if len(typ_j) == 0:
                warnings.warn("Atom at index %i is isolate."%i)
                continue
            fingerprints[i, :] = _evaluate(
                self.nmax,
                self.lmax,
                self.rcut,
                self.typespin,
                typ_i,
                coo_i,
                np.asarray(typ_j),
                np.asarray(coo_j)
            )

        return fingerprints

@njit("f8[:](i4, i4, f8, i4[:], i4, f8[:], i4[:], f8[:,:])")
def _evaluate(nmax, lmax, rcut, spin, ti, xi, tj, xj):
    multi = len(spin) > 1
    nv = (nmax + 1)*(lmax + 1)
    if multi:
        nv *= 2
    values = np.zeros(nv, dtype=np.float64)
    nj = len(tj)
    tf = np.empty((nmax + 1, nj), dtype=np.float64)
    ylm_table = np.empty((lmax + 1, 2*lmax + 1, nj), dtype=np.complex128)
    cnlm = np.zeros((nmax + 1, lmax + 1, 2*lmax + 1), dtype=np.complex128)
    nc = np.array([4*np.pi/(2*l + 1) for l in range(lmax + 1)])
    if multi:
        cnlm_s = np.zeros((nmax + 1, lmax + 1, 2*lmax + 1), dtype=np.complex128)
    for j in range(nj):
        s_j = spin[tj[j]]
        r_ij = xj[j] - xi
        d_ij = math.sqrt(r_ij[0]*r_ij[0] + r_ij[1]*r_ij[1] + r_ij[2]*r_ij[2])
        fc, _ = polymial_cutoff(d_ij, rcut)
        tf[:, j] = chebyshev_polynomial(rcut - d_ij, 0.0, rcut, nmax)
        ylm_table[:, :, j] = ylm_cartesian_all(r_ij, lmax)
        for l in range(lmax + 1):
            for m in range(2*l + 1):
                cnlm[:, l, m] = cnlm[:, l, m] + fc*tf[:, j]*ylm_table[l, m, j]
        if multi:
            for l in range(lmax + 1):
                for m in range(2*l + 1):
                    cnlm_s[:, l, m] = cnlm_s[:, l, m] + s_j*fc*tf[:, j]*ylm_table[l, m, j]
    for n in range(nmax + 1):
        i1 = n*(lmax + 1)
        if multi:
            i2 = i1 + nv//2
        for l in range(lmax + 1):
            values[i1 + l] += nc[l]*np.sum(np.real(cnlm[n, l, 0:(2*l + 1)]*
                                           np.conj(cnlm[n, l, 0:(2*l + 1)])))
        if multi:
            for l in range(lmax + 1):
                values[i2 + l] += nc[l]*np.sum(np.real(cnlm_s[n, l, 0:(2*l + 1)]*
                                               np.conj(cnlm_s[n, l, 0:(2*l + 1)])))

    return values


if __name__ == "__main__":
    from ase.build import molecule

    mol = molecule("H2O")
    mol.center(10)
    bas = SphericalChebyshev(["H", "O"], 2, 2, 4.0)
    fp = bas.evaluate(mol)
    fp_ref = [[2.46680041E+00, 9.35016299E-01, 7.24977557E-01,
              6.56081413E-01, 2.48681171E-01, 1.92818315E-01,
              5.40452299E-01, 2.04853099E-01, 1.58835626E-01,
              2.46680041E+00, 9.35016299E-01, 7.24977557E-01,
              6.56081413E-01, 2.48681171E-01, 1.92818315E-01,
              5.40452299E-01, 2.04853099E-01, 1.58835626E-01],
              [1.73743556E+00, 1.56003329E+00, 1.26163999E+00,
              2.82114189E-01, 2.60453054E-01, 2.24018698E-01,
              7.06706138E-01, 6.32978680E-01, 5.08967980E-01,
              6.37496443E-02, 2.41151914E-01, 5.39545216E-01,
              7.77541439E-02, 9.94152784E-02, 1.35849634E-01,
              1.11309988E-02, 8.48584570E-02, 2.08869157E-01],
              [1.73743556E+00, 1.56003329E+00, 1.26163999E+00,
              2.82114189E-01, 2.60453054E-01, 2.24018698E-01,
              7.06706138E-01, 6.32978680E-01, 5.08967980E-01,
              6.37496443E-02, 2.41151914E-01, 5.39545216E-01,
              7.77541439E-02, 9.94152784E-02, 1.35849634E-01,
              1.11309988E-02, 8.48584570E-02, 2.08869157E-01]]
    fp_ref = np.array(fp_ref)
    assert np.allclose(fp, fp_ref)
