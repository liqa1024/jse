# jse - Atomic Parameter Calculator (APC)

The `jse.atom.AtomicParameterCalculator` (accessed via `jse.atom.APC`) is the high-performance engine for structural analysis in `jse`. It calculates parameters like RDF, Structure Factor, and Bond-Orientational Order Parameters (BOOP).

**Performance Note:**
APC relies on internal primitive memory caches (`MatrixCache`, `VectorCache`) to achieve performance comparable to C/C++.
*   **Memory Management:** While Java's Garbage Collector (GC) handles cleaning up unused APC instances, it is recommended to use `try-with-resources` or use the `withOf` pattern in tight loops. This manually returns memory to the cache pool, significantly reducing GC pressure and increasing throughput.
*   **Thread Safety:** An APC instance is thread-safe for reading, but separate instances should be created for parallel tasks if internal states are modified.

**Import Strategy:**
```groovy
import jse.atom.APC
```

---

## 1. Initialization & Lifecycle

### 1.1 Creation
Do **not** use `new AtomicParameterCalculator(...)`. Use static factory methods.

*   **Standard:** `APC.of(IAtomData data)`
*   **With Thread Count:** `APC.of(data, nthreads)`
*   **Auto-Close:** `APC.withOf(data) {apc -> ...}`

```groovy
import jse.atom.APC

// 1. for simple use, GC auto-cleanup
// def apc = APC.of(data)

// 2. try-with-resources
try (def apc = APC.of(data)) {
    // ... use apc ...
} 
// apc.shutdown() is called automatically here

// 2. Closure Pattern (Automatically calls shutdown)
def q6 = APC.withOf(data) {apc -> apc.calBOOP(6)}
```

### 1.2 "Hot" Updates
You can update atomic coordinates in-place to reuse the allocated neighbor lists and memory buffers (e.g., during an MC/MD loop).

*   `setAtomXYZ(int idx, double x, double y, double z)`
*   `setAtomType(int idx, int type)`

---

## 2. Basic Properties

APC pre-calculates density and box metrics.

| Method | Description |
| :--- | :--- |
| `natoms()` | Total atom count. |
| `natoms(type)` | Atom count for specific type. |
| `ntypes()` | Number of atom types. |
| `rho()` | Number density ($N/V$). |
| `rho(type)` | Partial density. |
| `unitLen()` | Average interatomic distance ($\sqrt[3]{V/N}$). Used as default scaling unit for cutoffs. |

---

## 3. Structural Analysis (RDF & SF)

These methods return `IFunc1` (function objects) representing $g(r)$ or $S(q)$.

### 3.1 Radial Distribution Function (RDF)
*   **Standard:** `calRDF(nbins, rmax)`
*   **Gaussian Smeared:** `calRDF_G(nbins, rmax, sigmaMul)` (Smoother for small systems).
*   **Partial:** `calRDF_AB(typeA, typeB, ...)`
*   **All Pairs:** `calAllRDF(...)`
    *   Returns `List<IFunc1>`.
    *   **Indexing:** The list is flattened. To get pair $(A, B)$ where $A \ge B$ (1-based types):
        $$Index = \frac{A(A-1)}{2} + B$$
    *   Index 0 is the total global RDF.

```groovy
// Calculate global RDF, 160 bins, cutoff 6.0
def rdf = apc.calRDF(160, 6.0)
// Function operations
double peak = rdf.op().maxX()
```

### 3.2 Structure Factor (SF)
Calculated via direct Fourier transform of positions (expensive $O(N^2)$) or via RDF conversion.

*   **Direct:** `calSF(nBins, qMax)`
*   **Via RDF (FFT):** `APC.RDF2SF(rdf, rho)` or `apc.RDF2SF(rdf)`

```groovy
// Fast S(q) via RDF
def gr = apc.calRDF(500, 15.0) // Need long range RDF for good Sq
def sq = apc.RDF2SF(gr)
```

---

## 4. Order Parameters (BOOP)

Calculates Steinhardt parameters. Result is `IVector` (one value per atom).
Default cutoff ($R_{cut}$) is `unitLen() * 1.5`.

### 4.1 Parameter Types

1.  **Local ($Q_l, W_l$):** Standard definition.
    *   `calBOOP(l, rcut)` $\rightarrow Q_l$
    *   `calBOOP3(l, rcut)` $\rightarrow W_l$ (3rd order invariants)
2.  **Averaged ($q_l, w_l$):** Averaged over neighbors (Lechner-Dellago). Better for crystal identification.
    *   `calABOOP(l, rcut)` $\rightarrow \bar{Q}_l$ (often denoted $q_l$)
    *   `calABOOP3(l, rcut)` $\rightarrow \bar{W}_l$ (often denoted $w_l$)

### 4.2 Solid Detection
Detects crystalline atoms based on bond coherence ($S_{ij}$).

*   `checkSolidConnectCount6()`: Returns `ILogicalVector`. `true` if atom has enough "solid-like" bonds ($l=6$).
*   `calConnectCountBOOP(...)`: Returns raw number of coherent bonds per atom.

```groovy
// Detect solid atoms (Nucleation analysis)
def isSolid = apc.checkSolidConnectCount6(0.5, 7) // threshold=0.5, min_neighbors=7
int solidCount = isSolid.count()
```

---

## 5. Neighbor Lists

APC provides three levels of access to neighbor lists, ranging from ease-of-use to zero-copy high performance.

### 5.1 Standard Access (List Kopies)
Useful for general scripting where performance is not the bottleneck.

1.  **Indices Only:** `getNeighborList(idx, rcut)`
    *   Returns `IntVector`.
    *   *Note:* Only provides indices. Calculating distance manually requires handling PBC yourself.

2.  **Full Data (PBC Applied):** `getFullNeighborList(idx, rcut)`
    *   Returns `List<Vector>` in order: `[x, y, z, idx]`.
    *   **Feature:** The coordinates returned are **absolute** and **unwrapped** according to PBC. So the distance vector can be simply applied by $\vec{r}_{neighbor} - \vec{r}_{center}$.

```groovy
// Standard usage
def (nlx, nly, nlz, nlj) = apc.getFullNeighborList(0, 3.5)
```

### 5.2 High-Performance Direct Access (`apc.nl_()`)

For heavy custom analysis loops, creating intermediate Lists (above) is too slow. Use the internal `NeighborListGetter` via `nl_()`. This method uses a **Zero-Copy** callback approach.

**Method:** `nl_().forEachNeighbor(centerIdx, rcut, isHalf, callback)`
*   **Callback Signature:** `(double dx, double dy, double dz, int neighborIdx)`
*   `dx, dy, dz`: The vector from source to neighbor (PBC applied).
*   `isHalf`: Set `false` (default) for full neighbor list.

```groovy
// Iterate all atoms
for (i in 0..<apc.natoms()) {
    // Iterate neighbors of i
    apc.nl_().forEachNeighbor(i, 3.5) {dx, dy, dz, j ->
        // This block is executed for every neighbor j of atom i
        // dx, dy, dz are already calculated with PBC
        double r2 = dx*dx + dy*dy + dz*dz
        // Custom logic here...
    }
}
```

---

## 6. Machine Learning Descriptors (NNAP)

Extensions for using Neural Network Atomic Potentials basis functions as descriptors.

*   `NNAPExtensions.calBasisNNAP(apc, nmax, lmax, rcut)`: Calculates Spherical-Chebyshev descriptors. Returns `List<Vector>` (one vector per atom). In Groovy, can be used through `apc.calBasisNNAP(nmax, lmax, rcut)`
