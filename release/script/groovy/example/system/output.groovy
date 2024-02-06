package example.system

import static jse.code.UT.Exec.*


// OUTPUT TO FILE
system('echo 123456', '.temp/example/system/output')


// OUTPUT TO List<String>
def lines = system_str('echo 123456')
println('class: '+lines.getClass())
println('size: '+lines.size())
println('value: '+lines)
println('first line: '+lines[0])

//OUTPUT:
// class: class java.util.ArrayList
// size: 1
// value: [123456]
// first line: 123456


// CLOSE OUTPUT
println('=========================')

println('normal:')
system('echo 111111')

println('no std:')
exec().setNoSTDOutput()
system('echo 222222')
system('echoecho 333333') // error


println('no err:')
exec().setNoSTDOutput(false).setNoERROutput()
system('echo 444444')
system('echoecho 555555') // error

// set back
exec().setNoSTDOutput(false).setNoERROutput(false)

//OUTPUT:
// normal:
// 111111
// no std:
// /bin/bash: echoecho: command not found
// no err:
// 444444

