#include "lammpsplugin.h"

#include "pair_jse.h"
#include "fix_jse.h"

using namespace LAMMPS_NS;

static Pair *jsepaircreator(LAMMPS *lmp) {
    return new PairJSE(lmp);
}
static Fix *jsefixcreator(LAMMPS *lmp, int argc, char **argv) {
    return new FixJSE(lmp, argc, argv);
}

extern "C" {

JNIEXPORT void JNICALL lammpsplugin_init(void *lmp, void *handle, void *regfunc) {
    lammpsplugin_t plugin;
    lammpsplugin_regfunc register_plugin = (lammpsplugin_regfunc) regfunc;
    
    plugin.version = LAMMPS_VERSION;
    plugin.style = "pair";
    plugin.name = "jse";
    plugin.info = "jse pair";
    plugin.author = "liqa";
    plugin.creator.v1 = (lammpsplugin_factory1 *) &jsepaircreator;
    plugin.handle = handle;
    (*register_plugin)(&plugin, lmp);
    
    plugin.version = LAMMPS_VERSION;
    plugin.style = "fix";
    plugin.name = "jse";
    plugin.info = "jse fix";
    plugin.author = "liqa";
    plugin.creator.v2 = (lammpsplugin_factory2 *) &jsefixcreator;
    plugin.handle = handle;
    (*register_plugin)(&plugin, lmp);
}

}
