package example.system

import static jse.code.UT.Exec.*

def exitCode = system('echo 123456')
println("exitCode: $exitCode")

//OUTPUT:
// 123456
// exitCode: 0

