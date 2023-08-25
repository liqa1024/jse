package test

import com.jtool.code.UT
import com.jtool.lmp.Dump
import com.jtool.lmp.Lmpdat
import com.jtool.rareevent.atom.ClusterSizeCalculator
import com.jtool.rareevent.atom.MultiTypeClusterSizeCalculator


/** 测试计算 BOOP，测试团簇计算器的效果 */


// 首先导入 Lmpdat
def dataG = Lmpdat.read('lmp/data/data-glass');
def dataC = Lmpdat.read('lmp/data/data-crystal');
def dataFFS = Dump.read('lmp/data/dump-ffs');

// 默认的团簇计算器
UT.Timer.tic();
def cal = new ClusterSizeCalculator(1.5, 12);
println("default glass: ${cal.lambdaOf(dataG)}");
println("default crystal: ${cal.lambdaOf(dataC)}");
println("default ffs: ${cal.lambdaOf(dataFFS.last())}");
UT.Timer.toc();

// 用于计算合金的团簇计算器
UT.Timer.tic();
def calMulti = new MultiTypeClusterSizeCalculator(1.5, 12);
println("multi glass: ${calMulti.lambdaOf(dataG)}");
println("multi crystal: ${calMulti.lambdaOf(dataC)}");
println("multi ffs: ${calMulti.lambdaOf(dataFFS.last())}");
UT.Timer.toc();

