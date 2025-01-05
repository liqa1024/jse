package jsex.nnap;

import jep.python.PyObject;
import jse.atom.IAtomData;
import jse.atom.AtomicParameterCalculator;
import jse.cache.MatrixCache;
import jse.code.SP;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.io.ISavable;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jse.parallel.IAutoShutdown;
import jsex.nnap.basis.IBasis;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.*;
import java.util.function.IntUnaryOperator;

/**
 * jse 实现的 nnap 训练器，这里简单起见直接通过 python 训练。
 * 这里直接通过全局的 python 解释器执行
 * <p>
 * 通过重写方法来实现自定义的功能
 * @author liqa
 */
@ApiStatus.Experimental
public class Trainer implements IAutoShutdown, ISavable {
    
    protected final static int VERSION = 1;
    protected final static String DEFAULT_UNITS = "metal";
    protected final static int[] DEFAULT_HIDDEN_DIMS = {20, 20};
    protected final static boolean DEFAULT_BN_LAYER = true;
    /** 全局记录 python 中的变量名称 */
    protected final static String
          CLASS_SINGLE_MODEL    = "__NNAP_TRAINER_SingleEnergyModel__"
        , CLASS_TOTAL_MODEL     = "__NNAP_TRAINER_TotalEnergyModel__"
        , FN_TRAIN_STEP         = "__NNAP_TRAINER_train_step__"
        , FN_TEST_LOSS          = "__NNAP_TRAINER_test_loss__"
        , FN_CAL_MAE            = "__NNAP_TRAINER_cal_mae__"
        , FN_MODEL2BYTES        = "__NNAP_TRAINER_model2bytes__"
        , VAL_MODEL             = "__NNAP_TRAINER_model__"
        , VAL_MODEL_STATE_DICT  = "__NNAP_TRAINER_model_state_dict__"
        , VAL_OPTIMIZER         = "__NNAP_TRAINER_optimizer__"
        , VAL_LOSS_FN           = "__NNAP_TRAINER_loss_fn__"
        , VAL_LOSS_FN_ENG       = "__NNAP_TRAINER_loss_fn_eng__"
        , VAL_TRAIN_BASE        = "__NNAP_TRAINER_train_base__"
        , VAL_TRAIN_BASE_J      = "__NNAP_TRAINER_train_base_J__"
        , VAL_TRAIN_ATOM_NUM    = "__NNAP_TRAINER_train_atom_num__"
        , VAL_TRAIN_ATOM_NUM_J  = "__NNAP_TRAINER_train_atom_num_J__"
        , VAL_TRAIN_ENG         = "__NNAP_TRAINER_train_eng__"
        , VAL_TRAIN_ENG_J       = "__NNAP_TRAINER_train_eng_J__"
        , VAL_NORM_VEC          = "__NNAP_TRAINER_norm_vec__"
        , VAL_NORM_VEC_J        = "__NNAP_TRAINER_norm_vec_J__"
        , VAL_TEST_BASE         = "__NNAP_TRAINER_test_base__"
        , VAL_TEST_BASE_J       = "__NNAP_TRAINER_test_base_J__"
        , VAL_TEST_ATOM_NUM     = "__NNAP_TRAINER_test_atom_num__"
        , VAL_TEST_ATOM_NUM_J   = "__NNAP_TRAINER_test_atom_num_J__"
        , VAL_TEST_ENG          = "__NNAP_TRAINER_test_eng__"
        , VAL_TEST_ENG_J        = "__NNAP_TRAINER_test_eng_J__"
        , VAL_NTYPES            = "__NNAP_TRAINER_ntypes__"
        , VAL_BN_LAYERS         = "__NNAP_TRAINER_bn_layers__"
        , VAL_HIDDEN_DIMS       = "__NNAP_TRAINER_hidden_dims__"
        , VAL_INPUT_DIMS        = "__NNAP_TRAINER_input_dims__"
        , VAL_SUB_NORM_VEC      = "__NNAP_TRAINER_sub_norm_vec__"
        , VAL_SUB_BASE          = "__NNAP_TRAINER_sub_base__"
        , VAL_I                 = "__NNAP_TRAINER_i__"
        ;
    
