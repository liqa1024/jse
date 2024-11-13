function addjpath(pathToAdd)
% javaaddpath 的包装实现，不会重复添加路径来避免意外的情况

% 检查类路径中是否已经存在指定路径
tDir = dir(fullfile(pathToAdd));
tDir = tDir(1);
if isfile(pathToAdd)
    absPath = fullfile(tDir.folder, tDir.name);
elseif isfolder(pathToAdd)
    absPath = tDir.folder;
else
    warning(['Invalid Path: ', pathToAdd]);
    return;
end
classpath = javaclasspath('-dynamic');
pathAdded = any(strcmp(classpath, absPath));

% 如果路径未添加，则调用 javaaddpath 添加路径
if ~pathAdded
    javaaddpath(absPath);
end

end

