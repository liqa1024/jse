package code.system

import jse.code.IO
import jse.system.SSH


/*
FILE IN `.SECRET/SSH_INFO.json` WILL LIKE:
{
    "hostname": "127.0.0.1",
    "username": "admin",
    "password": "123456"
}
*/

def ssh = new SSH(IO.json2map('.SECRET/SSH_INFO.json'))

ssh.system('echo 123456')
ssh.system('hostname')

ssh.shutdown() // optional

//OUTPUT:
// 123456
// lon12