    public static class Conf {
        /** 全局初始化脚本，默认为设置串行；经过测试并行训练并不会明显更快 */
        public static String INIT_SCRIPT =
            "try:\n" +
            "    torch.set_num_threads(1)\n" +
            "    torch.set_num_interop_threads(1)\n" +
            "except RuntimeError:\n" +
            "    pass";
        
        /** 创建获取单粒子能量的模型的类，修改此值来实现自定义模型 */
        public static String SINGLE_MODEL_SCRIPT =
            "class "+CLASS_SINGLE_MODEL+"(torch.nn.Module):\n" +
            "    def __init__(self, input_dim, hidden_dims, bn_layer):\n" +
            "        super().__init__()\n" +
            "        self.in_layer = torch.nn.Linear(input_dim, hidden_dims[0])\n" +
            "        self.in_bn_layer = torch.nn.BatchNorm1d(hidden_dims[0]) if bn_layer else torch.nn.Identity()\n" +
            "        self.hidden_layers = torch.nn.ModuleList([torch.nn.Linear(hidden_dims[i], hidden_dims[i+1]) for i in range(len(hidden_dims)-1)])\n" +
            "        self.hidden_bn_layers = torch.nn.ModuleList([(torch.nn.BatchNorm1d(hidden_dims[i+1]) if bn_layer else torch.nn.Identity()) for i in range(len(hidden_dims)-1)])\n" +
            "        self.out_layer = torch.nn.Linear(hidden_dims[-1], 1)\n" +
            "        self.active = torch.nn.SiLU()\n" +
            "    \n" +
            "    def forward(self, x):\n" +
            "        x = self.active(self.in_bn_layer(self.in_layer(x)))\n" +
            "        for hidden_layer, hidden_bn_layer in zip(self.hidden_layers, self.hidden_bn_layers):\n" +
            "            x = self.active(hidden_bn_layer(hidden_layer(x)))\n" +
            "        x = self.out_layer(x)\n" +
            "        return torch.reshape(x, x.shape[:-1])";
        
        /** 创建获取总能量的模型的类，修改此值来实现自定义模型；现在统一返回没平均的总能量 */
        public static String TOTAL_MODEL_SCRIPT =
            "class "+CLASS_TOTAL_MODEL+"(torch.nn.Module):\n" +
            "    def __init__(self, input_dims, hidden_dims, bn_layers, ntypes=1):\n" +
            "        super().__init__()\n" +
            "        self.input_dims = input_dims\n" +
            "        self.ntypes = ntypes\n" +
            "        self.sub_models = torch.nn.ModuleList(["+CLASS_SINGLE_MODEL+"(input_dims[i], hidden_dims[i], bn_layers[i]) for i in range(ntypes)])\n" +
            "    \n" +
            "    def forward(self, x, atom_nums):\n" +
            "        ylen = len(atom_nums)\n" +
            "        y = torch.zeros(ylen, device=atom_nums.device, dtype=atom_nums.dtype)\n" +
            "        for i in range(self.ntypes):\n" +
            "            sumidx = x[i][:, -1].long()\n" +
            "            xout = self.sub_models[i](x[i][:, :-1])\n" +
            "            y.scatter_add_(0, sumidx, xout)\n" +
            "        return y";
        
        /** 总 loss 函数，修改此值实现自定义 loss 函数，主要用于控制各种 loss 之间的比例 */
        public static String LOSS_FN_SCRIPT =
            "def "+VAL_LOSS_FN+"(pred_engs, target_engs):\n" +
            "    return "+VAL_LOSS_FN_ENG+"(pred_engs, target_engs)";
        
        /** 用于计算能量的 loss 函数，修改此值实现自定义 loss 函数；目前默认采用 SmoothL1Loss */
        public static String LOSS_FN_ENG_SCRIPT = VAL_LOSS_FN_ENG+"=torch.nn.SmoothL1Loss()";
        
