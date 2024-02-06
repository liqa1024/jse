package example.system

import jse.system.SSH

// 替换成所需连接的远程服务器的 ip，用户名，以及密码
def sshInfo = [
    hostname: '127.0.0.1',
    username: 'admin',
    password: '123456'
]

try (def ssh = new SSH(sshInfo)) {
    ssh.system('echo 123456');
    ssh.system('hostname');
}

//OUTPUT:
// 123456
// lon12

