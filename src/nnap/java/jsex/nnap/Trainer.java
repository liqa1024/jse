package jsex.nnap;

import jse.atom.IAtom;
import jse.atom.IAtomData;
import jse.atom.MonatomicParameterCalculator;
import jse.code.SP;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * jse 实现的 nnap 训练器，这里简单起见直接通过 python 训练。
 * 这里直接通过全局的 python 解释器执行
 * <p>
 * 通过重写方法来实现自定义的功能
 * @author liqa
 */
@ApiStatus.Experimental
public class Trainer {
    public static class Conf {
        /** 全局初始化脚本，默认为设置串行；经过测试并行训练并不会明显更快 */
        public static String INIT_SCRIPT = "torch.set_num_threads(1); torch.set_num_interop_threads(1)";
        
        /** 创建获取单粒子能量的模型的类，修改此值来实现自定义模型 */
        public static String SINGLE_MODEL_SCRIPT =
            "class __NNAP_TRAINER_SingleEnergyModel__(torch.nn.Module):\n" +
            "    def __init__(self, input_dim, hidden_dims, bn_layer):\n" +
            "        super().__init__()\n" +
            "        self.in_layer = torch.nn.Linear(input_dim, hidden_dims[0])\n" +
            "        self.in_bn_layer = torch.nn.BatchNorm1d(hidden_dims[0]) if bn_layer else torch.nn.Identity()\n" +
            "        self.hidden_layers = torch.nn.ModuleList([torch.nn.Linear(hidden_dims[i], hidden_dims[i]) for i in range(len(hidden_dims)-1)])\n" +
            "        self.hidden_bn_layers = torch.nn.ModuleList([(torch.nn.BatchNorm1d(hidden_dims[i]) if bn_layer else torch.nn.Identity()) for i in range(len(hidden_dims)-1)])\n" +
            "        self.out_layer = torch.nn.Linear(hidden_dims[-1], 1)\n" +
            "        self.active = torch.nn.SiLU()\n" +
            "    \n" +
            "    def forward(self, x):\n" +
            "        x = self.active(self.in_bn_layer(self.in_layer(x)))\n" +
            "        for hidden_layer, hidden_bn_layer in zip(self.hidden_layers, self.hidden_bn_layers):\n" +
            "            x = self.active(hidden_bn_layer(hidden_layer(x)))\n" +
            "        x = self.out_layer(x)\n" +
            "        return torch.reshape(x, x.shape[:-1])";
        
        /** 创建获取总能量的模型的类，修改此值来实现自定义模型；注意这里约定返回每原子能量，可以方便拟合以及 loss 函数的设计 */
        public static String TOTAL_MODEL_SCRIPT =
            "class __NNAP_TRAINER_TotalEnergyModel__(torch.nn.Module):\n" +
            "    def __init__(self, input_dims, hidden_dims, bn_layers, ntypes=1):\n" +
            "        super().__init__()\n" +
            "        self.input_dims = input_dims\n" +
            "        self.ntypes = ntypes\n" +
            "        self.sub_models = torch.nn.ModuleList([__NNAP_TRAINER_SingleEnergyModel__(input_dims[i], hidden_dims[i], bn_layers[i]) for i in range(ntypes)])\n" +
            "    \n" +
            "    def forward(self, x, atom_numbers):\n" +
            "        ylen = len(atom_numbers)\n" +
            "        y = torch.zeros(ylen, device=atom_numbers.device, dtype=atom_numbers.dtype)\n" +
            "        for i in range(self.ntypes):\n" +
            "            sumidx = x[i][:, -1].long()\n" +
            "            xout = self.sub_models[i](x[i][:, :-1])\n" +
            "            y.scatter_add_(0, sumidx, xout)\n" +
            "        y /= atom_numbers\n" +
            "        return y";
        
