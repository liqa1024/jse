%% load lmp script and set global variable
addjpath('lib/jtool-all.jar');

%% 使用 Generator 脚本获取多孔结构
import jtool.atom.*

N = 100;
meshSize = 0.20;
C = -0.20;
dt = 0.0004;
steps = 80000;

GEN = Generator(4);

tic;
fun3Porous = GEN.porousCahnHilliard(N, meshSize, C, dt, steps);
toc;

GEN.shutdown;


%% 绘制结构 3D
x = linspace(fun3Porous.x0, fun3Porous.dx*fun3Porous.Nx, fun3Porous.Nx+1);
y = linspace(fun3Porous.y0, fun3Porous.dy*fun3Porous.Ny, fun3Porous.Ny+1);
z = linspace(fun3Porous.z0, fun3Porous.dz*fun3Porous.Nz, fun3Porous.Nz+1);
[X, Y, Z] = meshgrid(x, y, z);

porous = zeros(fun3Porous.Nx+1, fun3Porous.Ny+1, fun3Porous.Nz+1);
for i = 1:fun3Porous.Nx
for j = 1:fun3Porous.Ny
for k = 1:fun3Porous.Nz
    porous(i,j,k) = fun3Porous.get(i-1, j-1, k-1);
end
end
end

% 边界面手动设定 0 绘制出想要的结构
for j = 1:fun3Porous.Ny
for k = 1:fun3Porous.Nz
    value = porous(1,j,k);
    if value > 0; porous(1,j,k) = 0; end
    if value < 0; porous(end,j,k) = value; end
end
end

for i = 1:fun3Porous.Nx
for j = 1:fun3Porous.Ny
    value = porous(i,j,1);
    if value > 0; porous(i,j,1) = 0; end
    if value < 0; porous(i,j,end) = value; end
end
end

for i = 1:fun3Porous.Nx
for k = 1:fun3Porous.Nz
    value = porous(i,1,k);
    if value > 0; porous(i,1,k) = 0; end
    if value < 0; porous(i,end,k) = value; end
end
end

%%
figure;
isosurface(X, Y, Z, porous, 0);
axis equal;


%% 绘制结构 2D
x = linspace(fun3Porous.x0, fun3Porous.dx, fun3Porous.Nx);
y = linspace(fun3Porous.y0, fun3Porous.dy, fun3Porous.Ny);

plane = zeros(fun3Porous.Nx, fun3Porous.Ny);
for i = 1:fun3Porous.Nx
for j = 1:fun3Porous.Ny
    plane(i,j) = fun3Porous.get(i-1, j-1, 10);
end
end

figure;
colormap parula
imagesc(x, y, plane);
colorbar


%% unload lmp script and clear global variable
clear;
rmjpath('lib/jtool-all.jar');
