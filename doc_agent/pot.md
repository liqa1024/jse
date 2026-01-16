# jse - Atomic Potential & Calculator Documentation

This document guides Agents on using the `jse.atom.IPotential` interface. This is the backbone for calculating Energy, Forces, and Stress (Virial) in simulations.

## 1. Core Concepts & Architecture

### 1.1 Stateless Design
Unlike ASE, where a calculator is "attached" to an Atoms object, `jse` potentials are **stateless regarding the atomic configuration**.
*   **Concept:** The potential is a function. You pass the atom data *into* the calculation method.
*   **Pattern:** `double eng = potential.calEnergy(atoms)`

### 1.2 Lifecycle & Memory Management
Potentials that use native resources (NEP, NNAP, LmpPotential) or temporary files (SystemLmpPotential) implement `jse.parallel.IAutoShutdown`.

*   **Auto-Cleanup:** They use `ReferenceChecker` to free resources when the object is Garbage Collected, for simple scripts you can rely on this to ensure brevity.
*   **Heavy workflows:** Use **try-with-resources** to ensure deterministic cleanup.

```groovy
// Preferred Pattern in heavy workflows
try (def pot = new NNAP('model.json')) {
    double eng = pot.calEnergy(atoms)
} 
// pot.shutdown() is called automatically here
```

### 1.3 `IPotential` vs `IPairPotential`
*   **`IPotential`**: The generic interface. Supports Energy, Forces, Stress.
*   **`IPairPotential`**: Extends `IPotential`. Represents potentials dependent on pair-wise interactions and cutoffs. Supports **optimized local energy differences** (critical for Monte Carlo).

---

## 2. Available Potentials

### 2.1 Classical Potentials (High Performance)
Pure Java implementations. **Significantly faster (>50x)** and more robust than calling ASE equivalents via Python.

#### Lennard-Jones (`jse.atom.pot.LJ`)
*   **Constructor:** `new LJ(epsilon, sigma, rcut)`
*   **Input:** Scalars (same for all types) or `double[][]` matrices (for specific type pairs).

#### Embedded Atom Method (`jse.atom.pot.EAM`)
*   **Constructor:** `new EAM('path/to/potential.file')`
*   **Supported Formats:** `eam`, `alloy`, `fs`, `adp` (LAMMPS DYNAMO formats).

#### Soft Potential (`jse.atom.pot.Soft`)
*   **Constructor:** `new Soft(prefactor, rcut)`
*   **Formula:** $E = A[1 + \cos(\pi r / r_c)]$
*   **Usage:** Often used to push overlapping atoms apart during initialization.

### 2.2 Machine Learning Potentials

#### NEP (`jsex.nep.NEP`)
*   **Constructor:** `new NEP('nep.txt')`
*   **Backend:** Pure Java implementation (ported from GPUMD/NEP_CPU).

#### NNAP (`jsex.nnap.NNAP`)
*   **Constructor:** `new NNAP('model.json')` (or `.yaml`)
*   **Backend:** Pure C++ CPU implementation (highly optimized with SIMD).
*   **Threading:** Pass thread count in constructor: `new NNAP('model.json', 4)`.

### 2.3 LAMMPS Wrappers
Use LAMMPS to calculate properties. `jse` handles unit conversions automatically so results match `jse` standard units (eV, Angstrom, stress in eV/Å^3 for `metal`).

#### Native JNI (`jse.lmp.LmpPotential`)
*   **Constructor:** `new LmpPotential(pairStyle, pairCoeffs)`
*   **Pros:** Fast (in-memory JNI).
*   **Cons:** Fixed thread, environmental requirements.
*   **Example:**
    ```groovy
    def pot = new LmpPotential('lj/cut 2.5', '1 1 1.0 1.0')
    ```

#### System Shell (`jse.lmp.SystemLmpPotential`)
*   **Constructor:** `new SystemLmpPotential(pairStyle, pairCoeffs)`
*   **Pros:** Robust process isolation (system execution, compatible for some LAMMPS styles).
*   **Cons:** Slower (file I/O overhead, unstable in some filesystem).
*   **Usage:** Use this if JNI fails or for extremely complex LAMMPS styles.

### 2.4 ASE Bridge (`jse.ase.AseCalculator`)
Wraps a Python ASE calculator object.

```groovy
import jse.ase.AseCalculator
import jse.code.SP

// 1. Create Python Calculator object
SP.Python.exec('from ase.calculators.emt import EMT')
def EMT = SP.Python.get('EMT')
def pyCalc = EMT()

// 2. Wrap in jse interface
def pot = new AseCalculator(pyCalc)
```

---

## 3. Calculation Methods

### 3.1 Basic Properties

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `calEnergy(atoms)` | `double` | Total potential energy. |
| `calEnergies(atoms)` | `Vector` | Per-atom potential energies. |
| `calForces(atoms)` | `RowMatrix` | Forces ($N \times 3$). |
| `calStress(atoms)` | `List<Double>` | Global Virial Stress (`[xx, yy, zz, xy, xz, yz]`). |
| `calStresses(atoms)` | `List<Vector>` | Per-atom Virial Stress (List of 6 Vectors). |
| `asAseCalculator()` | `PyObject` (`PotentialCalculator` in python) | ASE calculator wrapper for jse potential |

### 3.2 Advanced: Monte Carlo Optimizations

If the potential implements `IPairPotential` (includes EAM, LJ, NEP, NNAP), it supports **localized calculations**. This is critical for efficient Monte Carlo (MC) simulations.

