# jse - Atomic Parameter Calculator (APC)

The `jse.atom.AtomicParameterCalculator` (accessed via short alias `jse.atom.APC`) is the engine for structural analysis. It calculates Radial Distribution Functions (RDF), Structure Factors, Bond-Orientational Order Parameters (BOOP), and handles neighbor lists.

## 1. Import & Initialization

For most scripts, simply create an instance using `APC.of()`.

```groovy
import jse.atom.APC

// 1. Standard creation (Recommended for general use)
def apc = APC.of(data)

// 2. Multithreaded creation (Specify thread count)
def apcPar = APC.of(data, 4)
```

### Performance Note: Manual Resource Management
APC uses internal native-like memory caching. While Java's Garbage Collector handles cleanup automatically, you can improve performance in **looped/heavy tasks** by manually releasing memory.

**Use this pattern for heavy loops:**

```groovy
// The 'withOf' pattern automatically closes/releases the APC after the block
APC.withOf(data) {apc ->
    def q6 = apc.calBOOP(6, 3.5d)
} // ... apc is recycled here
```

## 2. Basic Properties
Quick access to system metrics.

| Method | Description |
| :--- | :--- |
| `apc.natoms()` | Total atom count. |
| `apc.natoms(type)` | Atom count for specific type index. |
| `apc.rho()` | Number density ($N/V$). |
| `apc.unitLen()` | Average interatomic distance ($\sqrt[3]{V/N}$), useful for scaling cutoffs. |

## 3. Structural Analysis (RDF & SF)

### 3.1 Radial Distribution Function (RDF)
Calculates $g(r)$. Returns a function object that can be queried or plotted.

```groovy
// Args: bins, cutoff distance
def rdf = apc.calRDF(200, 8.0d)

// Get peak position
double peak = rdf.op().maxX()

// Calculate Partial RDF (e.g., Type 1 - Type 2)
def rdf12 = apc.calRDF_AB(1, 2, 200, 8.0d)
```

### 3.2 Structure Factor ($S(q)$)
Calculates the Static Structure Factor.

```groovy
// 1. Fast Method: Convert from existing RDF (FFT-based)
def rdf = apc.calRDF(500, 20.0d) // Long range needed for good S(q)
def sq = apc.RDF2SF(rdf)

// 2. Precise Method: Direct Calculation (Slower, O(N^2))
def sqDirect = apc.calSF(200, 12.0d) // bins, qMax
```

## 4. Bond-Orientational Order Parameters (BOOP)
Calculates Steinhardt parameters ($Q_l$). The result is an `IVector` containing one value per atom.

*   **Standard ($Q_l$):** Local symmetry.
*   **Averaged ($q_l$ / $\bar{Q}_l$):** Averaged over neighbors (better for crystal identification).

```groovy
double cutoff = 3.5d

// Calculate local Q6
def Q6 = apc.calBOOP(6, cutoff)

// Calculate averaged q6 (Lechner-Dellago)
def q6 = apc.calABOOP(6, cutoff)

// Check if atoms are solid-like (based on coherent bonds)
// Args: threshold (usually 0.5-0.6), min_neighbors
def isSolid = apc.checkSolidConnectCount6(0.5d, 7)
println('Solid atoms: ' + isSolid.count())
```

## 5. Neighbor Lists

APC provides three levels of access to neighbor lists, ranging from ease-of-use to zero-copy high performance.

### 5.1 Simple Access (Recommended for Scripts)
1.  **Indices Only:** `getNeighborList(idx, rcut)`
    *   Returns `IntVector`.
    *   *Note:* Only provides indices. Calculating distance manually requires handling PBC yourself.

2.  **Full Data (PBC Applied):** `getFullNeighborList(idx, rcut)`
    *   Returns `List<Vector>` in order: `[x, y, z, idx]`.
    *   **Feature:** The coordinates returned are **absolute** and **unwrapped**. Distance is $\sqrt{(x-x_c)^2+(y-z_c)^2+(z-z_c)^2}$.

```groovy
int atomIdx = 0
double rcut = 3.5d

// Atom data -> apc
def apc = APC.of(data)
// Returns list of neighbor vectors: [x, y, z, index]
def (nlx, nly, nlz, nlj) = apc.getFullNeighborList(atomIdx, rcut)
// Center atom
def atomc = data.atom(atomIdx)

for (jj in 0..<nlj.size()>) {
    // get neighbor index
    int j = nlj[jj] as int
    // utils to cal sqrt((nlx[jj]-atomc.x())^2 + (nly[jj]-atomc.y())^2 + (nlz[jj]-atomc.z())^2)
    double dist = atomCenter.distance(nlx[jj], nly[jj], nlz[jj])
    println("Neighbor ${j} distance: ${dist}")
}
```

### 5.2 High-Performance Access (For Advanced Analysis)
Instead of creating lists, use `nl_().forEachNeighbor`. This avoids memory allocation and is significantly faster.

```groovy
// Iterate all atoms
for (i in 0..<apc.natoms()) {
    // Zero-copy callback
    // Args: centerIdx, cutoff, closure
    apc.nl_().forEachNeighbor(i, 3.5d) {dx, dy, dz, j ->
        // dx, dy, dz are pre-calculated relative vectors (PBC applied)
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz)
        // logic...
    }
}
```

## 6. Machine Learning Descriptors
Extensions for calculating neural network basis functions (Spherical-Chebyshev).

```groovy
// Calculate NNAP descriptors
// Args: n_max, l_max, cutoff
def descriptors = apc.calBasisNNAP(5, 6, 6.0d)
```
