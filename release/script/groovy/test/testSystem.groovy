package test

import obj.Terminal
import com.jtool.code.UT
import com.jtool.system.WSL


/** 测试系统指令执行的接口 */
// 创建输出目录
UT.IO.mkdir('.temp');

// 获取执行器，可能对 wsl 中的 linux 相关指令会比较熟悉
wsl = new WSL(3);

println("直接执行");
wsl.system('ls');
wsl.system('sleep 1s; echo 1');

println("使用 submit 来提交并行执行，并行数为设置的 3");
wsl.submitSystem('sleep 1s; echo 1');
wsl.submitSystem('sleep 1s; echo 2');
wsl.submitSystem('sleep 1s; echo 3');
wsl.submitSystem('sleep 1s; echo 4');
wsl.submitSystem('sleep 1s; echo 5');
wsl.submitSystem('sleep 1s; echo 6');
wsl.submitSystem('sleep 1s; echo 7');

// 等待执行完成
wsl.waitUntilDone();

println("增加 str 从而获取输出作为 List 而不是退出代码");
out = wsl.system_str('ls');
println(out);

// 指定输出到文件而不是控制台
wsl.system('ls', '.temp/ls.txt');
wsl.submitSystem('ls; echo submit', '.temp/ls-submit.txt');


println("submit 形式也支持获取 list 输出");
task = wsl.submitSystem_str('sleep 1s; ls');
println(task.get());


// 支持使用 Terminal 这个类来直接像函数一样调用
terminal = new Terminal(wsl);

println("像成员一样调用时会获取输出");
out = terminal.ls;
println(out);

println("像函数一样调用时会直接输出到控制台");
terminal.echo(123456);

println("groovy 也支持这样调用函数");
terminal.echo 654321

println("成员调用时会替换下划线为空格");
println(terminal.echo_321);


// 最后记得关闭
wsl.shutdown();