        /** 训练一步的脚本函数，修改此值来实现自定义训练算法；默认使用 LBFGS 训练 */
        public static String TRAIN_STEP_SCRIPT =
            "def "+FN_TRAIN_STEP+"():\n" +
            "    "+VAL_MODEL+".train()\n"+
            "    def closure():\n" +
            "        "+VAL_OPTIMIZER+".zero_grad()\n" +
            "        pred = "+VAL_MODEL+"("+VAL_TRAIN_BASE+", "+VAL_TRAIN_ATOM_NUM+")\n" +
            "        pred /= "+VAL_TRAIN_ATOM_NUM+"\n" +
            "        loss = "+VAL_LOSS_FN+"(pred, "+VAL_TRAIN_ENG+")\n" +
            "        loss.backward()\n" +
            "        return loss\n" +
            "    loss = closure()\n" +
            "    "+VAL_OPTIMIZER+".step(closure)\n" +
            "    return loss.item()";
        
        /** 获取测试集误差的脚本函数，一般情况不需要重写 */
        public static String TEST_LOSS_SCRIPT =
            "def "+FN_TEST_LOSS+"():\n" +
            "    "+VAL_MODEL+".eval()\n"+
            "    with torch.no_grad():\n" +
            "        pred = "+VAL_MODEL+"("+VAL_TEST_BASE+", "+VAL_TEST_ATOM_NUM+")\n" +
            "        pred /= "+VAL_TEST_ATOM_NUM+"\n" +
            "        loss = "+VAL_LOSS_FN+"(pred, "+VAL_TEST_ENG+")\n" +
            "    return loss.item()";
        
        /** 将模型转为字节的脚本，一般情况不需要重写 */
        public static String MODEL2BYTES_SCRIPT =
            "def "+FN_MODEL2BYTES+"(i):\n" +
            "    sub_model = "+VAL_MODEL+".sub_models[i]\n" +
            "    sub_model.eval()\n" +
            "    input_ones = torch.ones(1, "+VAL_MODEL+".input_dims[i], dtype=torch.float64)\n" +
            "    traced_script_module = torch.jit.trace(sub_model, input_ones)\n" +
            "\n" +
            "    buffer = io.BytesIO()\n" +
            "    torch.jit.save(traced_script_module, buffer)\n" +
            "    buffer.seek(0)\n" +
            "    return buffer.read()";
    }
    static {
        SP.Python.exec("import torch");
        SP.Python.exec("import copy");
        SP.Python.exec("import io");
        SP.Python.exec(Conf.INIT_SCRIPT);
        SP.Python.exec(Conf.SINGLE_MODEL_SCRIPT);
        SP.Python.exec(Conf.TOTAL_MODEL_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_ENG_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_SCRIPT);
        SP.Python.exec(Conf.TRAIN_STEP_SCRIPT);
        SP.Python.exec(Conf.TEST_LOSS_SCRIPT);
        //noinspection ConcatenationWithEmptyString
        SP.Python.exec("" +
        "def "+FN_CAL_MAE+"(x, atom_nums, y):\n" +
        "    "+VAL_MODEL+".eval()\n"+
        "    with torch.no_grad():\n" +
        "        pred = "+VAL_MODEL+"(x, atom_nums)\n" +
        "        pred /= atom_nums\n" +
        "        return (y - pred).abs().mean().item()"
        );
        SP.Python.exec(Conf.MODEL2BYTES_SCRIPT);
    }
    
    private boolean mDead = false;
    public boolean isShutdown() {return mDead;}
    @Override public void shutdown() {
        if (!mDead) {
            mDead = true;
            if (mModelInited) SP.Python.removeValue(VAL_MODEL);
            if (mOptimizerInited) SP.Python.removeValue(VAL_OPTIMIZER);
        }
    }
    
    protected String mUnits = DEFAULT_UNITS;
    public Trainer setUnits(String aUnits) {mUnits = aUnits; return this;}
    
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final IBasis[] mBasis;
    protected final DoubleList[] mTrainBase; // 按照种类排序，然后内部是可扩展的具体数据，最后一列增加对应的能量的索引；现在使用这种 DoubleList 展开的形式存
    protected final RowMatrix[] mTrainBaseMat; // mTrainDataBase 的实际值，这个只是缓存结果
    protected final DoubleList mTrainEng = new DoubleList(64);
    protected final IntList mTrainAtomNum = new IntList(64);
    protected final Vector[] mNormVec;
    protected final DoubleList[] mTestBase;
    protected final RowMatrix[] mTestBaseMat;
    protected final DoubleList mTestEng = new DoubleList(64);
    protected final IntList mTestAtomNum = new IntList(64);
    protected boolean mHasTest = false;
    protected boolean mModelInited = false;
    protected final Map<String, ?> mModelSetting;
    protected boolean mOptimizerInited = false;
    protected final DoubleList mTrainLoss = new DoubleList(64);
    protected final DoubleList mTestLoss = new DoubleList(64);
    
