# jse - Atom & Structure Library Documentation

The `jse.atom` package is the core computational module for handling atomic structures, simulation boxes, and trajectories.

**Key Architectural Philosophy:**
Unlike many simulation codes that force all data into a single generic "Universal Atomic Structure," `jse` classes (`Lmpdat`, `POSCAR`, `DataXYZ`) **preserve the underlying memory layout** of their respective file formats as much as possible.
*   **Significance:** This avoids expensive full-object conversion during reading/writing.
*   **Result:** Different implementations share common interfaces (`IAtomData`, `ISettableAtomData`) but have unique internal logic optimized for their format.

**Import Strategy:**
```groovy
import jse.atom.*
import jse.atom.data.*
import jse.lmp.*
import jse.vasp.*
import jse.ase.*
```

---

## 0. Units & Conventions (Unit-Agnostic Design)

jse is **unit-agnostic**:

* jse **never assumes or enforces** units for coordinates, time, mass, or energy. All readers (LAMMPS, POSCAR, XYZ, etc.) **keep the raw numbers**; writers output the same numbers back.
* The only structured exception is the **ASE bridge** (`jse.ase.AseAtoms`): ASE applies its own internal unit conversions (time, etc.). jse applies the **inverse** conversion so that numbers in jse match what users see in ASE.
* All `IPotential`-based calculations (energy, force, stress) use the input numbers as-is. If your structure is in LAMMPS “metal units”, the resulting stress will naturally be in `eV/Å^3`, etc.
* When coupling to external codes (e.g. LAMMPS), any necessary unit conversion is either handled by jse’s integration layer or must be done explicitly by the user; `CS.UNITS' may help you with this.

## 1. The Zero-Origin Coordinate Rule

To simplify most usage, **all public coordinate Accessors and Setters in `jse` operate in a shifted coordinate system starting at (0, 0, 0).**

*   **Behavior:**
    *   If a file (e.g., LAMMPS data) defines a box from `xlo=-5.0` to `xhi=5.0`, accessing `atom.x()` will return values between `0.0` and `10.0` (if wrapped).
    *   The `box()` object handles dimensions relative to this origin.
*   **Internal Storage:** The underlying data often retains the original values (e.g., `Lmpdat` stores the original `xlo` in `LmpBox`). When writing back to a file, the `write()` method automatically restores the original offsets.
*   **Advanced usage:** Users can access/modify internal raw values via format-specific internal methods if absolutely necessary, but standard scripts should assume `min(x) >= 0` (if wrapped).

---

## 2. Core Interfaces

These interfaces provide a unified view over the different underlying implementations.

### 2.1 Atomic Coordinates (`IXYZ`) & Atoms (`IAtom`)
*   **`IXYZ`**: Represents a 3D vector.
    *   `x()`, `y()`, `z()`: Returns shifted coordinates.
    *   Math: `plus`, `minus`, `dot`, `cross`, `distance`.
    *   `toVec()`: Converts to a `jse.math.vector.Vector`.
*   **`IAtom` (extends `IXYZ`)**: Represents a particle.
    *   `type()`: Integer type ID (1-based, consistent with LAMMPS).
    *   `id()`: Atom ID (1-based, returns `-1` if usually not present).
    *   `vx()`, `vy()`, `vz()`: Velocity (returns `0.0` if not present).
    *   `index()`: The 0-based index in the parent list.

### 2.2 AtomData (`IAtomData`)
The container for a frame.
*   `atoms()`: Returns `List<IAtom>`.
*   `atom(i)`: Returns the atom at index `i`.
*   `box()`: Returns `IBox` (Dimensions `a, b, c` and tilt factors).
*   `natoms()`: Total number of atoms.
*   `ntypes()`: Total number of types.
*   `masses()`: Vector of masses.

### 2.3 Operations (`ISettableAtomDataOperation`)
Accessible via `.op()` or `.operation()`. Provides high-level manipulation.
*   `repeat(nx, ny, nz)`: Creates a supercell.
*   `wrapPBC2this()`: Wraps atoms into the box.
*   `perturbXYZGaussian2this(sigma)`: Random perturbation.

---

## 3. Format-Specific Classes & Behavior

### 3.1 LAMMPS Data (`jse.lmp.Lmpdat` / `jse.lmp.Data`)
Standard LAMMPS `read_data` format. High-performance, optimized for MD initialization.

*   **Specifics:** Supports masses, positions, velocities, bonds (experimental), and keep the original ID order.
*   **Usage:**
    ```groovy
    import jse.lmp.Lmpdat
    
    def lmp = Lmpdat.read('system.data')
    lmp.setMasses(26.98d, 63.55d) // Set masses for types, can use internal value like CS.MASS.Cu
    lmp.write('system_new.data')
    ```

### 3.2 VASP POSCAR (`jse.vasp.POSCAR`)
Standard structure file.

*   **Critical Memory Constraint:** In POSCAR logic, atoms are grouped contiguously by type (e.g., all type 1, then all type 2).
*   **Warning: Type Shuffling:**
    *   Changing an atom's type implies moving its location in memory/list to maintain the grouping.
    *   **Do NOT use:** `op().mapTypeRandom2this(...)` (In-place random shuffling). This causes undefined behavior in `POSCAR` structure.
    *   **Use instead:** `op().mapTypeRandom(...)` (Creates a *new* generic `AtomData` object) or convert to `Lmpdat`/`DataXYZ` first.
    ```groovy
    import jse.lmp.Lmpdat
    import jse.vasp.POSCAR
    
    def poscar = POSCAR.read('POSCAR')
  
    // SAFE: Convert to Lmpdat, then shuffle type in-place
    def lmp = Lmpdat.of(poscar).op().mapTypeRandom2this(0.5d, 0.5d)
  
    // SAFE: Create new structure
    def randomized = poscar.op().mapTypeRandom(0.5d, 0.5d)
    ```

### 3.3 Trajectories (`jse.lmp.Lammpstrj`, `jse.vasp.XDATCAR`, `jse.atom.data.DumpXYZ`)
List-based containers for molecular dynamics trajectories.

*   **Streaming Large Files:**
    Normal `read()` loads all frames into RAM. For massive files (>10GB), use internal streaming functions in a script loop (Groovy can access protected methods):
    ```groovy
    // Streaming Read Example (Advanced)
    import jse.code.IO
    import jse.lmp.SubLammpstrj
  
    try (def reader = IO.toReader('massive.dump')) {
        // SubLammpstrj.read_ reads one frame and pauses stream
        while ((frame = SubLammpstrj.read_(reader)) != null) {
            // Process 'frame' (ISettableAtomData)
            println(frame.natoms())
        }
    }
    ```

---

## 4. Type Conversion (Interoperability)

jse uses static `.of()` methods to convert between any `IAtomData` implementation. This performs a "Deep Copy," converting internal storage to the target format's layout.

| From | To | Method |
| :--- | :--- | :--- |
| Any (`IAtomData`) | `Lmpdat` | `Lmpdat.of(data)` |
| Any | `POSCAR` | `POSCAR.of(data)` |
| Any | `DataXYZ` | `DataXYZ.of(data)` |
| Any | `AseAtoms` | `AseAtoms.of(data)` |
| `Iterable<IAtomData>` | `Lammpstrj` | `Lammpstrj.of(list)` |
| `Iterable<IAtomData>` | `DumpXYZ` | `DumpXYZ.of(list)` |

**Example:**
```groovy
import jse.lmp.Lmpdat
import jse.vasp.POSCAR

