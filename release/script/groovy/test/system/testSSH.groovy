package test.system

import jse.code.UT;
import jse.system.SSH;


/** 创建 ssh */
SSH_INFO = UT.IO.json2map('.SECRET/SSH_INFO.json');

try (def ssh = new SSH(SSH_INFO)) {
    /** 上传和下载文件夹 */
    UT.Timer.tic();
    // 创建测试文件夹
    UT.IO.write('.temp/test1/test2/test3/1', '123456');
    // 测试上传
    ssh.putFiles(['.temp/test1/test2/test3/1']);
    UT.IO.rmdir('.temp/test1');
    // 测试下载
    ssh.getFiles(['.temp/test1/test2/test3/1']);
    UT.Timer.toc('put and get');
}
