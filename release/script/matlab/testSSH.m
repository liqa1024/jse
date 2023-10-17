%% load lmp script and set global variable
addjpath('lib/jtool-all.jar');

%% 获取 ssh 并执行指令
import jtool.system.*
import jtool.compat.*

ssh = SSH(UT.IO.json2map('.SECRET/SSH_INFO.json'));

ssh.system('echo 123');
ssh.system('ls');

ssh.shutdown();

%% unload lmp script and clear global variable
clear;
rmjpath('lib/jtool-all.jar');
