#include "nep_core.hpp"

// >>> NEPGEN REMOVE
#define __NEPGEN_USE_TABLE__ 0
#define __NEPGEN_VERSION__ 4
#define __NEPGEN_NTYPES__ 2
#define __NEPGEN_USE_TYPEWISE_CUTOFF__ 0
#define __NEPGEN_USE_TYPEWISE_CUTOFF_ZBL__ 0
#define __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__ 3.0
#define __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__ 2.0
#define __NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL__ 0.5
#define __NEPGEN_RCUT_R__ 6.0
#define __NEPGEN_RCUT_A__ 4.0
#define __NEPGEN_RCUT_INNER_ZBL__ 1.0
#define __NEPGEN_RCUT_OUTER_ZBL__ 2.0
#define __NEPGEN_NMAX_R__ 5
#define __NEPGEN_NMAX_A__ 5
#define __NEPGEN_BSIZE_R__ 8
#define __NEPGEN_BSIZE_A__ 8
#define __NEPGEN_NUMC_R__ 64
#define __NEPGEN_LMAX__ 4
#define __NEPGEN_NUML__ 6
#define __NEPGEN_ANN_DIM_A__ 20
#define __NEPGEN_ANN_DIM__ 30
#define __NEPGEN_NUM_NEURONS1__ 32
#define __NEPGEN_NUM_PARA_ANN__ 100
#define __NEPGEN_ZBL__ 1
#define __NEPGEN_ZBL_FLEXIBLED__ 0
// <<< NEPGEN REMOVE

#define __jsefunc__