| Method | Description | Optimization |
| :--- | :--- | :--- |
| `calEnergyDiffMove(atoms, idx, dx, dy, dz)` | Calculates $\Delta E$ for moving atom `idx`. | Only recalculates neighbors within $R_{cut}$. |
| `calEnergyDiffSwap(atoms, i, j)` | Calculates $\Delta E$ for swapping types of `i` and `j`. | Only recalculates affected neighborhoods. |
| `calEnergyDiffFlip(atoms, i, newType)` | Calculates $\Delta E$ for changing type of `i`. | Depends on $R_{cut}$. |

**Usage Notes:** 
1. Default behavior restores the atom data to its original state after calculation. Pass `false` as the last argument to apply the move permanently if accepted.
2. Use the interface passed in `APC` to avoid rebuilding the neighbor list, thereby achieving the ultimate optimization.

---

## 4. Accelerating ASE with jse Potentials

**Core Philosophy for Agents:**
If a user provided script uses ASE (Python) for complex workflows (e.g., NEB, Phonopy, Genetic Algorithms) but uses a slow or generic calculator (like `ase.calculators.lj` or pure-Python EAM), **always recommend replacing the calculator with a `jse` native equivalent converted via `.asAseCalculator()`**. This provides C++/Java speed (~50x faster) while maintaining full compatibility with the Python logic.

### 4.1 The Conversion Mechanism

Every `jse.atom.IPotential` implementation (LJ, EAM, Soft, NEP, NNAP, LmpPotential) includes a default method:

`PyObject asAseCalculator()`

*   **Function:** It wraps the Java potential inside a custom Python class (`jsepy.atom.PotentialCalculator`) that inherits from `ase.calculators.calculator.Calculator`.
*   **Behavior:** When ASE requests energy, forces, or stress, the Python wrapper calls back into the optimized Java engine.
*   **Auto-mapping:** It automatically maps `jse`'s Voigt stress notation and units to ASE's expected format.
*   **Auto-Cleanup:** Since there are now two references on the Java and Python sides, it is completely handed over to the automatic cleanup mechanism **instead of** manual shutdown.

### 4.2 Usage Pattern

#### Basic Workflow
1.  **Instantiate** the high-performance potential in Java/Groovy.
2.  **Convert** it to a Python object using `.asAseCalculator()`.
3.  **Attach** it to ASE Atoms objects.

#### Example Scenario: Accelerating an ASE Relaxation

**Scenario:** A user wants to relax a structure using EAM. Doing this in pure Python is slow.

**Optimized Script:**

```python
from ase.build import bulk
from ase.optimize import BFGS
from jse.atom.pot import EAM

# 1. Initialize the High-Performance Java Potential
# This runs in native code (fast neighbor lists, optimized math)
j_pot = EAM('Al.eam.alloy')

# 2. Convert to ASE Calculator (return PyObject, auto convert by JEP)
pot_cal = j_pot.asAseCalculator()

# 3. Run Python/ASE workflow
# Standard ASE setup
atoms = bulk('Al', 'fcc', a=4.1)

# Attach the jse calculator
atoms.calc = pot_cal

# jse handles the force calculation during relaxation
dyn = BFGS(atoms)
dyn.run(fmax=0.01)

print(f'Relaxed Energy: {atoms.get_potential_energy()}')
print(f'Stress: {atoms.get_stress()}')
```

> **Notation**: Although it is always recommended to use `try-with-resources`, it does not apply here because at this point there are two references in Java and Python, and the simplest processing at this time is to leave them all to the automatic recycling mechanism.

### 4.3 Supported Features Checklist

When converting `jse` potentials to ASE, the following properties are automatically supported if the underlying potential implements them:

| ASE Property | jse Capability | Note |
| :--- | :--- | :--- |
| `energy` | `calEnergy` | Total system potential |
| `forces` | `calForces` | $N \times 3$ force array |
| `stress` | `calStress` | 6-component virial stress (Voigt form) |
| `energies` | `calEnergies` | Per-atom potential energy |
| `stresses` | `calStresses` | Per-atom stress tensors |

### 4.4 Agent Guidelines (When to Recommend)

1.  **NEP/NNAP in ASE:** If a user wants to use a machine learning potential trained with GPUMD (NEP) or jse (NNAP) within an ASE pipeline (e.g., for `ase.neb` or `ase.phonons`), generate code using `from jsex.nep import NEP; pot_cal = NEP(...).asAseCalculator()`. This is the standard way to bridge these modern MLIPs into ASE.
2.  **Replacing LAMMPS-Python:** If a user is using `lammps` python interface just to calculate energy/forces, suggest `from jse.lmp import LmpPotential; lmp_cal = LmpPotential(...).asAseCalculator()`. It effectively embeds LAMMPS into ASE without the overhead of the official `ase.calculators.lammpsrun`, and has the correct stress results.
3.  **Legacy Scripts:** If generating a script that *must* use a Python library (like `CatLearn` or `Phonopy`) but the user complains about performance, swap the calculator for a `jse` proxy.

### 4.5 Reverse Operation (Python to Java)

Do not confuse `.asAseCalculator()` (Java $\to$ Python) with `new AseCalculator(pyObj)` (Python $\to$ Java).

*   **Fast Path (Recommended):** `jse Potential` $\to$ `asAseCalculator()` $\to$ `Python Script`.
*   **Slow Path (Avoid):** `Python Calculator` $\to$ `new AseCalculator()` $\to$ `Java Script`. (Only use this if the potential *only* exists in Python, e.g., a specific graph neural network not implemented in `jse`).
