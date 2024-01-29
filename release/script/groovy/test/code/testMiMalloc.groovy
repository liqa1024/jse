package test.code

import jtool.clib.MiMalloc

println(MiMalloc.InitHelper.initialized());
MiMalloc.InitHelper.init();
println(MiMalloc.InitHelper.initialized());

