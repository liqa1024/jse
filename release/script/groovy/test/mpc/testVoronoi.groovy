package test.mpc

import jtool.code.UT
import jtool.lmp.Lmpdat
import jtool.math.table.Tables

import static jtool.code.CS.*

data = Lmpdat.read('lmp/data/data-glass');

UT.Timer.tic();
voronoi = data.getMPC().withCloseable {it.calVoronoi();}
UT.Timer.toc('voronoi');

// 现在支持这样访问，不会有性能问题
table = Tables.zeros(voronoi.size());
table['i'] = 0..<data.atomNum();
table['coordination'] = (voronoi*.coordination());
table['atomicVolume'] = (voronoi*.atomicVolume());
table['cavityRadius'] = (voronoi*.cavityRadius());
for (i in 3..9) table["index.$i"] = 0;
table.asMatrix()[ALL][4..10] = (voronoi*.index());

for (i in 0..<data.atomNum()) {
    def vdata = voronoi[i];
    table['i'][i] = i;
    table['coordination'][i] = vdata.coordination();
    table['atomicVolume'][i] = vdata.atomicVolume();
    table['cavityRadius'][i] = vdata.cavityRadius();
    def index = vdata.index();
    for (j in 3..9) table["index.$j"][i] = index[j-1];
};
UT.IO.table2csv(table, 'lmp/.temp/voronoi.csv');

// 读取 ovito 的表并对比
tabelOvito = UT.IO.csv2table('lmp/.temp/voronoi-ovito.csv');

diffNum = 0;
for (i in 0..<data.atomNum()) {
    def coordinationErr = table['coordination'][i] - tabelOvito['coordination'][i];
    def index3Err = table['index.3'][i]-tabelOvito['index.3'][i];
    def index4Err = table['index.4'][i]-tabelOvito['index.4'][i];
    def index5Err = table['index.5'][i]-tabelOvito['index.5'][i];
    def index6Err = table['index.6'][i]-tabelOvito['index.6'][i];
    def index7Err = table['index.7'][i]-tabelOvito['index.7'][i];
    if (coordinationErr!=0 || index3Err!=0 || index4Err!=0 || index5Err!=0 || index6Err!=0 || index7Err!=0) {
        println("i = $i;\t coordinationErr: $coordinationErr;\t indexErr: [$index3Err, $index4Err, $index5Err, $index6Err, $index7Err];\t atomicVolumeErr: ${table['atomicVolume'][i]-tabelOvito['atomicVolume'][i]}");
        ++diffNum;
    }
}
println("Total Diff Number: $diffNum")

