#include "lammpsplugin.h"
#include "version.h"

#include "pair_jse.h"

using namespace LAMMPS_NS;

static Pair *jsecreator(LAMMPS *lmp) {
    return new PairJSE(lmp);
}

extern "C" {

JNIEXPORT void JNICALL lammpsplugin_init(void *lmp, void *handle, void *regfunc) {
    lammpsplugin_t plugin;
    lammpsplugin_regfunc register_plugin = (lammpsplugin_regfunc) regfunc;
    
    plugin.version = LAMMPS_VERSION;
    plugin.style = "pair";
    plugin.name = "jse";
    plugin.info = "jse pair v0.1";
    plugin.author = "liqa, CHanzyLazer";
    plugin.creator.v1 = (lammpsplugin_factory1 *) &jsecreator;
    plugin.handle = handle;
    (*register_plugin)(&plugin, lmp);
}

}
