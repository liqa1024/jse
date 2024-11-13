package example.system

import static jse.code.OS.*

def exitCode = system('echo 123456')
println("exitCode: $exitCode")

//OUTPUT:
// 123456
// exitCode: 0

