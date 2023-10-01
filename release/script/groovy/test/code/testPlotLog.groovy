package test.code

import com.jtool.math.MathEX;
import com.jtool.math.vector.Vectors;
import com.jtool.plot.Plotters;


/** 测试直接在 java 上绘图 */
// 获取数据
x = Vectors.linspace(0.2, 4.0, 20);
x0 = x.opt().map {MathEX.Fast.sqrt(it)};
x1 = x;
x2 = x * x;
x3 = x * x * x;

// 绘制
plt1 = Plotters.get();
plt1.plot(x, x0).lineType('--').markerType('s').markerColor('r');
plt1.plot(x, x1).lineType('..').markerType('o').markerColor('g');
plt1.plot(x, x2).lineType('-.').markerType('^').markerColor('b');
plt1.plot(x, x3).lineType('-' ).markerType('d').markerColor('m');
plt1.xLabel('x').yLabel('y');
plt1.yRange(0.2, 4.0);
plt1.show('plot');

// 绘制对数坐标图
plt2 = Plotters.get();
plt2.semilogx(x, x0).lineType('--').markerType('s').markerColor('r');
plt2.semilogx(x, x1).lineType('..').markerType('o').markerColor('g');
plt2.semilogx(x, x2).lineType('-.').markerType('^').markerColor('b');
plt2.semilogx(x, x3).lineType('-' ).markerType('d').markerColor('m');
plt2.xLabel('x').yLabel('y');
plt2.yRange(0.2, 4.0);
plt2.show('semilogx');

plt3 = Plotters.get();
plt3.semilogy(x, x0).lineType('--').markerType('s').markerColor('r');
plt3.semilogy(x, x1).lineType('..').markerType('o').markerColor('g');
plt3.semilogy(x, x2).lineType('-.').markerType('^').markerColor('b');
plt3.semilogy(x, x3).lineType('-' ).markerType('d').markerColor('m');
plt3.xLabel('x').yLabel('y');
plt3.yRange(0.2, 4.0);
plt3.show('semilogy');

plt4 = Plotters.get();
plt4.loglog(x, x0).lineType('--').markerType('s').markerColor('r');
plt4.loglog(x, x1).lineType('..').markerType('o').markerColor('g');
plt4.loglog(x, x2).lineType('-.').markerType('^').markerColor('b');
plt4.loglog(x, x3).lineType('-' ).markerType('d').markerColor('m');
plt4.xLabel('x').yLabel('y');
plt4.yRange(0.2, 4.0);
plt4.show('loglog');
