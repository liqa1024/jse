package test
// import jTool classes
import com.guan.code.UT;
import com.guan.ssh.ServerSSH;

import groovy.json.JsonSlurper


/** 创建 ssh */
var SSH_INFO = (new JsonSlurper()).parse(UT.IO.toReader('.SECRET/SSH_INFO.json'));
ssh = ServerSSH.get(SSH_INFO.csrc.wd as String, SSH_INFO.csrc.username as String, SSH_INFO.csrc.hostname as String);

/** 上传整个项目文件夹 */
UT.Timer.tic();
ssh.putWorkingDir();
UT.Timer.toc('put working dir');

/** 上传和下载文件夹 */
UT.Timer.tic();
// 创建测试文件夹
UT.IO.mkdir('.temp/test1/test2/test3');
UT.IO.write('.temp/test1/test2/test3/1', '123456');
// 测试上传
ssh.putDir('.temp/test1');
UT.IO.rmdir('.temp/test1');
// 测试下载
ssh.getDir('.temp/test1');
UT.Timer.toc('put and get');


/** 关闭 ssh */
ssh.shutdown();
