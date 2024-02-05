package test.system

import jse.code.UT
import jse.iofile.IOFiles
import jse.plot.Plotters
import jse.system.SSH
import obj.GUT;


/** 测试新的更加易用的 SSH */
// 创建输出目录
UT.IO.mkdir('.temp');

// 创建 ssh
SSH_INFO = UT.IO.json2map('.SECRET/SSH_INFO.json');
ssh = new SSH(SSH_INFO); // 新的可以直接通过 Map 来构建

// 发送指令
ssh.system('echo 1 > 1.txt');
ssh.system('ls');

// 指令结果存入文本
ssh.system('ls', '.temp/ssh-ls');

// 提交任务式，没有指定线程数默认依旧会默认串行执行
ssh.submitSystem('echo "submitSystem begin"; sleep 1s; echo "submitSystem done"');
println('MARK');

// 复杂指令（在服务器上计算 gr，带有输入输出，后续会使用 ProgramExecutor 来实现，这里展示直接使用 SystemExecutor 来实现通用的）
// 由于需要使用 jse 本身来计算，如果没有初始化需要首先使用这个指令初始化一下 ssh 上的 jse 环境
//GUT.initjseEnv(ssh);

// 构造涉及的输入输出文件，包含计算 RDF 的脚本，输入的 data 文件和计算完成后输出的 csv 文件，key 可以随便取
ioFiles = (new IOFiles())
        .i('<self>', 'script/groovy/test/testRDF.groovy')
        .i('data', 'lmp/data/data-glass')
        .o('csv', 'lmp/.temp/gr.csv');
// 提交指令
ssh.system("./jse -f ${ioFiles.i('<self>')}", ioFiles); // 通过 key 来获取注册的文件名

// 关闭 ssh
ssh.shutdown();

// 此时已经自动下载完成输出文件，直接读取即可
gr = UT.IO.csv2table(ioFiles.o('csv'));
// 绘制 gr
plt = Plotters.get();
plt.plot(gr['x'], gr['f'], 'RDF');
plt.show();