        /** 训练一步的脚本函数，修改此值来实现自定义训练算法；默认使用 LBFGS 训练 */
        public static String TRAIN_STEP_SCRIPT =
            "def __NNAP_TRAINER_train_step__():\n" +
            "   def closure():\n" +
            "       __NNAP_TRAINER_optimizer__.zero_grad()\n" +
            "       pred = __NNAP_TRAINER_model__(__NNAP_TRAINER_train_data_base__, __NNAP_TRAINER_train_data_atom_num__)\n" +
            "       loss = __NNAP_TRAINER_loss_fn__(pred, __NNAP_TRAINER_train_data_eng__)\n" +
            "       loss.backward()\n" +
            "       return loss\n" +
            "   loss = closure()\n" +
            "   __NNAP_TRAINER_optimizer__.step(closure)\n" +
            "   return loss.item()";
    }
    static {
        SP.Python.exec("import torch");
        SP.Python.exec(Conf.INIT_SCRIPT);
        SP.Python.exec(Conf.SINGLE_MODEL_SCRIPT);
        SP.Python.exec(Conf.TOTAL_MODEL_SCRIPT);
        SP.Python.exec(Conf.TRAIN_STEP_SCRIPT);
    }
    protected final static int[] DEFAULT_HIDDEN_DIMS = {20, 20};
    protected final static boolean DEFAULT_BN_LAYER = true;
    
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final Basis.IBasis[] mBasis;
    protected final DoubleList[] mTrainDataBase; // 按照种类排序，然后内部是可扩展的具体数据，最后一列增加对应的能量的索引；现在使用这种 DoubleList 展开的形式存
    protected final DoubleList mTrainDataEng = new DoubleList(64);
    protected final IntList mTrainDataAtomNum = new IntList(64);
    protected final Vector[] mNormVec;
    protected final RowMatrix[] mTrainDataBaseMat; // mTrainDataBase 的实际值，当然这个只是缓存结果
    protected boolean mModelInited = false;
    protected final Map<String, ?> mModelSetting;
    protected boolean mOptimizerInited = false;
    protected boolean mLossFnInited = false;
    
