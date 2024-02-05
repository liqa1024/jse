package test.mpc

import jse.code.UT
import jse.lmp.Dump
import jse.lmp.Lmpdat
import jse.math.table.Tables

import static jse.code.CS.*

data = Dump.read('lmp/.ffs-in/dump-fs1-new').last();

UT.Timer.tic();
voronoi = data.getMPC().withCloseable {it.calVoronoi();}.setAreaThreshold(0.02).setLengthThreshold(0.02)
UT.Timer.toc('voronoi');

// 现在支持这样访问，不会有性能问题
table = Tables.zeros(voronoi.size());
table['i'] = 0..<data.atomNum();
table['coordination'] = (voronoi*.coordination());
UT.IO.table2csv(table, 'lmp/.temp/voronoi.csv');

largeNum = 0;
for (i in 0..<data.atomNum()) {
    def coordination = table['coordination'][i];
    if (coordination >= 16) {
        println("i = $i;\t coordination: $coordination;\t index: ${voronoi[i].index()};\t atomicVolume: ${voronoi[i].atomicVolume()}");
        ++largeNum;
    }
}
println("Total Number: $largeNum")

