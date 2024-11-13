function fig = plotAtomData(jAtomData, colors, sizes, extend)
%绘制 atomData
% extend 可选，x y z 三个方向负正的延申次数

fig = figure;

dataSTD = jAtomData.dataSTD;
scatter3(dataSTD(:, 3), dataSTD(:, 4), dataSTD(:, 5), sizes(dataSTD(:, 2)), colors(dataSTD(:, 2), :), "filled");

hold on
grid on
if nargin > 3
    box = jAtomData.box.data;
    for i = extend(1,1):extend(1,2)
    for j = extend(2,1):extend(2,2)
    for k = extend(3,1):extend(3,2)
    if ~(i == 0 && j == 0 && k == 0)
        scatter3(dataSTD(:, 3)+box(1)*i, dataSTD(:, 4)+box(2)*j, dataSTD(:, 5)+box(3)*k, sizes(dataSTD(:, 2)), colors(dataSTD(:, 2), :), "filled");
    end
    end
    end
    end
end

axis equal

end
