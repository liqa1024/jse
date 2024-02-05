package test.code

import jse.clib.MiMalloc

println(MiMalloc.InitHelper.initialized());
MiMalloc.InitHelper.init();
println(MiMalloc.InitHelper.initialized());

