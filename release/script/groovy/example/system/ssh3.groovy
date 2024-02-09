package example.system

import jse.code.UT
import jse.system.SSH

/*
FILE IN `.SECRET/SSH_INFO.json` WILL LIKE:
{
    "hostname": "127.0.0.1",
    "username": "admin",
    "password": "123456"
}
*/

try (def ssh = new SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'))) {
    ssh.system('echo 123456');
    ssh.system('hostname');
}

//OUTPUT:
// 123456
// lon12

