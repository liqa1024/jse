%% load lmp script and set global variable
addjpath('lib/jse-all.jar');

%% 读取 data 转为 dump
import jse.lmp.*

data = Lmpdat.read('lmp/data/data-glass');
plotAtomData(data, [0.8,0.5,0.0; 0.0,0.8,0.3], [75, 86]);

%% unload lmp script and clear global variable
clear;
rmjpath('lib/jse-all.jar');
