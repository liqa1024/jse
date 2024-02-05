package test.code

import static jse.code.UT.Math.*
import static jse.code.UT.Plot.*


/** 测试直接在 java 上绘图 */
// 获取数据
x = linspace(-1.0, 1.0, 21);
sinx = sin(x*pi);
cosx = cos(x*pi);
nsinx=-sin(x*pi);
theta = linspace(0.0, 2.0*pi, 21);
xc = cos(theta);
yc = sin(theta);


// 绘制数据
line1 = plot(x, sinx);
line2 = plot(x, cosx);
line3 = plot(x,nsinx);
line4 = plot(xc, yc);

// 手动设置颜色和线型
line1.lineType('--').lineWidth(3.0).markerType('s').markerColor('r');
line2.lineType('..').lineWidth(3.0).markerType('o').markerColor('g');
line3.lineType('-.').lineWidth(3.0).markerType('^').markerColor('b');
line4.lineType('-' ).lineWidth(3.0).markerType('d').markerColor('m');

// 设置绘制范围，标题
xLabel('x');
yLabel('y');
title('Title of Test Plot');
// 现在自动设置 tick 和 axis 也可以
//tick(0.2);
//axis(-1.1, 1.1);

// 保存图片
save('.temp/testPlot.png', 600, 600);