    public Trainer(String[] aSymbols, IVector aRefEngs, IBasis[] aBasis, Map<String, ?> aModelSetting) {
        if (aSymbols.length != aRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
        if (aSymbols.length != aBasis.length) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        mSymbols = aSymbols;
        mRefEngs = aRefEngs;
        mBasis = aBasis;
        mNormVec = new Vector[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            mNormVec[i] = Vector.zeros(mBasis[i].rowNumber()*mBasis[i].columnNumber());
        }
        mTrainBase = new DoubleList[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            mTrainBase[i] = new DoubleList(64);
        }
        mTrainBaseMat = new RowMatrix[mSymbols.length];
        mTestBase = new DoubleList[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            mTestBase[i] = new DoubleList(64);
        }
        mTestBaseMat = new RowMatrix[mSymbols.length];
        mModelSetting = aModelSetting;
    }
    public Trainer(String[] aSymbols, IVector aRefEngs, IBasis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    public Trainer(String[] aSymbols, double[] aRefEngs, IBasis[] aBasis, Map<String, ?> aModelSetting) {this(aSymbols, Vectors.from(aRefEngs), aBasis, aModelSetting);}
    public Trainer(String[] aSymbols, double[] aRefEngs, IBasis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    private static IBasis[] repeatBasis_(IBasis aBasis, int aLen) {
        IBasis[] rOut = new IBasis[aLen];
        Arrays.fill(rOut, aBasis);
        return rOut;
    }
    
    public int atomTypeNumber() {return mSymbols.length;}
    public PyObject model(int aType) {return SP.Python.getAs(PyObject.class, VAL_MODEL+".sub_models["+(aType-1)+"]");}
    @SuppressWarnings("unchecked")
    public @Unmodifiable List<PyObject> models() {return (List<PyObject>)SP.Python.getAs(List.class, VAL_MODEL+".sub_models");}
    public IBasis basis(int aType) {return mBasis[aType-1];}
    public @Unmodifiable List<IBasis> basis() {return AbstractCollections.from(mBasis);}
    public String symbol(int aType) {return mSymbols[aType-1];}
    public @Unmodifiable List<String> symbols() {return AbstractCollections.from(mSymbols);}
    public String units() {return mUnits;}
    
    public IntUnaryOperator typeMap(IAtomData aAtomData) {return IAtomData.typeMap_(symbols(), aAtomData);}
    public boolean sameOrder(Collection<? extends CharSequence> aSymbolsIn) {return IAtomData.sameSymbolOrder_(symbols(), aSymbolsIn);}
    public int typeOf(String aSymbol) {return IAtomData.typeOf_(symbols(), aSymbol);}
    
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
        @Nullable Object tBnLayers = UT.Code.getWithDefault(mModelSetting, null, "bn_layers", "bn_layer", "bn");
        if (tBnLayers == null) {
            tBnLayers = NewCollections.from(mSymbols.length, i -> DEFAULT_BN_LAYER);
        }
        if (tBnLayers instanceof Boolean) {
            final Boolean tSubBnLayer = (Boolean)tBnLayers;
            tBnLayers = NewCollections.from(mSymbols.length, i -> tSubBnLayer);
        }
        SP.Python.setValue(VAL_INPUT_DIMS, NewCollections.map(mBasis, base -> base.rowNumber()*base.columnNumber()));
        SP.Python.setValue(VAL_HIDDEN_DIMS, tHiddenDims);
        SP.Python.setValue(VAL_BN_LAYERS, tBnLayers);
        SP.Python.setValue(VAL_NTYPES, mSymbols.length);
        try {
            SP.Python.exec(VAL_MODEL+" = "+CLASS_TOTAL_MODEL+"("+VAL_INPUT_DIMS+", "+VAL_HIDDEN_DIMS+", "+VAL_BN_LAYERS+", ntypes="+VAL_NTYPES+").double()");
        } finally {
            SP.Python.removeValue(VAL_NTYPES);
            SP.Python.removeValue(VAL_BN_LAYERS);
            SP.Python.removeValue(VAL_HIDDEN_DIMS);
            SP.Python.removeValue(VAL_INPUT_DIMS);
        }
    }
    /** 重写实现自定义优化器创建，默认采用 LBFGS */
    protected void initOptimizer() {
        SP.Python.exec(VAL_OPTIMIZER+" = torch.optim.LBFGS("+VAL_MODEL+".parameters(), history_size=100, max_iter=5, line_search_fn='strong_wolfe')");
    }
    /** 重写实现自定义归一化方案 */
    protected void initNormVec() {
        for (int i = 0; i < mTrainBase.length; ++i) {
            mNormVec[i].fill(0.0);
            for (IVector tRow : mTrainBaseMat[i].rows()) {
                IVector tRowData = tRow.subVec(0, mNormVec[i].size());
                mNormVec[i].operation().operate2this(tRowData, (lhs, rhs) -> lhs + Math.abs(rhs));
            }
            mNormVec[i].div2this(mTrainBaseMat[i].rowNumber());
        }
    }
    /** 开始训练模型，这里直接训练给定的步数 */
    public void train(int aEpochs, boolean aEarlyStop, boolean aPrintLog) {
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
        if (aPrintLog) System.out.println("Init train data...");
        // 初始化矩阵数据
        for (int i = 0; i < mTrainBase.length; ++i) {
            final int tColNum = mBasis[i].rowNumber()*mBasis[i].columnNumber() + 1;
            final int tRowNum = mTrainBase[i].size() / tColNum;
            mTrainBaseMat[i] = new RowMatrix(tRowNum, tColNum, mTrainBase[i].internalData());
        }
        if (mHasTest) for (int i = 0; i < mTestBase.length; ++i) {
            final int tColNum = mBasis[i].rowNumber()*mBasis[i].columnNumber() + 1;
            final int tRowNum = mTestBase[i].size() / tColNum;
            mTestBaseMat[i] = new RowMatrix(tRowNum, tColNum, mTestBase[i].internalData());
        }
        // 重新构建归一化向量
        initNormVec();
        // 构造 torch 数据，这里直接把数据放到 python 环境中变成一个 python 的全局变量
        SP.Python.setValue(VAL_NORM_VEC_J, mNormVec);
        SP.Python.setValue(VAL_TRAIN_BASE_J, mTrainBaseMat);
        Vector tTrainEng = mTrainEng.asVec();
        SP.Python.setValue(VAL_TRAIN_ENG_J, tTrainEng);
        IntVector tTrainAtomNum = mTrainAtomNum.asVec();
        SP.Python.setValue(VAL_TRAIN_ATOM_NUM_J, tTrainAtomNum);
        if (mHasTest) {
            SP.Python.setValue(VAL_TEST_BASE_J, mTestBaseMat);
            Vector tTestEng = mTestEng.asVec();
            SP.Python.setValue(VAL_TEST_ENG_J, tTestEng);
            IntVector tTestAtomNum = mTestAtomNum.asVec();
            SP.Python.setValue(VAL_TEST_ATOM_NUM_J, tTestAtomNum);
        }
        try {
            SP.Python.exec(VAL_NORM_VEC+" = [torch.tensor(sub_vec.asList(), dtype=torch.float64) for sub_vec in "+VAL_NORM_VEC_J+"]");
            SP.Python.exec(VAL_TRAIN_BASE+" = [torch.tensor(sub_base.asListRows(), dtype=torch.float64) for sub_base in "+VAL_TRAIN_BASE_J+"]");
            SP.Python.exec(VAL_TRAIN_ENG+" = torch.tensor("+VAL_TRAIN_ENG_J+".asList(), dtype=torch.float64)");
            SP.Python.exec(VAL_TRAIN_ATOM_NUM+" = torch.tensor("+VAL_TRAIN_ATOM_NUM_J+".asList(), dtype=torch.float64)");
            //noinspection ConcatenationWithEmptyString
            SP.Python.exec("" +
            "for "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC+" in zip("+VAL_TRAIN_BASE+", "+VAL_NORM_VEC+"):\n" +
            "    for "+VAL_I+" in range(len("+VAL_SUB_BASE+")):\n" +
            "        "+VAL_SUB_BASE+"["+VAL_I+", :-1] /= "+VAL_SUB_NORM_VEC+"\n" +
            "del "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC+", "+VAL_I
            );
            SP.Python.exec(VAL_TRAIN_ENG+" /= "+VAL_TRAIN_ATOM_NUM);
            if (mHasTest) {
                SP.Python.exec(VAL_TEST_BASE+" = [torch.tensor(sub_base.asListRows(), dtype=torch.float64) for sub_base in "+VAL_TEST_BASE_J+"]");
                SP.Python.exec(VAL_TEST_ENG+" = torch.tensor("+VAL_TEST_ENG_J+".asList(), dtype=torch.float64)");
                SP.Python.exec(VAL_TEST_ATOM_NUM+" = torch.tensor("+VAL_TEST_ATOM_NUM_J+".asList(), dtype=torch.float64)");
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                "for "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC+" in zip("+VAL_TEST_BASE+", "+VAL_NORM_VEC+"):\n" +
                "    for "+VAL_I+" in range(len("+VAL_SUB_BASE+")):\n" +
                "        "+VAL_SUB_BASE+"["+VAL_I+", :-1] /= "+VAL_SUB_NORM_VEC+"\n" +
                "del "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC+", "+VAL_I
                );
                SP.Python.exec(VAL_TEST_ENG+" /= "+VAL_TEST_ATOM_NUM);
            }
        } finally {
            SP.Python.removeValue(VAL_NORM_VEC_J);
            SP.Python.removeValue(VAL_TRAIN_BASE_J);
            SP.Python.removeValue(VAL_TRAIN_ENG_J);
            SP.Python.removeValue(VAL_TRAIN_ATOM_NUM_J);
            if (mHasTest) {
                SP.Python.removeValue(VAL_TEST_BASE_J);
                SP.Python.removeValue(VAL_TEST_ENG_J);
                SP.Python.removeValue(VAL_TEST_ATOM_NUM_J);
            }
        }
        // 开始训练
        try {
            if (aPrintLog) UT.Timer.progressBar("train", aEpochs);
            SP.Python.exec(VAL_MODEL+".train()");
            double tMinLoss = Double.POSITIVE_INFINITY;
            int tSelectEpoch = -1;
            for (int i = 0; i < aEpochs; ++i) {
                double tLoss = ((Number)SP.Python.invoke(FN_TRAIN_STEP)).doubleValue();
                double oLoss = mTrainLoss.isEmpty() ? Double.NaN : mTrainLoss.last();
                mTrainLoss.add(tLoss);
                double tTestLoss = Double.NaN;
                if (mHasTest) {
                    tTestLoss = ((Number)SP.Python.invoke(FN_TEST_LOSS)).doubleValue();
                    mTestLoss.add(tTestLoss);
                    if (aEarlyStop && tTestLoss<tMinLoss) {
                        tSelectEpoch = i;
                        tMinLoss = tTestLoss;
                        SP.Python.exec(VAL_MODEL_STATE_DICT+" = copy.deepcopy("+VAL_MODEL+".state_dict())");
                    }
                }
                if (!Double.isNaN(oLoss) && Math.abs(oLoss-tLoss)<(tLoss*1e-8)) {
                    if (aPrintLog) for (int j = i; j < aEpochs; ++j) {
                        UT.Timer.progressBar(mHasTest ? String.format("loss: %.4g | %.4g", tLoss, tTestLoss) : String.format("loss: %.4g", tLoss));
                    }
                    break;
                }
                if (aPrintLog) {
                    UT.Timer.progressBar(mHasTest ? String.format("loss: %.4g | %.4g", tLoss, tTestLoss) : String.format("loss: %.4g", tLoss));
                }
            }
            if (aEarlyStop && tSelectEpoch>=0) {
                SP.Python.exec(VAL_MODEL+".load_state_dict("+VAL_MODEL_STATE_DICT+"); del "+VAL_MODEL_STATE_DICT);
                System.out.printf("Model at epoch = %d selected, test loss = %.4g\n", tSelectEpoch, tMinLoss);
            }
            double tMAE = ((Number)SP.Python.eval(FN_CAL_MAE+"("+VAL_TRAIN_BASE+", "+VAL_TRAIN_ATOM_NUM+", "+VAL_TRAIN_ENG+")")).doubleValue();
            if (mHasTest) {
                double tTestMAE = ((Number)SP.Python.eval(FN_CAL_MAE+"("+VAL_TEST_BASE+", "+VAL_TEST_ATOM_NUM+", "+VAL_TEST_ENG+")")).doubleValue();
                System.out.printf("MAE-E: %.4g meV | %.4g meV\n", tMAE*1000, tTestMAE*1000);
            } else {
                System.out.printf("MAE-E: %.4g meV\n", tMAE*1000);
            }
        } finally {
            // 完事移除临时数据
            SP.Python.removeValue(VAL_NORM_VEC);
            SP.Python.removeValue(VAL_TRAIN_BASE);
            SP.Python.removeValue(VAL_TRAIN_ENG);
            SP.Python.removeValue(VAL_TRAIN_ATOM_NUM);
            if (mHasTest) {
                SP.Python.removeValue(VAL_TEST_BASE);
                SP.Python.removeValue(VAL_TEST_ENG);
                SP.Python.removeValue(VAL_TEST_ATOM_NUM);
            }
        }
    }
    public void train(int aEpochs, boolean aEarlyStop) {train(aEpochs, aEarlyStop, true);}
    public void train(int aEpochs) {train(aEpochs, true);}
    
    @ApiStatus.Internal
    protected void calRefEngBaseAndAdd_(IAtomData aAtomData, double aEnergy, final DoubleList rEngData, DoubleList[] rBaseData) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                IBasis tBasis = basis(tType);
                RowMatrix tBase = tBasis.eval(tAPC, i, tTypeMap);
                rBaseData[tType-1].addAll(tBase.asVecRow());
                rBaseData[tType-1].add(rEngData.size());
                MatrixCache.returnMat(tBase);
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
            }
        }
        // 这里后添加能量，这样 rEngData.size() 对应正确的索引
        rEngData.add(aEnergy);
    }
    /** 增加一个训练集数据，目前只需要能量 */
    public void addTrainData(IAtomData aAtomData, double aEnergy) {
        // 添加数据
        calRefEngBaseAndAdd_(aAtomData, aEnergy, mTrainEng, mTrainBase);
        mTrainAtomNum.add(aAtomData.atomNumber());
    }
    /** 增加一个测试集数据，目前只需要能量 */
    public void addTestData(IAtomData aAtomData, double aEnergy) {
        // 添加数据
        calRefEngBaseAndAdd_(aAtomData, aEnergy, mTestEng, mTestBase);
        mTestAtomNum.add(aAtomData.atomNumber());
        if (!mHasTest) mHasTest = true;
    }
    
    /** 获取历史 loss 值 */
    public IVector trainLoss() {return mTrainLoss.asVec();}
    public IVector testLoss() {return mTestLoss.asVec();}
    
    /** 保存训练的势函数 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        List rModels = new ArrayList();
        for (int i = 0; i < mSymbols.length; ++i) {
            Map rBasis = new LinkedHashMap();
            mBasis[i].save(rBasis);
            byte[] tModelByes = (byte[])SP.Python.invoke(FN_MODEL2BYTES, i);
            rModels.add(Maps.of(
                "symbol", mSymbols[i],
                "ref_eng", mRefEngs.get(i),
                "norm_vec", mNormVec[i].asList(),
                "basis", rBasis,
                "torch", Base64.getEncoder().encodeToString(tModelByes)
            ));
        }
        rSaveTo.put("version", VERSION);
        rSaveTo.put("units", mUnits);
        rSaveTo.put("models", rModels);
    }
    @SuppressWarnings({"rawtypes"})
    public void save(String aPath, boolean aPretty) throws IOException {
        Map rJson = new LinkedHashMap();
        save(rJson);
        UT.IO.map2json(rJson, aPath, aPretty);
    }
    public void save(String aPath) throws IOException {save(aPath, false);}
}
