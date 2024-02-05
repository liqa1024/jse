%% load lmp script and set global variable
addjpath('lib/jse-all.jar');

%% 获取 ssh 并执行指令
import jse.system.*
import jse.compat.*

ssh = SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'));

ssh.system('echo 123');
ssh.system('ls');

ssh.shutdown();

%% unload lmp script and clear global variable
clear;
rmjpath('lib/jse-all.jar');
