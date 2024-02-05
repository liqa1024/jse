package test.code

import jse.code.UT

data0 = [
    "pi": 3.141,
    "happy": true,
    "name": "Niels",
    "nothing": null,
    "answer": [
        "everything": 42
    ],
    "list": [1, 0, 2],
    "object": [
        "currency": "USD",
        "value": 42.99
    ]
];
println(data0);

UT.IO.map2json(data0, '.temp/data.json');
data1 = UT.IO.json2map('.temp/data.json');
println(data1);

UT.IO.map2yaml(data0, '.temp/data.yaml');
data2 = UT.IO.yaml2map('.temp/data.yaml');
println(data2);

