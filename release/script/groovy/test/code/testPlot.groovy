package test.code

import com.jtool.math.MathEX
import com.jtool.math.vector.Vectors;
import com.jtool.plot.Plotters;


/** 测试直接在 java 上绘图 */
// 获取数据
x = Vectors.linspace(-1.0, 0.1, 21);
sinx = x.opt().map {v -> MathEX.Fast.sin(v*MathEX.PI)};
cosx = x.opt().map {v -> MathEX.Fast.cos(v*MathEX.PI)};
nsinx= x.opt().map {v ->-MathEX.Fast.sin(v*MathEX.PI)};
theta =  Vectors.linspace(0.0, 0.1*MathEX.PI, 21);
xc = theta.opt().map {v -> MathEX.Fast.cos(v)};
yc = theta.opt().map {v -> MathEX.Fast.sin(v)};


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
// 现在自动设置 tick 和 axis 也可以
//plt.tick(0.2);
//plt.axis(-1.1, 1.1);

// 显示结果
fig = plt.show();

// 设置窗口尺寸和位置
fig.size(700, 700).location(500, 200);

// 保存图片（注意内部 panel 的 size 会和窗口设置尺寸后有一定出入，直接保存会和设置的尺寸关联，而等待一段后再保存则和实际显示的关联）
fig.save('.temp/testPlot.png');