    public Trainer(String[] aSymbols, IVector aRefEngs, Basis.IBasis[] aBasis, Map<String, ?> aModelSetting) {
        if (aSymbols.length != aRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
        if (aSymbols.length != aBasis.length) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        mSymbols = aSymbols;
        mRefEngs = aRefEngs;
        mBasis = aBasis;
        mNormVec = new Vector[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            mNormVec[i] = Vector.zeros(mBasis[i].rowNumber()*mBasis[i].columnNumber());
        }
        mTrainDataBase = new DoubleList[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            mTrainDataBase[i] = new DoubleList(64);
        }
        mTrainDataBaseMat = new RowMatrix[mSymbols.length];
        mModelSetting = aModelSetting;
    }
    public Trainer(String[] aSymbols, IVector aRefEngs, Basis.IBasis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    public Trainer(String[] aSymbols, double[] aRefEngs, Basis.IBasis[] aBasis, Map<String, ?> aModelSetting) {this(aSymbols, Vectors.from(aRefEngs), aBasis, aModelSetting);}
    public Trainer(String[] aSymbols, double[] aRefEngs, Basis.IBasis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    private static Basis.IBasis[] repeatBasis_(Basis.IBasis aBasis, int aLen) {
        Basis.IBasis[] rOut = new Basis.IBasis[aLen];
        Arrays.fill(rOut, aBasis);
        return rOut;
    }
    
    /** 重写实现自定义模型创建 */
    protected void initModel() {
        @Nullable List<?> tHiddenDims = (List<?>)UT.Code.getWithDefault(mModelSetting, null, "hidden_dims", "nnarch");
        if (tHiddenDims == null) {
            tHiddenDims = NewCollections.from(mSymbols.length, i -> DEFAULT_HIDDEN_DIMS);
        }
        if (tHiddenDims.get(0) instanceof Integer) {
            final List<?> tSubDims = tHiddenDims;
            tHiddenDims = NewCollections.from(mSymbols.length, i -> tSubDims);
        }
        @Nullable Object tBnLayers = UT.Code.getWithDefault(mModelSetting, null, "bn_layers", "bn");
        if (tBnLayers == null) {
            tBnLayers = NewCollections.from(mSymbols.length, i -> DEFAULT_BN_LAYER);
        }
        if (tBnLayers instanceof Boolean) {
            final Boolean tSubBnLayer = (Boolean)tBnLayers;
            tBnLayers = NewCollections.from(mSymbols.length, i -> tSubBnLayer);
        }
        SP.Python.setValue("__NNAP_TRAINER_input_dims__", NewCollections.map(mBasis, base -> base.rowNumber()*base.columnNumber()));
        SP.Python.setValue("__NNAP_TRAINER_hidden_dims__", tHiddenDims);
        SP.Python.setValue("__NNAP_TRAINER_bn_layers__", tBnLayers);
        SP.Python.setValue("__NNAP_TRAINER_ntypes__", mSymbols.length);
        SP.Python.exec("__NNAP_TRAINER_model__ = __NNAP_TRAINER_TotalEnergyModel__(__NNAP_TRAINER_input_dims__, __NNAP_TRAINER_hidden_dims__, __NNAP_TRAINER_bn_layers__, ntypes=__NNAP_TRAINER_ntypes__).double()");
        SP.Python.removeValue("__NNAP_TRAINER_ntypes__");
        SP.Python.removeValue("__NNAP_TRAINER_bn_layers__");
        SP.Python.removeValue("__NNAP_TRAINER_hidden_dims__");
        SP.Python.removeValue("__NNAP_TRAINER_input_dims__");
    }
    /** 重写实现自定义优化器创建，默认采用 LBFGS */
    protected void initOptimizer() {
        SP.Python.exec("__NNAP_TRAINER_optimizer__ = torch.optim.LBFGS(__NNAP_TRAINER_model__.parameters(), history_size=100, max_iter=5, line_search_fn='strong_wolfe')");
    }
    /** 重写实现自定义损失函数创建，这里目前默认采用 SmoothL1Loss */
    protected void initLossFn() {
        SP.Python.exec("__NNAP_TRAINER_loss_fn__ = torch.nn.SmoothL1Loss()");
    }
    /** 重写实现自定义归一化方案 */
    protected void initNormVec() {
        for (int i = 0; i < mTrainDataBase.length; ++i) {
            mNormVec[i].fill(0.0);
            for (IVector tRow : mTrainDataBaseMat[i].rows()) {
                IVector tRowData = tRow.subVec(0, mNormVec[i].size());
                mNormVec[i].operation().operate2this(tRowData, (lhs, rhs) -> lhs + Math.abs(rhs));
            }
            mNormVec[i].div2this(mTrainDataBaseMat[i].rowNumber());
        }
    }
    /** 开始训练模型，这里直接训练给定的步数 */
    public void train(int aEpochs, boolean aPrintLog) {
        // 在这里初始化模型，可以避免构造函数中调用多态方法的一些歧义
        if (!mModelInited) {
            initModel();
            mModelInited = true;
        }
        // 同样在这里初始化优化器
        if (!mOptimizerInited) {
            initOptimizer();
            mOptimizerInited = true;
        }
        // 同样在这里初始化损失函数
        if (!mLossFnInited) {
            initLossFn();
            mLossFnInited = true;
        }
        if (aPrintLog) System.out.println("Init train data...");
        // 初始化矩阵数据
        for (int i = 0; i < mTrainDataBase.length; ++i) {
            final int tColNum = mBasis[i].rowNumber()*mBasis[i].columnNumber() + 1;
            final int tRowNum = mTrainDataBase[i].size() / tColNum;
            mTrainDataBaseMat[i] = new RowMatrix(tRowNum, tColNum, mTrainDataBase[i].internalData());
        }
        // 重新构建归一化向量
        initNormVec();
        // 构造 torch 数据，这里直接把数据放到 python 环境中变成一个 python 的全局变量
        SP.Python.setValue("__NNAP_TRAINER_norm_vec_j__", mNormVec);
        SP.Python.exec("__NNAP_TRAINER_norm_vec__ = [torch.tensor(sub_vec.asList(), dtype=torch.float64) for sub_vec in __NNAP_TRAINER_norm_vec_j__]");
        SP.Python.setValue("__NNAP_TRAINER_train_data_base_j__", mTrainDataBaseMat);
        SP.Python.exec("__NNAP_TRAINER_train_data_base__ = [torch.tensor(sub_base.asListRows(), dtype=torch.float64) for sub_base in __NNAP_TRAINER_train_data_base_j__]");
        Vector tTrainDataEng = mTrainDataEng.asVec();
        SP.Python.setValue("__NNAP_TRAINER_train_data_eng_j__", tTrainDataEng);
        SP.Python.exec("__NNAP_TRAINER_train_data_eng__ = torch.tensor(__NNAP_TRAINER_train_data_eng_j__.asList(), dtype=torch.float64)");
        IntVector tTrainDataAtomNum = mTrainDataAtomNum.asVec();
        SP.Python.setValue("__NNAP_TRAINER_train_data_atom_num_j__", tTrainDataAtomNum);
        SP.Python.exec("__NNAP_TRAINER_train_data_atom_num__ = torch.tensor(__NNAP_TRAINER_train_data_atom_num_j__.asList(), dtype=torch.float64)");
        //noinspection ConcatenationWithEmptyString
        SP.Python.exec("" +
        "for __NNAP_TRAINER_base__, __NNAP_TRAINER_sub_norm_vec__ in zip(__NNAP_TRAINER_train_data_base__, __NNAP_TRAINER_norm_vec__):\n" +
        "    for __NNAP_TRAINER_i__ in range(len(__NNAP_TRAINER_base__)):\n" +
        "        __NNAP_TRAINER_base__[__NNAP_TRAINER_i__, :-1] /= __NNAP_TRAINER_sub_norm_vec__\n" +
        "del __NNAP_TRAINER_base__, __NNAP_TRAINER_sub_norm_vec__, __NNAP_TRAINER_i__"
        );
        SP.Python.exec("__NNAP_TRAINER_train_data_eng__ /= __NNAP_TRAINER_train_data_atom_num__");
        SP.Python.removeValue("__NNAP_TRAINER_norm_vec_j__");
        SP.Python.removeValue("__NNAP_TRAINER_train_data_base_j__");
        SP.Python.removeValue("__NNAP_TRAINER_train_data_eng_j__");
        SP.Python.removeValue("__NNAP_TRAINER_train_data_atom_num_j__");
        // 开始训练
        if (aPrintLog) UT.Timer.progressBar("train", aEpochs);
        SP.Python.exec("__NNAP_TRAINER_model__.train()");
        for (int i = 0; i < aEpochs; ++i) {
            double tLoss = ((Number)SP.Python.invoke("__NNAP_TRAINER_train_step__")).doubleValue();
            if (aPrintLog) UT.Timer.progressBar(String.format("loss: %.4g", tLoss));
        }
        // 完事移除临时数据
        SP.Python.removeValue("__NNAP_TRAINER_norm_vec__");
        SP.Python.removeValue("__NNAP_TRAINER_train_data_base__");
        SP.Python.removeValue("__NNAP_TRAINER_train_data_eng__");
        SP.Python.removeValue("__NNAP_TRAINER_train_data_atom_num__");
    }
    
    @ApiStatus.Internal
    protected void calRefEngBaseAndAdd_(IAtomData aAtomData, double aEnergy, final DoubleList rEngData, DoubleList[] rBaseData) {
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                final int fI = i;
                IAtom tAtom = aAtomData.atom(fI);
                Basis.IBasis tBasis = mBasis[tAtom.type()-1];
                RowMatrix tBase = tBasis.eval(dxyzTypeDo -> {
                    tMPC.nl_().forEachNeighbor(fI, tBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        dxyzTypeDo.run(dx, dy, dz, tMPC.atomType_().get(idx));
                    });
                });
                rBaseData[tAtom.type()-1].addAll(tBase.asVecRow());
                rBaseData[tAtom.type()-1].add(rEngData.size());
            }
        }
        // 这里后添加能量，这样 rEngData.size() 对应正确的索引
        for (IAtom atom : aAtomData.atoms()) {
            aEnergy -= mRefEngs.get(atom.type()-1);
        }
        rEngData.add(aEnergy);
    }
    /** 增加一个训练集数据，目前只需要能量 */
    public void addTrainData(IAtomData aAtomData, double aEnergy) {
        // 这里也重新排序原子数据的 symbol 顺序
        NNAP.reorderSymbols_(AbstractCollections.from(mSymbols), aAtomData);
        // 添加数据
        calRefEngBaseAndAdd_(aAtomData, aEnergy, mTrainDataEng, mTrainDataBase);
        mTrainDataAtomNum.add(aAtomData.atomNumber());
    }
}
