package test

import com.jtool.code.UT
import com.jtool.system.SSH

// 测试一下远程删除文件夹失败的问题
def ssh = new SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'));

ssh.rmdir('.temp/jTool@6ydvWO2h');

ssh.shutdown();