extern "C" {

__jsefunc__ int jse_nep_constructTable(const double *parameters,
    JSE_NEP::flt_t *gn_radial, JSE_NEP::flt_t *gn_angular,
    JSE_NEP::flt_t *gnp_radial, JSE_NEP::flt_t *gnp_angular) {
    
    JSE_NEP::construct_table<__NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_NMAX_R__, __NEPGEN_NMAX_A__,
                             __NEPGEN_BSIZE_R__, __NEPGEN_BSIZE_A__, __NEPGEN_NUMC_R__, __NEPGEN_NUM_PARA_ANN__>(
        parameters,
        __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
        gn_radial, gn_angular,
        gnp_radial, gnp_angular
    );
    return 0;
}

__jsefunc__ int jse_nep_calEnergy(
    const JSE_NEP::flt_t *nl_dx, const JSE_NEP::flt_t *nl_dy, const JSE_NEP::flt_t *nl_dz, const int *nl_type, int num_neigh, int ctype,
    const int *atomic_numbers, const JSE_NEP::flt_t *q_scaler,
    const JSE_NEP::flt_t **ann_w0, const JSE_NEP::flt_t **ann_b0, const JSE_NEP::flt_t **ann_w1, const JSE_NEP::flt_t *ann_b1, const JSE_NEP::flt_t *ann_c,
    const JSE_NEP::flt_t *zbl_para, const JSE_NEP::flt_t *gn_radial, const JSE_NEP::flt_t *gn_angular,
    JSE_NEP::flt_t *eng, JSE_NEP::flt_t *nl_fx, JSE_NEP::flt_t *nl_fy, JSE_NEP::flt_t *nl_fz,
    JSE_NEP::flt_t *fp, JSE_NEP::flt_t *sum_fxyz) {
    
    // manual clear required
    eng[0] = (JSE_NEP::flt_t)0.0;
    for (int i = 0; i < __NEPGEN_ANN_DIM__; ++i) {
        fp[i] = (JSE_NEP::flt_t)0.0;
    }
    constexpr int size_sum_fxyz = (__NEPGEN_NMAX_A__+1) * JSE_NEP::NUM_OF_ABC;
    for (int i = 0; i < size_sum_fxyz; ++i) {
        sum_fxyz[i] = (JSE_NEP::flt_t)0.0;
    }
    JSE_NEP::find_descriptor<__NEPGEN_USE_TABLE__, __NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                             __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__, __NEPGEN_NUMC_R__,
                             __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__,
                             __NEPGEN_ANN_DIM__, __NEPGEN_NUM_NEURONS1__>(1, 0,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__, __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
        __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
        q_scaler,
        ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
        ctype, num_neigh,
        nl_type,
        nl_dx, nl_dy, nl_dz,
        __NEPGEN_USE_TABLE__?gn_radial:nullptr,
        __NEPGEN_USE_TABLE__?gn_angular:nullptr,
        fp, sum_fxyz,
        eng
    );
    if (__NEPGEN_ZBL__) {
        // manual clear required
        for (int j = 0; j < num_neigh; ++j) {
            nl_fx[j] = (JSE_NEP::flt_t)0.0;
            nl_fy[j] = (JSE_NEP::flt_t)0.0;
            nl_fz[j] = (JSE_NEP::flt_t)0.0;
        }
        JSE_NEP::find_force_ZBL<__NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF_ZBL__, __NEPGEN_ZBL_FLEXIBLED__>(1, 0,
            atomic_numbers,
            __NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL__,
            __NEPGEN_ZBL_FLEXIBLED__?zbl_para:nullptr,
            __NEPGEN_RCUT_INNER_ZBL__, __NEPGEN_RCUT_OUTER_ZBL__,
            ctype, num_neigh,
            nl_type,
            nl_dx, nl_dy, nl_dz,
            nl_fx, nl_fy, nl_fz,
            eng
        );
    }
    
    return 0;
}

__jsefunc__ int jse_nep_calEnergyForce(
    const JSE_NEP::flt_t *nl_dx, const JSE_NEP::flt_t *nl_dy, const JSE_NEP::flt_t *nl_dz, const int *nl_type, int num_neigh, int ctype,
    const int *atomic_numbers, const JSE_NEP::flt_t *q_scaler,
    const JSE_NEP::flt_t **ann_w0, const JSE_NEP::flt_t **ann_b0, const JSE_NEP::flt_t **ann_w1, const JSE_NEP::flt_t *ann_b1, const JSE_NEP::flt_t *ann_c,
    const JSE_NEP::flt_t *zbl_para, const JSE_NEP::flt_t *gn_radial, const JSE_NEP::flt_t *gn_angular, const JSE_NEP::flt_t *gnp_radial, const JSE_NEP::flt_t *gnp_angular,
    JSE_NEP::flt_t *eng, JSE_NEP::flt_t *nl_fx, JSE_NEP::flt_t *nl_fy, JSE_NEP::flt_t *nl_fz,
    JSE_NEP::flt_t *fp, JSE_NEP::flt_t *sum_fxyz) {
    
    // manual clear required
    eng[0] = (JSE_NEP::flt_t)0.0;
    for (int i = 0; i < __NEPGEN_ANN_DIM__; ++i) {
        fp[i] = (JSE_NEP::flt_t)0.0;
    }
    constexpr int size_sum_fxyz = (__NEPGEN_NMAX_A__+1) * JSE_NEP::NUM_OF_ABC;
    for (int i = 0; i < size_sum_fxyz; ++i) {
        sum_fxyz[i] = (JSE_NEP::flt_t)0.0;
    }
    for (int j = 0; j < num_neigh; ++j) {
        nl_fx[j] = (JSE_NEP::flt_t)0.0;
        nl_fy[j] = (JSE_NEP::flt_t)0.0;
        nl_fz[j] = (JSE_NEP::flt_t)0.0;
    }
    JSE_NEP::find_descriptor<__NEPGEN_USE_TABLE__, __NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                             __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__, __NEPGEN_NUMC_R__,
                             __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__,
                             __NEPGEN_ANN_DIM__, __NEPGEN_NUM_NEURONS1__>(1, 0,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__, __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
        __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
        q_scaler,
        ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
        ctype, num_neigh,
        nl_type,
        nl_dx, nl_dy, nl_dz,
        __NEPGEN_USE_TABLE__?gn_radial:nullptr,
        __NEPGEN_USE_TABLE__?gn_angular:nullptr,
        fp, sum_fxyz,
        eng
    );
    JSE_NEP::find_force_radial<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                               __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__>(1, 0,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__,
        __NEPGEN_RCUT_R__,
        ann_c,
        ctype, num_neigh,
        nl_type,
        nl_dx, nl_dy, nl_dz,
        fp,
        __NEPGEN_USE_TABLE__?gnp_radial:nullptr,
        nl_fx, nl_fy, nl_fz
    );
    JSE_NEP::find_force_angular<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                                __NEPGEN_NMAX_R__, __NEPGEN_NUMC_R__,
                                __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__, __NEPGEN_ANN_DIM_A__>(1, 0,
        atomic_numbers,
        __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
        __NEPGEN_RCUT_A__,
        ann_c,
        ctype, num_neigh,
        nl_type,
        nl_dx, nl_dy, nl_dz,
        fp, sum_fxyz,
        __NEPGEN_USE_TABLE__?gn_angular:nullptr, __NEPGEN_USE_TABLE__?gnp_angular:nullptr,
        nl_fx, nl_fy, nl_fz
    );
    if (__NEPGEN_ZBL__) {
        JSE_NEP::find_force_ZBL<__NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF_ZBL__, __NEPGEN_ZBL_FLEXIBLED__>(1, 0,
            atomic_numbers,
            __NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL__,
            __NEPGEN_ZBL_FLEXIBLED__?zbl_para:nullptr,
            __NEPGEN_RCUT_INNER_ZBL__, __NEPGEN_RCUT_OUTER_ZBL__,
            ctype, num_neigh,
            nl_type,
            nl_dx, nl_dy, nl_dz,
            nl_fx, nl_fy, nl_fz,
            eng
        );
    }
    return 0;
}

#define JSE_LMP_NEIGHMASK 0x1FFFFFFF

__jsefunc__ int jse_nep_statNeiNumLammps(int *ilist, int *numneigh, int inum, int *numneighMax) {
    int numneighMax_ = 0;
    for (int ii = 0; ii < inum; ++ii) {
        int i = ilist[ii];
        int jnum = numneigh[i];
        if (jnum > numneighMax_) numneighMax_ = jnum;
    }
    numneighMax[0] = numneighMax_;
    return 0;
}

__jsefunc__ int jse_nep_computeLammps(
    int inum, int eflag, int vflag, int eflagAtom, int vflagAtom, int cvflagAtom,
    const double **x, double **f, const int *type,
    const int *ilist, const int *numneigh, const int **firstneigh, double cutsq, const int *type_map,
    double *engVdwl, double *eatom, double *virial, double **vatom, double **cvatom,
    JSE_NEP::flt_t *nl_dx, JSE_NEP::flt_t *nl_dy, JSE_NEP::flt_t *nl_dz, int *nl_type, int *nl_idx,
    const int *atomic_numbers, const JSE_NEP::flt_t *q_scaler,
    const JSE_NEP::flt_t **ann_w0, const JSE_NEP::flt_t **ann_b0, const JSE_NEP::flt_t **ann_w1, const JSE_NEP::flt_t *ann_b1, const JSE_NEP::flt_t *ann_c,
    const JSE_NEP::flt_t *zbl_para, const JSE_NEP::flt_t *gn_radial, const JSE_NEP::flt_t *gn_angular, const JSE_NEP::flt_t *gnp_radial, const JSE_NEP::flt_t *gnp_angular,
    JSE_NEP::flt_t *nl_fx, JSE_NEP::flt_t *nl_fy, JSE_NEP::flt_t *nl_fz,
    JSE_NEP::flt_t *fp, JSE_NEP::flt_t *sum_fxyz) {
    
    /// begin compute here
    for (int ii = 0; ii < inum; ++ii) {
        const int i = ilist[ii];
        const int typei = type[i];
        const int typeiNEP = type_map[typei];
        const double xtmp = x[i][0];
        const double ytmp = x[i][1];
        const double ztmp = x[i][2];
        const int *jlist = firstneigh[i];
        const int jnum = numneigh[i];
        
        /// build neighbor list
        int num_neigh = 0;
        for (int jj = 0; jj < jnum; ++jj) {
            int j = jlist[jj];
            j &= JSE_LMP_NEIGHMASK;
            // Note that dxyz in jse and lammps are defined oppositely
            double delx = x[j][0] - xtmp;
            double dely = x[j][1] - ytmp;
            double delz = x[j][2] - ztmp;
            double rsq = delx*delx + dely*dely + delz*delz;
            if (rsq < cutsq) {
                nl_dx[num_neigh] = (JSE_NEP::flt_t)delx;
                nl_dy[num_neigh] = (JSE_NEP::flt_t)dely;
                nl_dz[num_neigh] = (JSE_NEP::flt_t)delz;
                nl_type[num_neigh] = type_map[type[j]];
                nl_idx[num_neigh] = j;
                ++num_neigh;
            }
        }
        
        /// begin nnap here
        JSE_NEP::flt_t eng = 0.0;
        // manual clear required
        for (int k = 0; k < __NEPGEN_ANN_DIM__; ++k) {
            fp[k] = (JSE_NEP::flt_t)0.0;
        }
        constexpr int size_sum_fxyz = (__NEPGEN_NMAX_A__+1) * JSE_NEP::NUM_OF_ABC;
        for (int k = 0; k < size_sum_fxyz; ++k) {
            sum_fxyz[k] = (JSE_NEP::flt_t)0.0;
        }
        for (int j = 0; j < num_neigh; ++j) {
            nl_fx[j] = (JSE_NEP::flt_t)0.0;
            nl_fy[j] = (JSE_NEP::flt_t)0.0;
            nl_fz[j] = (JSE_NEP::flt_t)0.0;
        }
        JSE_NEP::find_descriptor<__NEPGEN_USE_TABLE__, __NEPGEN_VERSION__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                                 __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__, __NEPGEN_NUMC_R__,
                                 __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__,
                                 __NEPGEN_ANN_DIM__, __NEPGEN_NUM_NEURONS1__>(1, 0,
            atomic_numbers,
            __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__, __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
            __NEPGEN_RCUT_R__, __NEPGEN_RCUT_A__,
            q_scaler,
            ann_w0, ann_b0, ann_w1, ann_b1, ann_c,
            typeiNEP, num_neigh,
            nl_type,
            nl_dx, nl_dy, nl_dz,
            __NEPGEN_USE_TABLE__?gn_radial:nullptr,
            __NEPGEN_USE_TABLE__?gn_angular:nullptr,
            fp, sum_fxyz,
            &eng
        );
        JSE_NEP::find_force_radial<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                                   __NEPGEN_NMAX_R__, __NEPGEN_BSIZE_R__>(1, 0,
            atomic_numbers,
            __NEPGEN_TYPEWISE_CUTOFF_FACTOR_R__,
            __NEPGEN_RCUT_R__,
            ann_c,
            typeiNEP, num_neigh,
            nl_type,
            nl_dx, nl_dy, nl_dz,
            fp,
            __NEPGEN_USE_TABLE__?gnp_radial:nullptr,
            nl_fx, nl_fy, nl_fz
        );
        JSE_NEP::find_force_angular<__NEPGEN_USE_TABLE__, __NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF__,
                                   __NEPGEN_NMAX_R__, __NEPGEN_NUMC_R__,
                                   __NEPGEN_NMAX_A__, __NEPGEN_BSIZE_A__, __NEPGEN_LMAX__, __NEPGEN_NUML__, __NEPGEN_ANN_DIM_A__>(1, 0,
            atomic_numbers,
            __NEPGEN_TYPEWISE_CUTOFF_FACTOR_A__,
            __NEPGEN_RCUT_A__,
            ann_c,
            typeiNEP, num_neigh,
            nl_type,
            nl_dx, nl_dy, nl_dz,
            fp, sum_fxyz,
            __NEPGEN_USE_TABLE__?gn_angular:nullptr, __NEPGEN_USE_TABLE__?gnp_angular:nullptr,
            nl_fx, nl_fy, nl_fz
        );
        if (__NEPGEN_ZBL__) {
            JSE_NEP::find_force_ZBL<__NEPGEN_NTYPES__, __NEPGEN_USE_TYPEWISE_CUTOFF_ZBL__, __NEPGEN_ZBL_FLEXIBLED__>(1, 0,
                atomic_numbers,
                __NEPGEN_TYPEWISE_CUTOFF_FACTOR_ZBL__,
                __NEPGEN_ZBL_FLEXIBLED__?zbl_para:nullptr,
                __NEPGEN_RCUT_INNER_ZBL__, __NEPGEN_RCUT_OUTER_ZBL__,
                typeiNEP, num_neigh,
                nl_type,
                nl_dx, nl_dy, nl_dz,
                nl_fx, nl_fy, nl_fz,
                &eng
            );
        }
        
        /// collect results
        if (eflag) {
            *engVdwl += eng;
            if (eflagAtom) eatom[i] += eng;
        }
        for (int jj = 0; jj < num_neigh; ++jj) {
            const int j = nl_idx[jj];
            const JSE_NEP::flt_t fx = nl_fx[jj];
            const JSE_NEP::flt_t fy = nl_fy[jj];
            const JSE_NEP::flt_t fz = nl_fz[jj];
            f[i][0] -= fx;
            f[i][1] -= fy;
            f[i][2] -= fz;
            f[j][0] += fx;
            f[j][1] += fy;
            f[j][2] += fz;
            if (vflag) {
                const JSE_NEP::flt_t dx = nl_dx[jj];
                const JSE_NEP::flt_t dy = nl_dy[jj];
                const JSE_NEP::flt_t dz = nl_dz[jj];
                virial[0] += dx*fx;
                virial[1] += dy*fy;
                virial[2] += dz*fz;
                virial[3] += dx*fy;
                virial[4] += dx*fz;
                virial[5] += dy*fz;
                if (vflagAtom) {
                    vatom[j][0] += dx*fx;
                    vatom[j][1] += dy*fy;
                    vatom[j][2] += dz*fz;
                    vatom[j][3] += dx*fy;
                    vatom[j][4] += dx*fz;
                    vatom[j][5] += dy*fz;
                }
                if (cvflagAtom) {
                    cvatom[j][0] += dx*fx;
                    cvatom[j][1] += dy*fy;
                    cvatom[j][2] += dz*fz;
                    cvatom[j][3] += dx*fy;
                    cvatom[j][4] += dx*fz;
                    cvatom[j][5] += dy*fz;
                    cvatom[j][6] += dy*fx;
                    cvatom[j][7] += dz*fx;
                    cvatom[j][8] += dz*fy;
                }
            }
        }
    }
    return 0;
}

}