def poscar = POSCAR.read('POSCAR')
// Convert to LAMMPS format effectively
def lmp = Lmpdat.of(poscar, 1.0d, 2.0d) 
// If not specified, will auto looked up from CS.MASS based on the symbols.
```

---

## 5. ASE Integration (Python Bridge)

`jse.ase.AseAtoms` bridges Java and the Python `ase` library.

**Data Copying & Loss:**
To ensure performance, `AseAtoms` **copies** the core ASE data (positions, numbers, cell, momenta) into Java primitives.
*   **Result:** Custom ASE arrays/properties (e.g., magnetic moments, constraints) are **lost** during this transfer.

**Recommended Workflows:**

1.  **Read via ASE, Process in Java:**
    Use this when you need ASE's full file parsers (e.g., `.cif`, `.xsf`) but want Java speed or jse futures for analysis.
    ```groovy
    import jse.ase.AseAtoms
    import jse.code.SP
    import jse.math.vector.Vectors
    
    // Simple read, basic information
    // def atoms = AseAtoms.read('file.cif')
    
    // Read via Python directly to get PyObject
    SP.Python.exec('from ase.io import read as ase_read')
    def ase_read = SP.Python.get('ase_read')
    def pyObj = ase_read('file.cif')
    // Convert PyObject to Java (Copy)
    def atoms = AseAtoms.of(pyObj) 
    // Obtain additional information through pyObj and .fromNumpy()
    def moments = Vectors.fromNumpy(pyObj.get_magnetic_moments()) 
    // or use Matrices.fromNumpy() for 3-D data
    
    // Process...
    def supercell = atoms.op().repeat(2)
    ```

2.  **Export Java to ASE:**
    Use this to use ASE's file writers or calculators (EMT, VASP).
    ```groovy
    import jse.ase.AseAtoms
    import jse.atom.Structure
    import jse.code.SP
    
    // 1. Create in Java
    def data = Structure.fcc(4.0d, 2)
    // 2. Convert to PyObject (Copy)
    def pyAtoms = AseAtoms.of(data).toPyObject()
    // 3. Use in Python
    pyAtoms.write('out.cif')
    ```

3.  **Forbidden Pattern:**
    `AseAtoms.read('file.cif').toPyObject()`
    *   *Why?* This reads into Python -> copies to Java -> copies back to Python. You lose data twice. Just use Python Object wrapper for this pipeline.
