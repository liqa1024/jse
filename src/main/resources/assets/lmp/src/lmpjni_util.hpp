#ifndef LMPJNI_UTIL_H
#define LMPJNI_UTIL_H

#include "lammps/lammps.h"
#include "lammps/error.h"
#include "lammps/input.h"
#include "lammps/update.h"
#include "jniutil.h"


#define BEGIN_CAPTURE        \
    LAMMPS_NS::Error *tErr = tLmp->error; \
    try

#define END_CAPTURE \
    catch(LAMMPS_NS::LAMMPSAbortException &ae) { \
    int nprocs = 0; \
    MPI_Comm_size(ae.get_universe(), &nprocs ); \
    \
    if (nprocs > 1) { \
        tErr->set_last_error(ae.what(), LAMMPS_NS::ERROR_ABORT); \
    } else { \
        tErr->set_last_error(ae.what(), LAMMPS_NS::ERROR_NORMAL); \
    } \
    } catch(LAMMPS_NS::LAMMPSException &e) { \
        tErr->set_last_error(e.what(), LAMMPS_NS::ERROR_NORMAL); \
    }


namespace JSE_LMPJNI {

static void lammpsInputFile(JNIEnv *aEnv, void *aLmpPtr) {
    LAMMPS_NS::LAMMPS *tLmp = (LAMMPS_NS::LAMMPS *)aLmpPtr;
    if (!tLmp || !tLmp->error || !tLmp->update || !tLmp->input) {
        throwExceptionLMP(aEnv, "Invalid LAMMPS handle");
        return;
    }
    BEGIN_CAPTURE
    {
        if (tLmp->update->whichflag!=0) {
            throwExceptionLMP(aEnv, "Issuing LAMMPS commands during a run is not allowed");
        } else {
            tLmp->input->file();
        }
    }
    END_CAPTURE
}

}

#endif //LMPJNI_UTIL_H
