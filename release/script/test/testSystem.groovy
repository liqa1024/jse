package test

import com.guan.code.UT
import com.guan.system.WSL


/** 测试系统指令执行的接口 */
// 创建输出目录
UT.IO.mkdir('.temp');

// 获取执行器，wsl 中的 linux 相关指令会比较好理解
wsl = new WSL(3);

// 直接执行
wsl.system('ls');
wsl.system('sleep 1s; echo 1');

// 使用 submit 来提交并行执行，并行数为设置的 3
wsl.submitSystem('sleep 1s; echo 1');
wsl.submitSystem('sleep 1s; echo 2');
wsl.submitSystem('sleep 1s; echo 3');
wsl.submitSystem('sleep 1s; echo 4');
wsl.submitSystem('sleep 1s; echo 5');
wsl.submitSystem('sleep 1s; echo 6');
wsl.submitSystem('sleep 1s; echo 7');

// 等待执行完成
wsl.waitUntilDone();

// 增加 str 从而获取输出作为 List 而不是退出代码
println(wsl.system_str('ls'));

// 指定输出到文件而不是控制台
wsl.system('ls', '.temp/ls.txt');
wsl.submitSystem('ls; echo submit', '.temp/ls-submit.txt');

// 最后记得关闭
wsl.shutdown();
wsl.awaitTermination();
