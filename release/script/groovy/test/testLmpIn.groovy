package test

import com.jtool.code.UT
import com.jtool.lmp.LmpIn


/** 测试 lammps 输入文件相关 */
// 创建输出目录
UT.IO.mkdir('lmp/.temp');

// 创建 in 文件
lmpIn = LmpIn.INIT_MELT_NPT_Cu();

// 直接写入
lmpIn.write('lmp/.temp/in-init-1');

// 修改参数，groovy 中支持直接使用这种写法
lmpIn.vT = 3000;
lmpIn.vSeed = 123456;

// 写入修改参数的结果
lmpIn.write('lmp/.temp/in-init-2');


// 路径相关的修改会直接同步
println('修改路径变量之前的输入输出文件');
println(lmpIn.i().asList());
println(lmpIn.o().asList());

lmpIn.vInDataPath = 'lmp/.temp/data-in';
lmpIn.vOutRestartPath = 'lmp/.temp/restart-out';

println('修改路径变量之后的输入输出文件');
println(lmpIn.i().asList());
println(lmpIn.o().asList());

lmpIn.write('lmp/.temp/in-init-3');

println('执行写入后的输入文件');
println(lmpIn.i().asList());


// 对于有多个输入输出的，内置的类型支持自动同步实际输入输出的文件数目
lmpIn = LmpIn.RESTART_MELT_NPT_Cu();

println('修改路径变量之前的输入输出文件');
println(lmpIn.i().asList());
println(lmpIn.o().asList());

lmpIn.vBeginIdx = 10;
lmpIn.vEndIdx = 16;

println('修改路径变量之后的输入输出文件');
println(lmpIn.i().asList());
println(lmpIn.o().asList());

lmpIn.vOutRestartPath = 'lmp/.temp/restart-out';

println('修改路径变量之后的输出文件');
println(lmpIn.o().asList());

lmpIn.write('lmp/.temp/in-init-4');

// 现在支持移除所有的附加设置
lmpIn.clear();
println('移除所有附加设置之后的输入输出文件');
println(lmpIn.i().asList());
println(lmpIn.o().asList());

