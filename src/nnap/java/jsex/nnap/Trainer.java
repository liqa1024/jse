package jsex.nnap;

import jep.NDArray;
import jep.python.PyCallable;
import jep.python.PyObject;
import jse.atom.IAtomData;
import jse.atom.AtomicParameterCalculator;
import jse.cache.IntVectorCache;
import jse.cache.MatrixCache;
import jse.code.SP;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.io.ISavable;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import jsex.nnap.basis.IBasis;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
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
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_L2_LOSS_WEIGHT = 0.0;
    protected final static boolean DEFAULT_CLEAR_DATA_ON_TRAINING = true;
    protected final static PyObject TORCH;
    /** 全局记录 python 中的变量名称 */
    protected final static String
          CLASS_SINGLE_MODEL                = "__NNAP_TRAINER_SingleEnergyModel__"
        , CLASS_TOTAL_MODEL                 = "__NNAP_TRAINER_TotalModel__"
        , FN_TRAIN_STEP                     = "__NNAP_TRAINER_train_step__"
        , FN_TEST_LOSS                      = "__NNAP_TRAINER_test_loss__"
        , FN_CAL_MAE                        = "__NNAP_TRAINER_cal_mae__"
        , FN_MODEL2BYTES                    = "__NNAP_TRAINER_model2bytes__"
        , VAL_MODEL                         = "__NNAP_TRAINER_model__"
        , VAL_MODEL_STATE_DICT              = "__NNAP_TRAINER_model_state_dict__"
        , VAL_OPTIMIZER                     = "__NNAP_TRAINER_optimizer__"
        , VAL_LOSS_FN                       = "__NNAP_TRAINER_loss_fn__"
        , VAL_LOSS_FN_ENG                   = "__NNAP_TRAINER_loss_fn_eng__"
        , VAL_LOSS_FN_FORCE                 = "__NNAP_TRAINER_loss_fn_force__"
        , VAL_HAS_FORCE                     = "__NNAP_TRAINER_has_force__"
        , VAL_FORCE_WEIGHT                  = "__NNAP_TRAINER_force_wright__"
        , VAL_L2_LOSS_WEIGHT                = "__NNAP_TRAINER_l2_loss_wright__"
        , VAL_TRAIN_BASE                    = "__NNAP_TRAINER_train_base__"
        , VAL_TRAIN_BASE_J                  = "__NNAP_TRAINER_train_base_J__"
        , VAL_TRAIN_ENG_INDICES             = "__NNAP_TRAINER_train_eng_indices__"
        , VAL_TRAIN_ENG_INDICES_J           = "__NNAP_TRAINER_train_eng_indices_J__"
        , VAL_TRAIN_BASE_PARTIAL            = "__NNAP_TRAINER_train_base_partial__"
        , VAL_TRAIN_BASE_PARTIAL_J          = "__NNAP_TRAINER_train_base_partial_J__"
        , VAL_TRAIN_FORCE_INDICES           = "__NNAP_TRAINER_train_force_indices__"
        , VAL_TRAIN_FORCE_INDICES_J         = "__NNAP_TRAINER_train_force_indices_J__"
        , VAL_TRAIN_ATOM_NUM                = "__NNAP_TRAINER_train_atom_num__"
        , VAL_TRAIN_ATOM_NUM_J              = "__NNAP_TRAINER_train_atom_num_J__"
        , VAL_TRAIN_ENG                     = "__NNAP_TRAINER_train_eng__"
        , VAL_TRAIN_ENG_J                   = "__NNAP_TRAINER_train_eng_J__"
        , VAL_TRAIN_FORCE                   = "__NNAP_TRAINER_train_force__"
        , VAL_TRAIN_FORCE_J                 = "__NNAP_TRAINER_train_force_J__"
        , VAL_NORM_VEC                      = "__NNAP_TRAINER_norm_vec__"
        , VAL_NORM_VEC_J                    = "__NNAP_TRAINER_norm_vec_J__"
        , VAL_TEST_BASE                     = "__NNAP_TRAINER_test_base__"
        , VAL_TEST_BASE_J                   = "__NNAP_TRAINER_test_base_J__"
        , VAL_TEST_ENG_INDICES              = "__NNAP_TRAINER_test_eng_indices__"
        , VAL_TEST_ENG_INDICES_J            = "__NNAP_TRAINER_test_eng_indices_J__"
        , VAL_TEST_BASE_PARTIAL             = "__NNAP_TRAINER_test_base_partial__"
        , VAL_TEST_BASE_PARTIAL_J           = "__NNAP_TRAINER_test_base_partial_J__"
        , VAL_TEST_FORCE_INDICES            = "__NNAP_TRAINER_test_force_indices__"
        , VAL_TEST_FORCE_INDICES_J          = "__NNAP_TRAINER_test_force_indices_J__"
        , VAL_TEST_ATOM_NUM                 = "__NNAP_TRAINER_test_atom_num__"
        , VAL_TEST_ATOM_NUM_J               = "__NNAP_TRAINER_test_atom_num_J__"
        , VAL_TEST_ENG                      = "__NNAP_TRAINER_test_eng__"
        , VAL_TEST_ENG_J                    = "__NNAP_TRAINER_test_eng_J__"
        , VAL_TEST_FORCE                    = "__NNAP_TRAINER_test_force__"
        , VAL_TEST_FORCE_J                  = "__NNAP_TRAINER_test_force_J__"
        , VAL_NTYPES                        = "__NNAP_TRAINER_ntypes__"
        , VAL_HIDDEN_DIMS                   = "__NNAP_TRAINER_hidden_dims__"
        , VAL_INPUT_DIMS                    = "__NNAP_TRAINER_input_dims__"
        , VAL_SUB_NORM_VEC                  = "__NNAP_TRAINER_sub_norm_vec__"
        , VAL_SUB_BASE                      = "__NNAP_TRAINER_sub_base__"
        , VAL_SUB_BASE_B                    = "__NNAP_TRAINER_sub_base_b__"
        , VAL_SUB                           = "__NNAP_TRAINER_sub__"
        , VAL_I                             = "__NNAP_TRAINER_i__"
        , VAL_S                             = "__NNAP_TRAINER_s__"
        , VAL_INDICES                       = "__NNAP_TRAINER_indices__"
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
            "    def __init__(self, input_dim, hidden_dims):\n" +
            "        super().__init__()\n" +
            "        self.in_layer = torch.nn.Linear(input_dim, hidden_dims[0])\n" +
            "        self.hidden_layers = torch.nn.ModuleList([torch.nn.Linear(hidden_dims[i], hidden_dims[i+1]) for i in range(len(hidden_dims)-1)])\n" +
            "        self.out_layer = torch.nn.Linear(hidden_dims[-1], 1)\n" +
            "        self.active = torch.nn.SiLU()\n" +
            "    \n" +
            "    def forward(self, x):\n" +
            "        x = self.active(self.in_layer(x))\n" +
            "        for hidden_layer in self.hidden_layers:\n" +
            "            x = self.active(hidden_layer(x))\n" +
            "        x = self.out_layer(x)\n" +
            "        return torch.reshape(x, x.shape[:-1])";
        
        /** 创建获取总能量的模型的类，一般情况不需要重写 */
        public static String TOTAL_MODEL_SCRIPT =
            "class "+CLASS_TOTAL_MODEL+"(torch.nn.Module):\n" +
            "    def __init__(self, input_dims, hidden_dims, ntypes=1):\n" +
            "        super().__init__()\n" +
            "        self.input_dims = input_dims\n" +
            "        self.ntypes = ntypes\n" +
            "        self.sub_models = torch.nn.ModuleList(["+CLASS_SINGLE_MODEL+"(input_dims[i], hidden_dims[i]) for i in range(ntypes)])\n" +
            "    \n" +
            "    def forward(self, x):\n" +
            "        return [self.sub_models[i](x[i]) for i in range(self.ntypes)]\n" +
            "    \n" +
            "    def cal_l2_loss(self):\n" +
            "        l2_loss = 0.0\n" +
            "        nparams = 0\n" +
            "        for param in self.parameters():\n" +
            "            param = param.flatten()\n" +
            "            l2_loss += param.dot(param).mean()\n" +
            "            nparams += 1\n" +
            "        l2_loss /= nparams\n" +
            "        return l2_loss\n" +
            "    \n" +
            "    def cal_eng_force(self, fp, indices, atom_nums, xyz_grad=None, indices_f=None, create_graph=False):\n" +
            "        eng_all = self.forward(fp)\n" +
            "        eng_len = len(atom_nums)\n" +
            "        eng = torch.zeros(eng_len, device=atom_nums.device, dtype=atom_nums.dtype)\n" +
            "        for i in range(self.ntypes):\n" +
            "            eng.scatter_add_(0, indices[i], eng_all[i])\n" +
            "        eng /= atom_nums\n" +
            "        if xyz_grad is None:\n" +
            "            return eng\n" +
            "        force_len = atom_nums.sum().long().item()*3\n" + // 这里直接把力全部展开成单个向量
            "        force = torch.zeros(force_len, device=atom_nums.device, dtype=atom_nums.dtype)\n" +
            "        for i in range(self.ntypes):\n" +
            "            fp_grad = torch.autograd.grad(eng_all[i].sum(), fp[i], create_graph=create_graph)[0]\n" +
            "            split_ = indices_f[0][i].shape[0]\n" +
            "            force_all = -torch.bmm(xyz_grad[0][i], fp_grad[:split_, :].unsqueeze(-1))\n" +
            "            force.scatter_add_(0, indices_f[0][i].flatten(), force_all.flatten())\n" +
            "            force_all = -torch.bmm(xyz_grad[1][i], fp_grad[split_:, :].unsqueeze(-1))\n" +
            "            force.scatter_add_(0, indices_f[1][i].flatten(), force_all.flatten())\n" +
            "        return eng, force";
        
        /** 总 loss 函数，修改此值实现自定义 loss 函数，主要用于控制各种 loss 之间的比例 */
        public static String LOSS_FN_SCRIPT =
            "def "+VAL_LOSS_FN+"(pred_engs, target_engs, pred_forces=None, target_forces=None):\n" +
            "    eng_loss = "+VAL_LOSS_FN_ENG+"(pred_engs, target_engs)\n" +
            "    if pred_forces is None:\n" +
            "        return eng_loss\n" +
            "    return eng_loss + "+VAL_FORCE_WEIGHT+"*"+VAL_LOSS_FN_FORCE+"(pred_forces, target_forces)";
        
        /** 用于计算能量的 loss 函数，修改此值实现自定义 loss 函数；目前默认采用 SmoothL1Loss */
        public static String LOSS_FN_ENG_SCRIPT = VAL_LOSS_FN_ENG+"=torch.nn.SmoothL1Loss()";
        /** 用于计算能量的 loss 函数，修改此值实现自定义 loss 函数；目前默认采用 SmoothL1Loss */
        public static String LOSS_FN_FORCE_SCRIPT = VAL_LOSS_FN_FORCE+"=torch.nn.SmoothL1Loss()";
        
        /** 训练一步的脚本函数，修改此值来实现自定义训练算法；默认使用 LBFGS 训练 */
        public static String TRAIN_STEP_SCRIPT =
            "def "+FN_TRAIN_STEP+"():\n" +
            "    "+VAL_MODEL+".train()\n"+
            "    def closure():\n" +
            "        "+VAL_OPTIMIZER+".zero_grad()\n" +
            "        if not "+VAL_HAS_FORCE+":\n" +
            "            pred = "+VAL_MODEL+".cal_eng_force("+VAL_TRAIN_BASE+", "+VAL_TRAIN_ENG_INDICES+", "+VAL_TRAIN_ATOM_NUM+")\n" +
            "            loss = "+VAL_LOSS_FN+"(pred, "+VAL_TRAIN_ENG+")\n" +
            "        else:\n" +
            "            pred, pred_f = "+VAL_MODEL+".cal_eng_force("+VAL_TRAIN_BASE+", "+VAL_TRAIN_ENG_INDICES+", "+VAL_TRAIN_ATOM_NUM+", "+VAL_TRAIN_BASE_PARTIAL+", "+VAL_TRAIN_FORCE_INDICES+", True)\n" +
            "            loss = "+VAL_LOSS_FN+"(pred, "+VAL_TRAIN_ENG+", pred_f, "+VAL_TRAIN_FORCE+")\n" +
            "        loss += "+VAL_L2_LOSS_WEIGHT+"*"+VAL_MODEL+".cal_l2_loss()\n" +
            "        loss.backward()\n" +
            "        return loss\n" +
            "    loss = closure()\n" +
            "    "+VAL_OPTIMIZER+".step(closure)\n" +
            "    return loss.item()";
        
        /** 获取测试集误差的脚本函数，一般情况不需要重写 */
        public static String TEST_LOSS_SCRIPT =
            "def "+FN_TEST_LOSS+"():\n" +
            "    "+VAL_MODEL+".eval()\n"+
            "    if not "+VAL_HAS_FORCE+":\n" +
            "        with torch.no_grad():\n" +
            "            pred = "+VAL_MODEL+".cal_eng_force("+VAL_TEST_BASE+", "+VAL_TEST_ENG_INDICES+", "+VAL_TEST_ATOM_NUM+")\n" +
            "            loss = "+VAL_LOSS_FN+"(pred, "+VAL_TEST_ENG+")\n" +
            "            return loss.item()\n" +
            "    pred, pred_f = "+VAL_MODEL+".cal_eng_force("+VAL_TEST_BASE+", "+VAL_TEST_ENG_INDICES+", "+VAL_TEST_ATOM_NUM+", "+VAL_TEST_BASE_PARTIAL+", "+VAL_TEST_FORCE_INDICES+")\n" +
            "    loss = "+VAL_LOSS_FN+"(pred, "+VAL_TEST_ENG+", pred_f, "+VAL_TEST_FORCE+")\n" +
            "    return loss.item()";
        
        /** 将模型转为字节的脚本，一般情况不需要重写 */
        public static String MODEL2BYTES_SCRIPT =
            "def "+FN_MODEL2BYTES+"(i):\n" +
            "    sub_model = "+VAL_MODEL+".sub_models[i].double()\n" +
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
        TORCH = SP.Python.getClass("torch");
        SP.Python.exec("import copy");
        SP.Python.exec("import io");
        SP.Python.exec(Conf.INIT_SCRIPT);
        SP.Python.exec(Conf.SINGLE_MODEL_SCRIPT);
        SP.Python.exec(Conf.TOTAL_MODEL_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_ENG_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_FORCE_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_SCRIPT);
        SP.Python.exec(Conf.TRAIN_STEP_SCRIPT);
        SP.Python.exec(Conf.TEST_LOSS_SCRIPT);
        //noinspection ConcatenationWithEmptyString
        SP.Python.exec("" +
        "def "+FN_CAL_MAE+"(fp, indices, atom_nums, engs, xyz_grad=None, indices_f=None, forces=None):\n" +
        "    "+VAL_MODEL+".eval()\n"+
        "    if forces is None:\n" +
        "        with torch.no_grad():\n" +
        "            pred = "+VAL_MODEL+".cal_eng_force(fp, indices, atom_nums)\n" +
        "            return (engs - pred).abs().mean().item()\n" +
        "    pred, pred_f = "+VAL_MODEL+".cal_eng_force(fp, indices, atom_nums, xyz_grad, indices_f)\n" +
        "    return (engs - pred).abs().mean().item(), (forces - pred_f).abs().mean().item()"
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
    protected boolean mTrainInFloat = true;
    public Trainer setTrainInFloat(boolean aFlag) {mTrainInFloat = aFlag; return this;}
    protected double mForceWeight = DEFAULT_FORCE_WEIGHT;
    public Trainer setForceWeight(double aWeight) {mForceWeight = aWeight; return this;}
    protected double mL2LossWeight = DEFAULT_L2_LOSS_WEIGHT;
    public Trainer setL2LossWeight(double aWeight) {mL2LossWeight = aWeight; return this;}
    protected boolean mClearDataOnTraining = DEFAULT_CLEAR_DATA_ON_TRAINING;
    public Trainer setClearDataOnTraining(boolean aFlag) {mClearDataOnTraining = aFlag; return this;}
    
    
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final IBasis[] mBasis;
    protected final DoubleList[] mTrainBase; // 按照种类排序，内部是可扩展的具体数据；现在使用这种 DoubleList 展开的形式存
    protected final RowMatrix[] mTrainBaseMat; // mTrainDataBase 的实际值（按行排列），这个只是缓存结果
    protected final IntList[] mTrainEngIndices; // 每个 base 对应的能量的索引，按照种类排序
    protected final List<List<PyObject>> mTrainBasePartial; // 按照种类排序，内部是每个原子的所有基组偏导值，每个基组偏导值按行排列，和交叉项以及 xyz 共同组成一个矩阵；为了减少内存占用现在统一直接存 torch tensor
    protected final List<List<PyObject>> mTrainForceIndices; // 每个 base partial 对应的力的索引，按照种类排序；为了减少内存占用现在统一直接存 torch tensor
    protected final IntList[] mTrainNN; // 训练集每个原子的近邻原子数列表
    protected final DoubleList mTrainEng = new DoubleList(64);
    protected final DoubleList mTrainForce = new DoubleList(64); // 这里的力数值直接展开成单个向量，通过 mTrainForceIndices 来获取对应的索引
    protected final DoubleList mTrainStress = new DoubleList(64); // 这里的压力数值直接展开成单个向量，通过 mTrainStressIndices 来获取对应的索引
    protected final IntList mTrainAtomNum = new IntList(64);
    protected final Vector[] mNormVec;
    protected final DoubleList[] mTestBase;
    protected final RowMatrix[] mTestBaseMat;
    protected final IntList[] mTestEngIndices;
    protected final List<List<PyObject>> mTestBasePartial;
    protected final List<List<PyObject>> mTestForceIndices;
    protected final IntList[] mTestNN;
    protected final DoubleList mTestEng = new DoubleList(64);
    protected final DoubleList mTestForce = new DoubleList(64);
    protected final DoubleList mTestStress = new DoubleList(64);
    protected final IntList mTestAtomNum = new IntList(64);
    protected boolean mHasData = false;
    protected boolean mHasForce = false;
    protected boolean mHasStress = false;
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
        mTrainEngIndices = new IntList[mSymbols.length];
        mTrainBasePartial = new ArrayList<>(mSymbols.length);
        mTrainForceIndices = new ArrayList<>(mSymbols.length);
        mTrainNN = new IntList[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            mTrainBase[i] = new DoubleList(64);
            mTrainEngIndices[i] = new IntList(64);
            mTrainBasePartial.add(new ArrayList<>(64));
            mTrainForceIndices.add(new ArrayList<>(64));
            mTrainNN[i] = new IntList(64);
        }
        mTrainBaseMat = new RowMatrix[mSymbols.length];
        mTestBase = new DoubleList[mSymbols.length];
        mTestEngIndices = new IntList[mSymbols.length];
        mTestBasePartial = new ArrayList<>(mSymbols.length);
        mTestForceIndices = new ArrayList<>(mSymbols.length);
        mTestNN = new IntList[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            mTestBase[i] = new DoubleList(64);
            mTestEngIndices[i] = new IntList(64);
            mTestBasePartial.add(new ArrayList<>(64));
            mTestForceIndices.add(new ArrayList<>(64));
            mTestNN[i] = new IntList(64);
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
        SP.Python.setValue(VAL_INPUT_DIMS, NewCollections.map(mBasis, base -> base.rowNumber()*base.columnNumber()));
        SP.Python.setValue(VAL_HIDDEN_DIMS, tHiddenDims);
        SP.Python.setValue(VAL_NTYPES, mSymbols.length);
        try {
            SP.Python.exec(VAL_MODEL+" = "+CLASS_TOTAL_MODEL+"("+VAL_INPUT_DIMS+", "+VAL_HIDDEN_DIMS+", ntypes="+VAL_NTYPES+")");
            if (!mTrainInFloat) SP.Python.exec(VAL_MODEL+" = "+VAL_MODEL+".double()");
        } finally {
            SP.Python.removeValue(VAL_NTYPES);
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
                mNormVec[i].operation().operate2this(tRow, (lhs, rhs) -> lhs + Math.abs(rhs));
            }
            mNormVec[i].div2this(mTrainBaseMat[i].rowNumber());
        }
    }
    @ApiStatus.Internal
    protected int calBestSplit_(IIntVector aSortedNN) {
        final int tSize = aSortedNN.size();
        final int tSizeMM = tSize-1;
        long tMinSizeOut = (long)aSortedNN.last()*tSize;
        int tBest = 0;
        for (int split = 1; split < tSizeMM; ++split) {
            long tSizeOut = (long)aSortedNN.get(split-1)*split + (long)aSortedNN.get(tSizeMM)*(tSize-split);
            if (tSizeOut < tMinSizeOut) {
                tBest = split;
                tMinSizeOut = tSizeOut;
            }
        }
        return tBest;
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
            final int tColNum = mBasis[i].rowNumber()*mBasis[i].columnNumber();
            final int tRowNum = mTrainBase[i].size() / tColNum;
            mTrainBaseMat[i] = new RowMatrix(tRowNum, tColNum, mTrainBase[i].internalData());
        }
        if (mHasTest) for (int i = 0; i < mTestBase.length; ++i) {
            final int tColNum = mBasis[i].rowNumber()*mBasis[i].columnNumber();
            final int tRowNum = mTestBase[i].size() / tColNum;
            mTestBaseMat[i] = new RowMatrix(tRowNum, tColNum, mTestBase[i].internalData());
        }
        // 重新构建归一化向量
        initNormVec();
        // 构造 torch 数据，这里直接把数据放到 python 环境中变成一个 python 的全局变量
        SP.Python.setValue(VAL_HAS_FORCE, mHasForce);
        SP.Python.setValue(VAL_FORCE_WEIGHT, mForceWeight);
        SP.Python.setValue(VAL_L2_LOSS_WEIGHT, mL2LossWeight);
        SP.Python.setValue(VAL_NORM_VEC_J, mNormVec);
        SP.Python.setValue(VAL_TRAIN_BASE_J, mTrainBaseMat);
        SP.Python.setValue(VAL_TRAIN_ENG_INDICES_J, mTrainEngIndices);
        SP.Python.setValue(VAL_TRAIN_ENG_J, mTrainEng);
        SP.Python.setValue(VAL_TRAIN_ATOM_NUM_J, mTrainAtomNum);
        if (mHasForce) {
            SP.Python.setValue(VAL_TRAIN_BASE_PARTIAL_J, mTrainBasePartial);
            SP.Python.setValue(VAL_TRAIN_FORCE_INDICES_J, mTrainForceIndices);
            SP.Python.setValue(VAL_TRAIN_FORCE_J, mTrainForce);
        }
        if (mHasTest) {
            SP.Python.setValue(VAL_TEST_BASE_J, mTestBaseMat);
            SP.Python.setValue(VAL_TEST_ENG_INDICES_J, mTestEngIndices);
            SP.Python.setValue(VAL_TEST_ENG_J, mTestEng);
            SP.Python.setValue(VAL_TEST_ATOM_NUM_J, mTestAtomNum);
            if (mHasForce) {
                SP.Python.setValue(VAL_TEST_BASE_PARTIAL_J, mTestBasePartial);
                SP.Python.setValue(VAL_TEST_FORCE_INDICES_J, mTestForceIndices);
                SP.Python.setValue(VAL_TEST_FORCE_J, mTestForce);
            }
        }
        try {
            String tDType = mTrainInFloat ? "float32" : "float64";
            SP.Python.exec(VAL_NORM_VEC+" = [torch.tensor("+VAL_SUB+".asList(), dtype=torch."+tDType+") for "+VAL_SUB+" in "+VAL_NORM_VEC_J+"]");
            SP.Python.exec(VAL_TRAIN_ENG_INDICES+" = [torch.tensor("+VAL_SUB+".asList(), dtype=torch.int64) for "+VAL_SUB+" in "+VAL_TRAIN_ENG_INDICES_J+"]");
            SP.Python.exec(VAL_TRAIN_ENG+" = torch.tensor("+VAL_TRAIN_ENG_J+".asList(), dtype=torch."+tDType+")");
            SP.Python.exec(VAL_TRAIN_ATOM_NUM+" = torch.tensor("+VAL_TRAIN_ATOM_NUM_J+".asList(), dtype=torch."+tDType+")");
            // 这里将最大的 Base 数据也用 numpy 的方式转换
            SP.Python.exec(VAL_TRAIN_BASE+" = []");
            for (int i = 0; i < mSymbols.length; ++i) {
                NDArray<double[]> tBaseNP = new NDArray<>(mTrainBaseMat[i].internalData(), mTrainBaseMat[i].rowNumber(), mTrainBaseMat[i].columnNumber());
                SP.Python.setValue(VAL_SUB, tBaseNP);
                SP.Python.exec(VAL_TRAIN_BASE+".append(torch.from_numpy("+VAL_SUB+(mTrainInFloat?").float())":"))"));
            }
            if (mClearDataOnTraining) {
                for (int i = 0; i < mSymbols.length; ++i) {
                    mTrainBase[i].clear();
                    mTrainEngIndices[i].clear();
                    mTrainBase[i].clear();
                    mTrainBaseMat[i] = null;
                }
                mTrainEng.clear();
                mTrainAtomNum.clear();
            }
            //noinspection ConcatenationWithEmptyString
            SP.Python.exec("" +
            "for "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC+" in zip("+VAL_TRAIN_BASE+", "+VAL_NORM_VEC+"):\n" +
            "    "+VAL_SUB_BASE+" /= "+VAL_SUB_NORM_VEC+"\n" +
            "del "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC
            );
            SP.Python.exec(VAL_TRAIN_ENG+" /= "+VAL_TRAIN_ATOM_NUM);
            if (mHasForce) {
                SP.Python.exec(VAL_TRAIN_BASE_PARTIAL+" = ([], [])");
                SP.Python.exec(VAL_TRAIN_FORCE_INDICES+" = ([], [])");
                for (int i = 0; i < mSymbols.length; ++i) {
                    // 先根据近邻数排序 BasePartial 和 Indices，拆分成两份来减少内存占用
                    final List<PyObject> tSubBasePartial = mTrainBasePartial.get(i);
                    final List<PyObject> tSubBasePartialIndices = mTrainForceIndices.get(i);
                    IntVector tSubNN = mTrainNN[i].asVec();
                    IntVector tSortIndices = Vectors.range(tSubNN.size());
                    tSubNN.operation().biSort((ii, jj) -> {
                        Collections.swap(tSubBasePartial, ii, jj);
                        Collections.swap(tSubBasePartialIndices, ii, jj);
                        tSortIndices.swap(ii, jj);
                    });
                    SP.Python.setValue(VAL_INDICES, tSortIndices.internalData());
                    SP.Python.setValue(VAL_S, calBestSplit_(tSubNN));
                    SP.Python.setValue(VAL_I, i);
                    // 基组本身也需要同步排序
                    SP.Python.exec(VAL_TRAIN_BASE+"["+VAL_I+"] = "+VAL_TRAIN_BASE+"["+VAL_I+"]["+VAL_INDICES+", :]");
                    SP.Python.exec(VAL_TRAIN_ENG_INDICES+"["+VAL_I+"] = "+VAL_TRAIN_ENG_INDICES+"["+VAL_I+"]["+VAL_INDICES+"]");
                    SP.Python.removeValue(VAL_INDICES);
                    // 直接通过 torch.nn.utils.rnn.pad_sequence 来填充 0
                    SP.Python.exec(VAL_TRAIN_BASE_PARTIAL+"[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_TRAIN_BASE_PARTIAL_J+"["+VAL_I+"][:"+VAL_S+"], batch_first=True))");
                    SP.Python.exec(VAL_TRAIN_BASE_PARTIAL+"[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_TRAIN_BASE_PARTIAL_J+"["+VAL_I+"]["+VAL_S+":], batch_first=True))");
                    SP.Python.exec(VAL_TRAIN_FORCE_INDICES+"[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_TRAIN_FORCE_INDICES_J+"["+VAL_I+"][:"+VAL_S+"], batch_first=True).long())");
                    SP.Python.exec(VAL_TRAIN_FORCE_INDICES+"[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_TRAIN_FORCE_INDICES_J+"["+VAL_I+"]["+VAL_S+":], batch_first=True).long())");
                    // 遍历过程中清空数据，进一步减少转换过程的内存占用峰值
                    if (mClearDataOnTraining) {
                        tSubBasePartial.forEach(PyObject::close);
                        tSubBasePartial.clear();
                        tSubBasePartialIndices.forEach(PyObject::close);
                        tSubBasePartialIndices.clear();
                        mTrainNN[i].clear();
                    }
                }
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                "for "+VAL_SUB_BASE+", "+VAL_SUB_BASE_B+", "+VAL_SUB_NORM_VEC+" in zip("+VAL_TRAIN_BASE_PARTIAL+"[0], "+VAL_TRAIN_BASE_PARTIAL+"[1], "+VAL_NORM_VEC+"):\n" +
                "    "+VAL_SUB_BASE+" /= "+VAL_SUB_NORM_VEC+"\n" +
                "    "+VAL_SUB_BASE_B+" /= "+VAL_SUB_NORM_VEC+"\n" +
                "del "+VAL_SUB_BASE+", "+VAL_SUB_BASE_B+", "+VAL_SUB_NORM_VEC
                );
                SP.Python.exec(VAL_TRAIN_FORCE+" = torch.tensor("+VAL_TRAIN_FORCE_J+".asList(), dtype=torch."+tDType+")");
                if (mClearDataOnTraining) {
                    mTrainForce.clear();
                }
                // 此时基组值本身需要梯度
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                "for "+VAL_SUB_BASE+" in "+VAL_TRAIN_BASE+":\n" +
                "    "+VAL_SUB_BASE+".requires_grad_(True)\n" +
                "del "+VAL_SUB_BASE
                );
            }
            if (mHasTest) {
                SP.Python.exec(VAL_TEST_ENG_INDICES+" = [torch.tensor("+VAL_SUB+".asList(), dtype=torch.int64) for "+VAL_SUB+" in "+VAL_TEST_ENG_INDICES_J+"]");
                SP.Python.exec(VAL_TEST_ENG+" = torch.tensor("+VAL_TEST_ENG_J+".asList(), dtype=torch."+tDType+")");
                SP.Python.exec(VAL_TEST_ATOM_NUM+" = torch.tensor("+VAL_TEST_ATOM_NUM_J+".asList(), dtype=torch."+tDType+")");
                // 这里将最大的 Base 数据也用 numpy 的方式转换
                SP.Python.exec(VAL_TEST_BASE+" = []");
                for (int i = 0; i < mSymbols.length; ++i) {
                    NDArray<double[]> tBaseNP = new NDArray<>(mTestBaseMat[i].internalData(), mTestBaseMat[i].rowNumber(), mTestBaseMat[i].columnNumber());
                    SP.Python.setValue(VAL_SUB, tBaseNP);
                    SP.Python.exec(VAL_TEST_BASE+".append(torch.from_numpy("+VAL_SUB+(mTrainInFloat?").float())":"))"));
                }
                if (mClearDataOnTraining) {
                    for (int i = 0; i < mSymbols.length; ++i) {
                        mTestBase[i].clear();
                        mTestEngIndices[i].clear();
                        mTestBase[i].clear();
                        mTestBaseMat[i] = null;
                    }
                    mTestEng.clear();
                    mTestAtomNum.clear();
                }
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                "for "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC+" in zip("+VAL_TEST_BASE+", "+VAL_NORM_VEC+"):\n" +
                "    "+VAL_SUB_BASE+" /= "+VAL_SUB_NORM_VEC+"\n" +
                "del "+VAL_SUB_BASE+", "+VAL_SUB_NORM_VEC
                );
                SP.Python.exec(VAL_TEST_ENG+" /= "+VAL_TEST_ATOM_NUM);
                if (mHasForce) {
                    SP.Python.exec(VAL_TEST_BASE_PARTIAL+" = ([], [])");
                    SP.Python.exec(VAL_TEST_FORCE_INDICES+" = ([], [])");
                    for (int i = 0; i < mSymbols.length; ++i) {
                        // 先根据近邻数排序 BasePartial 和 Indices，拆分成两份来减少内存占用
                        final List<PyObject> tSubBasePartial = mTestBasePartial.get(i);
                        final List<PyObject> tSubBasePartialIndices = mTestForceIndices.get(i);
                        IntVector tSubNN = mTestNN[i].asVec();
                        IntVector tSortIndices = Vectors.range(tSubNN.size());
                        tSubNN.operation().biSort((ii, jj) -> {
                            Collections.swap(tSubBasePartial, ii, jj);
                            Collections.swap(tSubBasePartialIndices, ii, jj);
                            tSortIndices.swap(ii, jj);
                        });
                        SP.Python.setValue(VAL_INDICES, tSortIndices.internalData());
                        SP.Python.setValue(VAL_S, calBestSplit_(tSubNN));
                        SP.Python.setValue(VAL_I, i);
                        // 基组本身也需要同步排序
                        SP.Python.exec(VAL_TEST_BASE+"["+VAL_I+"] = "+VAL_TEST_BASE+"["+VAL_I+"]["+VAL_INDICES+", :]");
                        SP.Python.exec(VAL_TEST_ENG_INDICES+"["+VAL_I+"] = "+VAL_TEST_ENG_INDICES+"["+VAL_I+"]["+VAL_INDICES+"]");
                        SP.Python.removeValue(VAL_INDICES);
                        // 直接通过 torch.nn.utils.rnn.pad_sequence 来填充 0
                        SP.Python.exec(VAL_TEST_BASE_PARTIAL+"[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_TEST_BASE_PARTIAL_J+"["+VAL_I+"][:"+VAL_S+"], batch_first=True))");
                        SP.Python.exec(VAL_TEST_BASE_PARTIAL+"[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_TEST_BASE_PARTIAL_J+"["+VAL_I+"]["+VAL_S+":], batch_first=True))");
                        SP.Python.exec(VAL_TEST_FORCE_INDICES+"[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_TEST_FORCE_INDICES_J+"["+VAL_I+"][:"+VAL_S+"], batch_first=True).long())");
                        SP.Python.exec(VAL_TEST_FORCE_INDICES+"[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_TEST_FORCE_INDICES_J+"["+VAL_I+"]["+VAL_S+":], batch_first=True).long())");
                        // 遍历过程中清空数据，进一步减少转换过程的内存占用峰值
                        if (mClearDataOnTraining) {
                            tSubBasePartial.forEach(PyObject::close);
                            tSubBasePartial.clear();
                            tSubBasePartialIndices.forEach(PyObject::close);
                            tSubBasePartialIndices.clear();
                            mTestNN[i].clear();
                        }
                    }
                    //noinspection ConcatenationWithEmptyString
                    SP.Python.exec("" +
                    "for "+VAL_SUB_BASE+", "+VAL_SUB_BASE_B+", "+VAL_SUB_NORM_VEC+" in zip("+VAL_TEST_BASE_PARTIAL+"[0], "+VAL_TEST_BASE_PARTIAL+"[1], "+VAL_NORM_VEC+"):\n" +
                    "    "+VAL_SUB_BASE+" /= "+VAL_SUB_NORM_VEC+"\n" +
                    "    "+VAL_SUB_BASE_B+" /= "+VAL_SUB_NORM_VEC+"\n" +
                    "del "+VAL_SUB_BASE+", "+VAL_SUB_BASE_B+", "+VAL_SUB_NORM_VEC
                    );
                    SP.Python.exec(VAL_TEST_FORCE+" = torch.tensor("+VAL_TEST_FORCE_J+".asList(), dtype=torch."+tDType+")");
                    if (mClearDataOnTraining) {
                        mTestForce.clear();
                    }
                    // 此时基组值本身需要梯度
                    //noinspection ConcatenationWithEmptyString
                    SP.Python.exec("" +
                    "for "+VAL_SUB+" in "+VAL_TEST_BASE+":\n" +
                    "    "+VAL_SUB+".requires_grad_(True)\n" +
                    "del "+VAL_SUB
                    );
                }
            }
        } finally {
            SP.Python.removeValue(VAL_NORM_VEC_J);
            SP.Python.removeValue(VAL_TRAIN_BASE_J);
            SP.Python.removeValue(VAL_TRAIN_ENG_INDICES_J);
            SP.Python.removeValue(VAL_TRAIN_ENG_J);
            SP.Python.removeValue(VAL_TRAIN_ATOM_NUM_J);
            if (mHasForce) {
                SP.Python.removeValue(VAL_TRAIN_BASE_PARTIAL_J);
                SP.Python.removeValue(VAL_TRAIN_FORCE_INDICES_J);
                SP.Python.removeValue(VAL_TRAIN_FORCE_J);
            }
            if (mHasTest) {
                SP.Python.removeValue(VAL_TEST_BASE_J);
                SP.Python.removeValue(VAL_TEST_ENG_INDICES_J);
                SP.Python.removeValue(VAL_TEST_ENG_J);
                SP.Python.removeValue(VAL_TEST_ATOM_NUM_J);
                if (mHasForce) {
                    SP.Python.removeValue(VAL_TEST_BASE_PARTIAL_J);
                    SP.Python.removeValue(VAL_TEST_FORCE_INDICES_J);
                    SP.Python.removeValue(VAL_TEST_FORCE_J);
                }
            }
        }
        // 开始训练
        try {
            if (aPrintLog) UT.Timer.progressBar("train", aEpochs);
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
            double tMAE_E, tMAE_F = Double.NaN;
            if (mHasForce) {
                List<?> tMAE = (List<?>)SP.Python.eval(FN_CAL_MAE+"("+VAL_TRAIN_BASE+", "+VAL_TRAIN_ENG_INDICES+", "+VAL_TRAIN_ATOM_NUM+", "+VAL_TRAIN_ENG+", "
                                                           +VAL_TRAIN_BASE_PARTIAL+", "+VAL_TRAIN_FORCE_INDICES+", "+VAL_TRAIN_FORCE+")");
                tMAE_E = ((Number)tMAE.get(0)).doubleValue();
                tMAE_F = ((Number)tMAE.get(1)).doubleValue();
            } else {
                tMAE_E = ((Number)SP.Python.eval(FN_CAL_MAE+"("+VAL_TRAIN_BASE+", "+VAL_TRAIN_ENG_INDICES+", "+VAL_TRAIN_ATOM_NUM+", "+VAL_TRAIN_ENG+")")).doubleValue();
            }
            if (mHasTest) {
                double tTestMAE_E, tTestMAE_F = Double.NaN;
                if (mHasForce) {
                    List<?> tMAE = (List<?>)SP.Python.eval(FN_CAL_MAE+"("+VAL_TEST_BASE+", "+VAL_TEST_ENG_INDICES+", "+VAL_TEST_ATOM_NUM+", "+VAL_TEST_ENG+", "
                                                               +VAL_TEST_BASE_PARTIAL+", "+VAL_TEST_FORCE_INDICES+", "+VAL_TEST_FORCE+")");
                    tTestMAE_E = ((Number)tMAE.get(0)).doubleValue();
                    tTestMAE_F = ((Number)tMAE.get(1)).doubleValue();
                } else {
                    tTestMAE_E = ((Number)SP.Python.eval(FN_CAL_MAE+"("+VAL_TEST_BASE+", "+VAL_TEST_ENG_INDICES+", "+VAL_TEST_ATOM_NUM+", "+VAL_TEST_ENG+")")).doubleValue();
                }
                System.out.printf("MAE-E: %.4g meV | %.4g meV\n", tMAE_E*1000, tTestMAE_E*1000);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g meV/A | %.4g meV/A\n", tMAE_F*1000, tTestMAE_F*1000);
                }
            } else {
                System.out.printf("MAE-E: %.4g meV\n", tMAE_E*1000);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g meV/A\n", tMAE_F*1000);
                }
            }
        } finally {
            // 完事移除临时数据
            SP.Python.removeValue(VAL_NORM_VEC);
            SP.Python.removeValue(VAL_TRAIN_BASE);
            SP.Python.removeValue(VAL_TRAIN_ENG_INDICES);
            SP.Python.removeValue(VAL_TRAIN_ENG);
            SP.Python.removeValue(VAL_TRAIN_ATOM_NUM);
            if (mHasForce) {
                SP.Python.removeValue(VAL_TRAIN_BASE_PARTIAL);
                SP.Python.removeValue(VAL_TRAIN_FORCE_INDICES);
                SP.Python.removeValue(VAL_TRAIN_FORCE);
            }
            if (mHasTest) {
                SP.Python.removeValue(VAL_TEST_BASE);
                SP.Python.removeValue(VAL_TEST_ENG_INDICES);
                SP.Python.removeValue(VAL_TEST_ENG);
                SP.Python.removeValue(VAL_TEST_ATOM_NUM);
                if (mHasForce) {
                    SP.Python.removeValue(VAL_TEST_BASE_PARTIAL);
                    SP.Python.removeValue(VAL_TEST_FORCE_INDICES);
                    SP.Python.removeValue(VAL_TEST_FORCE);
                }
            }
        }
    }
    public void train(int aEpochs, boolean aEarlyStop) {train(aEpochs, aEarlyStop, true);}
    public void train(int aEpochs) {train(aEpochs, true);}
    
    @ApiStatus.Internal
    protected void calRefEngBaseAndAdd_(IAtomData aAtomData, double aEnergy, final DoubleList rEngData, DoubleList[] rBaseData, IntList[] rBaseIndices) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                IBasis tBasis = basis(tType);
                RowMatrix tBase = tBasis.eval(tAPC, i, tTypeMap);
                rBaseData[tType-1].addAll(tBase.asVecRow());
                rBaseIndices[tType-1].add(rEngData.size());
                MatrixCache.returnMat(tBase);
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
            }
        }
        // 这里后添加能量，这样 rEngData.size() 对应正确的索引
        rEngData.add(aEnergy);
    }
    @ApiStatus.Internal
    protected void calRefEngBasePartialAndAdd_(IAtomData aAtomData, double aEnergy, final DoubleList rEngData, DoubleList[] rBaseData, IntList[] rBaseIndices,
                                               @NotNull IMatrix aForces, DoubleList rForceData, List<List<PyObject>> rBasePartial, List<List<PyObject>> rBasePartialIndices, IntList[] rNN) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                IBasis tBasis = basis(tType);
                List<RowMatrix> tOut = tBasis.evalPartial(true, true, tAPC, i, tTypeMap);
                // 基组和索引
                rBaseData[tType-1].addAll(tOut.get(0).asVecRow());
                rBaseIndices[tType-1].add(rEngData.size());
                // 基组偏导和索引
                int tRowNum = tOut.size()-1;
                final RowMatrix tBasePartial = MatrixCache.getMatRow(tRowNum, tBasis.rowNumber()*tBasis.columnNumber());
                final IntVector tBasePartialIndices = IntVectorCache.getVec(tRowNum);
                final int tShift = rForceData.size();
                tBasePartialIndices.set(0, tShift + 3*i);
                tBasePartialIndices.set(1, tShift + 3*i + 1);
                tBasePartialIndices.set(2, tShift + 3*i + 2);
                tBasePartial.row(0).fill(tOut.get(1).asVecRow());
                tBasePartial.row(1).fill(tOut.get(2).asVecRow());
                tBasePartial.row(2).fill(tOut.get(3).asVecRow());
                final int tNN = (tOut.size()-4)/3;
                final int[] j = {0};
                tAPC.nl_().forEachNeighbor(i, tBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                    tBasePartial.row(3 + 3*j[0]).fill(tOut.get(4+j[0]).asVecRow());
                    tBasePartial.row(4 + 3*j[0]).fill(tOut.get(4+tNN+j[0]).asVecRow());
                    tBasePartial.row(5 + 3*j[0]).fill(tOut.get(4+tNN+tNN+j[0]).asVecRow());
                    tBasePartialIndices.set(3 + 3*j[0], tShift + 3*idx);
                    tBasePartialIndices.set(4 + 3*j[0], tShift + 3*idx + 1);
                    tBasePartialIndices.set(5 + 3*j[0], tShift + 3*idx + 2);
                    ++j[0];
                });
                MatrixCache.returnMat(tOut);
                // 将数据转换为 torch 的 tensor，这里最快的方式是利用 torch 的 from_numpy 进行转换
                PyObject tPyBasePartial, tPyBasePartialIndices;
                try (PyCallable tFromNumpy = TORCH.getAttr("from_numpy", PyCallable.class)) {
                    tPyBasePartial = tFromNumpy.callAs(PyObject.class, new NDArray<>(tBasePartial.internalData(), tBasePartial.rowNumber(), tBasePartial.columnNumber()));
                    if (mTrainInFloat) {
                        try (PyObject oPyBasePartial = tPyBasePartial; PyCallable tFloat = oPyBasePartial.getAttr("float", PyCallable.class)) {
                            tPyBasePartial = tFloat.callAs(PyObject.class);
                        }
                    }
                    tPyBasePartialIndices = tFromNumpy.callAs(PyObject.class, new NDArray<>(tBasePartialIndices.internalData(), tBasePartialIndices.size()));
                }
                rBasePartial.get(tType-1).add(tPyBasePartial);
                rBasePartialIndices.get(tType-1).add(tPyBasePartialIndices);
                rNN[tType-1].add(tNN);
                MatrixCache.returnMat(tBasePartial);
                IntVectorCache.returnVec(tBasePartialIndices);
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
            }
        }
        // 这里后添加能量，这样 rEngData.size() 对应正确的索引
        rEngData.add(aEnergy);
        // 这里后添加力，这样 rForceData.size() 对应正确的索引
        rForceData.addAll(aForces.asVecRow());
    }
    
    /**
     * 增加一个训练集数据
     * <p>
     * 目前方便起见，如果有力则所有数据统一都要有力
     * @param aAtomData 原子结构数据
     * @param aEnergy 此原子结构数据的总能量
     * @param aForces 可选的每个原子的力，按行排列，每列对应 x,y,z 方向的力
     * @param aStress 可选的原子结构数据的应力值，按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列
     * @see IAtomData
     * @see IMatrix
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress) {
        if (!mHasData) {
            mHasData = true;
            mHasForce = aForces!=null;
            mHasStress = aStress!=null;
        } else {
            if (mHasForce && aForces==null) throw new IllegalArgumentException("All data MUST has forces when add force");
            if (!mHasForce && aForces!=null) throw new IllegalArgumentException("All data MUST NOT has forces when not add force");
            if (mHasStress && aStress==null) throw new IllegalArgumentException("All data MUST has stress when add stress");
            if (!mHasStress && aStress!=null) throw new IllegalArgumentException("All data MUST NOT has stress when not add stress");
        }
        // 添加数据
        if (mHasForce) {
            calRefEngBasePartialAndAdd_(aAtomData, aEnergy, mTrainEng, mTrainBase, mTrainEngIndices,
                                        aForces, mTrainForce, mTrainBasePartial, mTrainForceIndices, mTrainNN);
        } else {
            calRefEngBaseAndAdd_(aAtomData, aEnergy, mTrainEng, mTrainBase, mTrainEngIndices);
        }
        mTrainAtomNum.add(aAtomData.atomNumber());
        mHasData = true;
    }
    /**
     * {@code addTrainData(aAtomData, aEnergy, aForces, null)}
     * @see #addTrainData(IAtomData, double, IMatrix, IVector)
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy, IMatrix aForces) {addTrainData(aAtomData, aEnergy, aForces, null);}
    /**
     * {@code addTrainData(aAtomData, aEnergy, null, null)}
     * @see #addTrainData(IAtomData, double, IMatrix, IVector)
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy) {addTrainData(aAtomData, aEnergy, null, null);}
    /**
     * 增加一个测试集数据
     * <p>
     * 目前方便起见，如果有力则所有数据统一都要有力
     * @param aAtomData 原子结构数据
     * @param aEnergy 此原子结构数据的总能量
     * @param aForces 可选的每个原子的力，按行排列，每列对应 x,y,z 方向的力
     * @param aStress 可选的原子结构数据的应力值，按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列
     * @see IAtomData
     * @see IMatrix
     */
    public void addTestData(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress) {
        if (!mHasData) {
            mHasData = true;
            mHasForce = aForces!=null;
            mHasStress = aStress!=null;
        } else {
            if (mHasForce && aForces==null) throw new IllegalArgumentException("All data MUST has forces when add force");
            if (!mHasForce && aForces!=null) throw new IllegalArgumentException("All data MUST NOT has forces when not add force");
            if (mHasStress && aStress==null) throw new IllegalArgumentException("All data MUST has stress when add stress");
            if (!mHasStress && aStress!=null) throw new IllegalArgumentException("All data MUST NOT has stress when not add stress");
        }
        // 添加数据
        if (mHasForce) {
            calRefEngBasePartialAndAdd_(aAtomData, aEnergy, mTestEng, mTestBase, mTestEngIndices,
                                        aForces, mTestForce, mTestBasePartial, mTestForceIndices, mTestNN);
        } else {
            calRefEngBaseAndAdd_(aAtomData, aEnergy, mTestEng, mTestBase, mTestEngIndices);
        }
        mTestAtomNum.add(aAtomData.atomNumber());
        if (!mHasTest) mHasTest = true;
    }
    /**
     * {@code addTestData(aAtomData, aEnergy, aForces, null)}
     * @see #addTestData(IAtomData, double, IMatrix, IVector)
     */
    public void addTestData(IAtomData aAtomData, double aEnergy, IMatrix aForces) {addTestData(aAtomData, aEnergy, aForces, null);}
    /**
     * {@code addTestData(aAtomData, aEnergy, null, null)}
     * @see #addTestData(IAtomData, double, IMatrix, IVector)
     */
    public void addTestData(IAtomData aAtomData, double aEnergy) {addTestData(aAtomData, aEnergy, null, null);}
    
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
