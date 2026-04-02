#ifndef NEP_INTERFACE_CUDA_H
#define NEP_INTERFACE_CUDA_H

#if defined(WIN32) || defined(_WIN64) || defined(_WIN32)
#define JSE_PLUGINEXPORT __declspec(dllexport)
#define JSE_PLUGINCALL __cdecl
#else
#define JSE_PLUGINEXPORT __attribute__((visibility("default")))
#define JSE_PLUGINCALL
#endif

extern "C" {

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_constructTable(void *, void *);

JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_statNeiNumLammps(void *, void *);
JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_lammps2cuda(void *, void *);
JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_cuda2lammps(void *, void *);
JSE_PLUGINEXPORT int JSE_PLUGINCALL jse_nep_computeLammpsCuda(void *, void *);

}

#endif //NEP_INTERFACE_CUDA_H