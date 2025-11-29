仅用于记录各个 nnap 版本下的格式以及相关核心改动。

## VERSION == 1

最初版本，java 实现基组和基组导数，torch 实现神经网络部分以及训练。

参考的标准势函数格式如下：

```json
{
    "version": 1,
    "units": "metal",
    "models": [{
        "symbol": "Cu",
        "ref_eng": -3.51,
        "norm_vec": "[...], normalize vector represented by a list",
        "basis": {
            "type": "spherical_chebyshev",
            "nmax": 5,
            "lmax": 6,
            "rcut": 6.0
        },
        "torch": "UEsDBAAACAgA...., base64 encoded torch model data"
    }, {
        "symbol": "Zr",
        "ref_eng": -8.0475,
        "norm_vec": "[...], normalize vector represented by a list",
        "basis": {
            "type": "spherical_chebyshev",
            "nmax": 5,
            "lmax": 6,
            "rcut": 6.0
        },
        "torch": "UEsDBAAACAgA...., base64 encoded torch model data"
    }]
}
```


## VERSION == 2

基本功能完善版本。

- 增加能量归一化支持
- 基组归一化现在不是简单缩放而是会归一化到标准正态分布
- 镜像基组 `Mirror` 支持
- 增加高阶角向序的支持


参考的标准势函数格式如下：

```json
{
    "version": 2,
    "units": "metal",
    "models": [{
        "symbol": "Cu",
        "ref_eng": -3.51,
        "norm_sigma": "[...], normalize sigma represented by a list",
        "norm_mu": "[...], normalize mu represented by a list",
        "norm_sigma_eng": 0.39135312682638995,
        "norm_mu_eng": -0.154824751875001,
        "basis": {
            "type": "spherical_chebyshev",
            "nmax": 5,
            "lmax": 6,
            "l3max": 0,
            "l3cross": true,
            "rcut": 6.0
        },
        "torch": "UEsDBAAACAgA...., base64 encoded torch model data"
    }, {
        "symbol": "Zr",
        "ref_eng": -8.0475,
        "norm_sigma": "[...], normalize sigma represented by a list",
        "norm_mu": "[...], normalize mu represented by a list",
        "norm_sigma_eng": 0.39135312682638995,
        "norm_mu_eng": -0.154824751875001,
        "basis": {
            "type": "spherical_chebyshev",
            "nmax": 5,
            "lmax": 6,
            "l3max": 0,
            "l3cross": true,
            "rcut": 6.0
        },
        "torch": "UEsDBAAACAgA...., base64 encoded torch model data"
    }]
}
```

对于镜像基组 `Mirror`，其参考格式为：

```json
{
    "version": 2,
    "units": "metal",
    "models": [{
        "symbol": "Fe",
        "some thing else...": "some thing else..."
    }, {
        "symbol": "Fe_a",
        "basis": {
            "type": "mirror",
            "mirror": 1
        }
    }]
}
```


## VERSION == 3

基组部分采用 C 编写来加速，标准势函数格式没有显著区别。


## VERSION == 4

- 增加 `single`, `none`, `full`, `exfull` 这些额外的种类项处理
- 增加纯径向基组 `chebyshev` 以及合并基组 `merge` 支持
- 神经网络部分不再使用 torch 而是手搓 C 代码来实现从而进一步加速

旧版的势函数文件会按照兼容模式运行，会以非常慢的速度运行。可以通过
`jsex.nnap.TrainerTorch.convert()` 函数来将其转换成新版本。

参考的标准势函数格式如下：

```json
{
    "version": 4,
    "units": "metal",
    "models": [{
        "symbol": "Cu",
        "ref_eng": -3.51,
        "norm_sigma": "[...], normalize sigma represented by a list",
        "norm_mu": "[...], normalize mu represented by a list",
        "norm_sigma_eng": 0.39135312682638995,
        "norm_mu_eng": -0.154824751875001,
        "basis": {
            "type": "merge",
            "basis": [{
                "type": "chebyshev",
                "nmax": 5,
                "rcut": 6.0,
                "wtype": "exfull"
            }, {
                "type": "spherical_chebyshev",
                "nmax": 4,
                "lmax": 6,
                "noradial": false,
                "l3max": 0,
                "l3cross": true,
                "rcut": 4.0,
                "wtype": "exfull"
            }]
        },
        "nn": {
            "type": "feed_forward",
            "input_dim": 123,
            "hidden_dims": [32, 32],
            "hidden_weights": "[...], 3-D list of nn weights, order: [layer][output][input]",
            "hidden_biases": "[...], 2-D list of nn biases, order: [layer][output]",
            "output_weight": "[...], 1-D list of nn output weight",
            "output_bias": -0.21909025311470032
        }
    }, {
        "symbol": "Zr",
        "ref_eng": -8.0475,
        "norm_sigma": "[...], normalize sigma represented by a list",
        "norm_mu": "[...], normalize mu represented by a list",
        "norm_sigma_eng": 0.39135312682638995,
        "norm_mu_eng": -0.154824751875001,
        "basis": {
            "type": "merge",
            "basis": [{
                "type": "chebyshev",
                "nmax": 5,
                "rcut": 6.0,
                "wtype": "exfull"
            }, {
                "type": "spherical_chebyshev",
                "nmax": 4,
                "lmax": 6,
                "noradial": false,
                "l3max": 0,
                "l3cross": true,
                "rcut": 4.0,
                "wtype": "exfull"
            }]
        },
        "nn": {
            "type": "feed_forward",
            "input_dim": 123,
            "hidden_dims": [32, 32],
            "hidden_weights": "[...], 3-D list of nn weights, order: [layer][output][input]",
            "hidden_biases": "[...], 2-D list of nn biases, order: [layer][output]",
            "output_weight": "[...], 1-D list of nn output weight",
            "output_bias": 0.5367032885551453
        }
    }]
}
```

