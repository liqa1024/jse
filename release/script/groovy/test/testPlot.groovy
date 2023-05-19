package test

import com.jtool.math.MathEX;
import com.jtool.plot.Plotters;


/** 测试直接在 java 上绘图 */
// 获取数据
x = MathEX.Func.sequence(-1.0, 0.1, 21);
sinx = MathEX.Vec.mapDo(x, v -> MathEX.Fast.sin(v*MathEX.PI));
cosx = MathEX.Vec.mapDo(x, v -> MathEX.Fast.cos(v*MathEX.PI));
nsinx= MathEX.Vec.mapDo(x, v ->-MathEX.Fast.sin(v*MathEX.PI));
theta = MathEX.Func.sequence(0.0, 0.1*MathEX.PI, 21);
xc = MathEX.Vec.mapDo(theta, v -> MathEX.Fast.cos(v));
yc = MathEX.Vec.mapDo(theta, v -> MathEX.Fast.sin(v));


// 获取 plotter
plt = Plotters.get();

// 绘制数据
line1 = plt.plot(x, sinx);
line2 = plt.plot(x, cosx);
line3 = plt.plot(x,nsinx);
line4 = plt.plot(xc, yc);

// 手动设置颜色和线型
line1.lineType('--').markerType('s').markerColor('r');
line2.lineType('..').markerType('o').markerColor('g');
line3.lineType('-.').markerType('^').markerColor('b');
line4.lineType('-' ).markerType('d').markerColor('m');

// 设置绘制范围，标题
plt.xLabel('x');
plt.yLabel('y');
plt.tick(0.2);
plt.axis(-1.1, 1.1);

// 显示结果
fig = plt.show();

// 设置窗口尺寸和位置
fig.size(700, 700).location(500, 200);

