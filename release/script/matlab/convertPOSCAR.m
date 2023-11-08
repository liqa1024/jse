%% load lmp script and set global variable
addjpath('lib/jtool-all.jar');

%% 定义需要处理的 POSCAR 路径
path = 'lmp/data/re_MgZn2.poscar';

%% 读取 POSCAR
import jtool.vasp.*

data = POSCAR.read(path);

%% 获取 box 和 direct
box = data.vaspBox;
direct = data.direct;

%% 转为 matlab 的矩阵
boxMat = box.data;
directMat = direct.data;

%% 由于已经是正交的，直接做一次 svd 来得到旋转矩阵 R
[U, S, V] = svd(boxMat);

%% 更新 box 和 direct 并处理计算误差
boxMat = U*S*U';
directMat = directMat * (V*U');

boxMat(abs(boxMat)<1e-10) = 0.0;
directMat(abs(directMat)<1e-10) = 0.0;

%% 注入到 java 对象中
box.fill(boxMat);
direct.fill(directMat);

%% 输出结果
data.write(path);

%% unload lmp script and clear global variable
clear;
rmjpath('lib/jtool-all.jar');