### 后期 4 版本支持

- 纯 java 的 nnap 训练支持
- 反向传播计算力，从而大幅提高速度
- nnap 训练不再缓存
- nnap 使用 C++ 模板重写
- `fuse` 种类混合支持，从而基组训练支持

此时对于 `fuse` 种类格式参考：

```json
{
    "type": "spherical_chebyshev",
    "nmax": 4,
    "lmax": 6,
    "noradial": false,
    "l3max": 0,
    "l3cross": true,
    "rcut": 4.0,
    "wtype": "fuse",
    "fuse_weight": "[...], 2-D list of fuse weight, order: [fuse_k][type]"
}
```

## VERSION == 5

- 移除 `single` 种类项
- `l3max` 现在可以增加到 6
- `l4max` 支持且可以支持到 3
- nnap 继续训练支持
- adam 优化器支持以及分 batch 支持
- 能量归一化现在只存储一个值，保证一定不会不相同（这个主要导致旧版不兼容）


### 后期 5 版本支持

- 共享归一化系数的格式支持
- 共享基组支持
- 共享神经网络支持
- `cnlm` 二次混合支持
- `fuse_weight` 排序调整
- 增加 `exfuse` 种类项
- 移除 `noradial`, `l3cross`, `l4cross` 可调项

参考的标准势函数格式如下：

```json
{
    "version": 5,
    "units": "metal",
    "models": [{
        "symbol": "Cu",
        "ref_eng": -3.51,
        "norm_sigma": "[...], normalize sigma represented by a list",
        "norm_mu": "[...], normalize mu represented by a list",
        "norm_sigma_eng": 0.39135312682638995,
        "norm_mu_eng": -0.154824751875001,
        "basis": {
            "type": "merge",
            "basis": [{
                "type": "chebyshev",
                "nmax": 5,
                "rcut": 6.0,
                "wtype": "exfuse",
                "fuse_style": "limited",
                "fuse_size": 2,
                "fuse_weight": "[...], 2-D list of fuse weight, order: [type][fuse_k]"
            }, {
                "type": "spherical_chebyshev",
                "nmax": 4,
                "lmax": 6,
                "l3max": 0,
                "l4max": 0,
                "rcut": 4.0,
                "wtype": "exfuse",
                "fuse_style": "limited",
                "fuse_size": 2,
                "fuse_weight": "[...], 2-D list of fuse weight, order: [type][fuse_k]",
                "post_fuse": true,
                "post_fuse_size": 8,
                "post_fuse_scale": 0.25819888974716113,
                "post_fuse_weight": "[...], 1-D list of post fuse weight"
            }]
        },
        "nn": {
            "type": "feed_forward",
            "input_dim": 74,
            "hidden_dims": [48, 32],
            "hidden_weights": "[...], 3-D list of nn weights, order: [layer][output][input]",
            "hidden_biases": "[...], 2-D list of nn biases, order: [layer][output]",
            "output_weight": "[...], 1-D list of nn output weight",
            "output_bias": -0.21909025311470032
        }
    }, {
        "symbol": "Zr",
        "ref_eng": -8.0475,
        "norm_sigma": "[...], normalize sigma represented by a list",
        "norm_mu": "[...], normalize mu represented by a list",
        "basis": {
            "type": "share",
            "share": 1
        },
        "nn": {
            "type": "shared_feed_forward",
            "share": 1,
            "input_dim": 74,
            "shared_hidden_dims": [24, 16, 0],
            "hidden_weights": "[...], 3-D list of nn weights, order: [layer][output][input]",
            "hidden_biases": "[...], 2-D list of nn biases, order: [layer][output]",
            "output_weight": "[...], 1-D list of nn output weight",
            "output_bias": 0.5367032885551453
        }
    }]
}
```

## VERSION == 6 (计划中)

- 基组包含自身种类信息支持
- 训练代码直接砍掉不同种类不同基组的写法，现在只允许使用相同基组，并且总是共享归一化系数（同时也会总是共享 scale）
- `Mirror` 也改成单基组的写法
- 共享基组默认会共享归一化系数，从而简化训练时的共享归一化系数设置


