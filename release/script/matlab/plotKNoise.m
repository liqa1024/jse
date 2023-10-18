%% load lmp script and set global variable
addjpath('lib/jtool-all.jar');

%% load data
import jtool.compat.*
table = UT.IO.csv2table('.temp/FFS3c-noise.csv');

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

lines{1} = semilogy(table.get('lambda').data, table.get('kNoise').data, '-', 'Color', COLORS(1,:), 'LineWidth', 1.3, 'Marker', 'o', 'MarkerFaceColor', COLORS(1,:));
hold on
lines{2} = semilogy(table.get('lambda').data, table.get('kRef'  ).data, '-', 'Color', COLORS(2,:), 'LineWidth', 1.3, 'Marker', '^', 'MarkerFaceColor', COLORS(2,:));


ylabel('Growth rate k[step^{-1}]'); xlabel('λ');
legend([lines{:}], {'high noise', 'low noise (reference)'});
axis([20, 200, 1e-17, 1e-3]);
grid on;


%% unload lmp script and clear global variable
clear;
rmjpath('lib/jtool-all.jar');
