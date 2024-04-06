package example.cross

import jse.code.SP

SP.Python.set('a', [0, 1, 2, 3, 4, 5])
SP.Python.exec('print(type(a))')
SP.Python.exec('print(a[1:-1])')
def a = SP.Python.get('a')
println(a.class.name)
println(a)


//OUPUT:
// <class 'java.util.ArrayList'>
// [1, 2, 3, 4]
// java.util.ArrayList
// [0, 1, 2, 3, 4, 5]
