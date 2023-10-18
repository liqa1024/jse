%% load lmp script and set global variable
addjpath('lib/jtool-all.jar');

%% load data
import jtool.compat.*
table = UT.IO.csv2table('.temp/FFS3c-gap.csv');

%%
COLORS = [0.0,0.0,0.0;
          1.0,0.2,0.0;
          0.3,0.7,0.2;
          0.2,0.4,0.8;
          0.7,0.0,0.7;
          0.8,0.6,0.0;
          0.0,0.6,0.8];

%%
fig = figure;

lines = {};

lines{2} = semilogy(table.get('lambda').data, table.get('all'      ).data, '-', 'Color', COLORS(1,:), 'LineWidth', 1.3, 'Marker', 'o', 'MarkerFaceColor', COLORS(1,:));
hold on
lines{3} = semilogy(table.get('lambda').data, table.get('gap=100'  ).data, '-', 'Color', COLORS(3,:), 'LineWidth', 1.3, 'Marker', 's', 'MarkerFaceColor', COLORS(3,:));
lines{4} = semilogy(table.get('lambda').data, table.get('gap=1000' ).data, '-', 'Color', COLORS(4,:), 'LineWidth', 1.3, 'Marker', 's', 'MarkerFaceColor', COLORS(4,:));
lines{5} = semilogy(table.get('lambda').data, table.get('gap=10000').data, '-', 'Color', COLORS(5,:), 'LineWidth', 1.3, 'Marker', 's', 'MarkerFaceColor', COLORS(5,:));

lines{1} = semilogy(table.get('lambda').data, table.get('kRef'     ).data, '-', 'Color', COLORS(2,:), 'LineWidth', 1.5, 'Marker', '^', 'MarkerFaceColor', COLORS(2,:));


ylabel('Growth rate k[step^{-1}]'); xlabel('λ');
legend([lines{:}], {'reference (low noise)', 'all', 'gap=100', 'gap=1000', 'gap=10000'});
axis([20, 200, 1e-17, 1e-3]);
grid on;


%% unload lmp script and clear global variable
clear;
rmjpath('lib/jtool-all.jar');
