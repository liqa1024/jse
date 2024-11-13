package example.io

import static jse.code.UT.IO.*

def data0 = [
    'pi': 3.141,
    'happy': true,
    'name': 'Niels',
    'nothing': null,
    'answer': [
        'everything': 42
    ],
    'list': [1, 0, 2],
    'object': [
        'currency': 'USD',
        'value': 42.99
    ]
]
println('origin data:')
println(data0)


map2json(data0, '.temp/example/io/data.json')
println('json text:')
println(readAllText('.temp/example/io/data.json'))

def data1 = json2map('.temp/example/io/data.json')
println('json data:')
println(data1)


map2yaml(data0, '.temp/example/io/data.yaml')
println('yaml text:')
println(readAllText('.temp/example/io/data.yaml'))

def data2 = yaml2map('.temp/example/io/data.yaml')
println('yaml data:')
println(data2)


//OUTPUT:
// origin data:
// [pi:3.141, happy:true, name:Niels, nothing:null, answer:[everything:42], list:[1, 0, 2], object:[currency:USD, value:42.99]]
// json text:
// {"pi":3.141,"happy":true,"name":"Niels","nothing":null,"answer":{"everything":42},"list":[1,0,2],"object":{"currency":"USD","value":42.99}}
// json data:
// [pi:3.141, happy:true, name:Niels, nothing:null, answer:[everything:42], list:[1, 0, 2], object:[currency:USD, value:42.99]]
// yaml text:
// ---
// pi: 3.141
// happy: true
// name: "Niels"
// nothing: null
// answer:
//   everything: 42
// list:
// - 1
// - 0
// - 2
// object:
//   currency: "USD"
//   value: 42.99
//
// yaml data:
// [pi:3.141, happy:true, name:Niels, nothing:null, answer:[everything:42], list:[1, 0, 2], object:[currency:USD, value:42.99]]

