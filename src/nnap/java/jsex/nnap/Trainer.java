package jsex.nnap;

import jep.NDArray;
import jep.python.PyCallable;
import jep.python.PyObject;
import jse.atom.IAtomData;
import jse.atom.AtomicParameterCalculator;
import jse.atom.IHasSymbol;
import jse.cache.IntMatrixCache;
import jse.cache.IntVectorCache;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.IO;
import jse.code.SP;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.code.io.ISavable;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowIntMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import jsex.nnap.basis.IBasis;
import jsex.nnap.basis.Mirror;
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
public class Trainer implements IHasSymbol, IAutoShutdown, ISavable {
    
    protected final static String DEFAULT_UNITS = "metal";
    protected final static int[] DEFAULT_HIDDEN_DIMS = {20, 20};
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_STRESS_WEIGHT = 1.0;
    protected final static double DEFAULT_L2_LOSS_WEIGHT = 0.001;
    protected final static boolean DEFAULT_CLEAR_DATA_ON_TRAINING = true;
    protected final static PyObject TORCH;
    /** 全局记录 python 中的变量名称 */
    protected final static String
          CLASS_DATA_SET                    = "__NNAP_TRAINER_DataSet__"
        , CLASS_SINGLE_MODEL                = "__NNAP_TRAINER_SingleEnergyModel__"
        , CLASS_TOTAL_MODEL                 = "__NNAP_TRAINER_TotalModel__"
        , FN_TRAIN_STEP                     = "__NNAP_TRAINER_train_step__"
        , FN_TEST_LOSS                      = "__NNAP_TRAINER_test_loss__"
        , FN_CAL_MAE                        = "__NNAP_TRAINER_cal_mae__"
        , FN_CAL_LOSS_DETAIL                = "__NNAP_TRAINER_cal_loss_detail__"
        , FN_MODEL2BYTES                    = "__NNAP_TRAINER_model2bytes__"
        , VAL_MODEL                         = "__NNAP_TRAINER_model__"
        , VAL_MODEL_STATE_DICT              = "__NNAP_TRAINER_model_state_dict__"
        , VAL_OPTIMIZER                     = "__NNAP_TRAINER_optimizer__"
        , VAL_LOSS_FN                       = "__NNAP_TRAINER_loss_fn__"
        , VAL_LOSS_FN_ENG                   = "__NNAP_TRAINER_loss_fn_eng__"
        , VAL_LOSS_FN_FORCE                 = "__NNAP_TRAINER_loss_fn_force__"
        , VAL_LOSS_FN_STRESS                = "__NNAP_TRAINER_loss_fn_stress__"
        , VAL_HAS_FORCE                     = "__NNAP_TRAINER_has_force__"
        , VAL_HAS_STRESS                    = "__NNAP_TRAINER_has_stress__"
        , VAL_FORCE_WEIGHT                  = "__NNAP_TRAINER_force_weight__"
        , VAL_STRESS_WEIGHT                 = "__NNAP_TRAINER_stress_weight__"
        , VAL_L2_LOSS_WEIGHT                = "__NNAP_TRAINER_l2_loss_wright__"
        , VAL_NORM_MU                       = "__NNAP_TRAINER_norm_mu__"
        , VAL_NORM_MU_J                     = "__NNAP_TRAINER_norm_mu_J__"
        , VAL_NORM_SIGMA                    = "__NNAP_TRAINER_norm_sigma__"
        , VAL_NORM_SIGMA_J                  = "__NNAP_TRAINER_norm_sigma_J__"
        , VAL_NORM_MU_ENG                   = "__NNAP_TRAINER_norm_mu_eng__"
        , VAL_NORM_SIGMA_ENG                = "__NNAP_TRAINER_norm_sigma_eng__"
        , VAL_TRAIN_DATA                    = "__NNAP_TRAINER_train_data__"
        , VAL_TEST_DATA                     = "__NNAP_TRAINER_test_data__"
        , VAL_DATA_J                        = "__NNAP_TRAINER_data_J__"
        , VAL_NTYPES                        = "__NNAP_TRAINER_ntypes__"
        , VAL_MIRROR_MAP                    = "__NNAP_TRAINER_mirror_map__"
        , VAL_HIDDEN_DIMS                   = "__NNAP_TRAINER_hidden_dims__"
        , VAL_INPUT_DIMS                    = "__NNAP_TRAINER_input_dims__"
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
        
        /** python 中对应的 DataSet 类，内部存储 torch 的 tensor，一般情况不需要重写 */
        public static String DATA_SET_SCRIPT =
            "class "+CLASS_DATA_SET+":\n" +
            "    def __init__(self):\n" +
            "        self.fp = []\n" +
            "        self.eng_indices = []\n" +
            "        self.fp_partial = ([], [])\n" +
            "        self.force_indices = None\n" +
            "        self.stress_indices = None\n" +
            "        self.stress_dxyz = None\n" +
            "        self.eng = None\n" +
            "        self.force = None\n" +
            "        self.stress = None\n" +
            "        self.atom_num = None\n" +
            "        self.volume = None";
        
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
            "    def __init__(self, input_dims, hidden_dims, mirror_map, ntypes):\n" +
            "        super().__init__()\n" +
            "        self.input_dims = input_dims\n" +
            "        self.ntypes = ntypes\n" +
            "        self.sub_models = torch.nn.ModuleList([(None if i in mirror_map else "+CLASS_SINGLE_MODEL+"(input_dims[i], hidden_dims[i])) for i in range(ntypes)])\n" +
            "        for k, v in mirror_map.items():\n" +
            "             self.sub_models[k] = self.sub_models[v]\n" +
            "    \n" +
            "    def forward(self, x):\n" +
            "        return [self.sub_models[i](x[i]) for i in range(self.ntypes)]\n" +
            "    \n" +
            "    def cal_l2_loss(self):\n" +
            "        l2_loss = 0.0\n" +
            "        nparams = 0\n" +
            "        for name, param in self.named_parameters():\n" +
            "            if 'weight' in name:\n" +
            "                param = param.flatten()\n" +
            "                l2_loss += param.dot(param)\n" +
            "                nparams += param.numel()\n" +
            "        l2_loss /= nparams\n" +
            "        return l2_loss\n" +
            "    \n" +
            "    def cal_eng(self, fp, indices, atom_nums):\n" +
            "        eng, _, _ = self.cal_eng_force_stress(fp, indices, atom_nums)\n" +
            "        return eng\n" +
            "    \n" +
            "    def cal_eng_force_stress(self, fp, indices, atom_nums, xyz_grad=None, indices_f=None, indices_s=None, dxyz=None, volumes=None, create_graph=False):\n" +
            "        eng_all = self.forward(fp)\n" +
            "        eng_len = len(atom_nums)\n" +
            "        eng = torch.zeros(eng_len, device=atom_nums.device, dtype=atom_nums.dtype)\n" +
            "        for i in range(self.ntypes):\n" +
            "            eng.scatter_add_(0, indices[i], eng_all[i])\n" +
            "        eng /= atom_nums\n" +
            "        if xyz_grad is None:\n" +
            "            return eng, None, None\n" +
            "        force, stress = None, None\n" +
            "        if indices_f is not None:\n" +
            "            force_len = atom_nums.sum().long().item()*3\n" + // 这里直接把力全部展开成单个向量
            "            force = torch.zeros(force_len, device=atom_nums.device, dtype=atom_nums.dtype)\n" +
            "        if indices_s is not None:\n" +
            "            stress_len = eng_len*6\n" + // 这里直接把应力全部展开成单个向量
            "            stress = torch.zeros(stress_len, device=atom_nums.device, dtype=atom_nums.dtype)\n" +
            "        for i in range(self.ntypes):\n" +
            "            fp_grad = torch.autograd.grad(eng_all[i].sum(), fp[i], create_graph=create_graph)[0]\n" +
            "            split_ = xyz_grad[0][i].shape[0]\n" +
            "            force_all = torch.bmm(xyz_grad[0][i], fp_grad[:split_, :].unsqueeze(-1))\n" +
            "            if indices_f is not None:\n" +
            "                force.scatter_add_(0, indices_f[0][i].flatten(), force_all.flatten())\n" +
            "            if indices_s is not None:\n" +
            "                stress_all = force_all * dxyz[0][i]\n" + // 利用 boardcast 的特性，自动计算交叉项
            "                stress.scatter_add_(0, indices_s[0][i].flatten(), stress_all.flatten())\n" +
            "            force_all = torch.bmm(xyz_grad[1][i], fp_grad[split_:, :].unsqueeze(-1))\n" +
            "            if indices_f is not None:\n" +
            "                force.scatter_add_(0, indices_f[1][i].flatten(), force_all.flatten())\n" +
            "            if indices_s is not None:\n" +
            "                stress_all = force_all * dxyz[1][i]\n" + // 利用 boardcast 的特性，自动计算交叉项
            "                stress.scatter_add_(0, indices_s[1][i].flatten(), stress_all.flatten())\n" +
            "        if indices_f is not None:\n" +
            "            force = -force\n" + // 这里统一补回负号，这样 stress 就不需要增加负号
            "        if indices_s is not None:\n" +
            "            stress = stress.reshape(eng_len, 6)\n" +
            "            stress /= volumes.unsqueeze(-1)\n" + // 利用 boardcast 的特性统一消去体积
            "            stress = stress.flatten()\n" +
            "        return eng, force, stress";
        
        /** 总 loss 函数，修改此值实现自定义 loss 函数，主要用于控制各种 loss 之间的比例 */
        public static String LOSS_FN_SCRIPT =
            "def "+VAL_LOSS_FN+"(pred_e, target_e, pred_f=None, target_f=None, pred_s=None, target_s=None, detail=False):\n" +
            "    loss_e = "+VAL_LOSS_FN_ENG+"(pred_e, target_e)\n" +
            "    loss_f = torch.tensor(0.0) if pred_f is None else "+VAL_LOSS_FN_FORCE+"(pred_f, target_f)\n" +
            "    loss_s = torch.tensor(0.0) if pred_s is None else "+VAL_LOSS_FN_STRESS+"(pred_s, target_s)\n" +
            "    if detail:\n" +
            "        return loss_e, "+VAL_FORCE_WEIGHT+"*loss_f, "+VAL_STRESS_WEIGHT+"*loss_s\n" +
            "    return loss_e + "+VAL_FORCE_WEIGHT+"*loss_f + "+VAL_STRESS_WEIGHT+"*loss_s";
        
        /** 用于计算能量的 loss 函数，修改此值实现自定义 loss 函数；目前默认采用 SmoothL1Loss */
        public static String LOSS_FN_ENG_SCRIPT = VAL_LOSS_FN_ENG+"=torch.nn.SmoothL1Loss()";
        /** 用于计算力的 loss 函数，修改此值实现自定义 loss 函数；目前默认采用 SmoothL1Loss */
        public static String LOSS_FN_FORCE_SCRIPT = VAL_LOSS_FN_FORCE+"=torch.nn.SmoothL1Loss()";
        /** 用于计算应力的 loss 函数，修改此值实现自定义 loss 函数；目前默认采用 SmoothL1Loss */
        public static String LOSS_FN_STRESS_SCRIPT = VAL_LOSS_FN_STRESS+"=torch.nn.SmoothL1Loss()";
        
        /** 训练一步的脚本函数，修改此值来实现自定义训练算法；默认使用 LBFGS 训练 */
        public static String TRAIN_STEP_SCRIPT =
            "def "+FN_TRAIN_STEP+"():\n" +
            "    data = "+VAL_TRAIN_DATA+"\n"+
            "    "+VAL_MODEL+".train()\n"+
            "    def closure():\n" +
            "        "+VAL_OPTIMIZER+".zero_grad()\n" +
            "        if not "+VAL_HAS_FORCE+" and not "+VAL_HAS_STRESS+":\n" +
            "            pred = "+VAL_MODEL+".cal_eng(data.fp, data.eng_indices, data.atom_num)\n" +
            "            loss = "+VAL_LOSS_FN+"(pred, data.eng)\n" +
            "        else:\n" +
            "            pred, pred_f, pred_s = "+VAL_MODEL+".cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume, create_graph=True)\n" +
            "            loss = "+VAL_LOSS_FN+"(pred, data.eng, pred_f, data.force, pred_s, data.stress)\n" +
            "        loss += "+VAL_L2_LOSS_WEIGHT+"*"+VAL_MODEL+".cal_l2_loss()\n" +
            "        loss.backward()\n" +
            "        return loss\n" +
            "    loss = closure()\n" +
            "    "+VAL_OPTIMIZER+".step(closure)\n" +
            "    return loss.item()";
        
        /** 获取测试集误差的脚本函数，一般情况不需要重写 */
        public static String TEST_LOSS_SCRIPT =
            "def "+FN_TEST_LOSS+"():\n" +
            "    data = "+VAL_TEST_DATA+"\n"+
            "    "+VAL_MODEL+".eval()\n"+
            "    if not "+VAL_HAS_FORCE+" and not "+VAL_HAS_STRESS+":\n" +
            "        with torch.no_grad():\n" +
            "            pred = "+VAL_MODEL+".cal_eng(data.fp, data.eng_indices, data.atom_num)\n" +
            "            loss = "+VAL_LOSS_FN+"(pred, data.eng)\n" +
            "            return loss.item()\n" +
            "    pred, pred_f, pred_s = "+VAL_MODEL+".cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume)\n" +
            "    loss = "+VAL_LOSS_FN+"(pred, data.eng, pred_f, data.force, pred_s, data.stress)\n" +
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
        SP.Python.exec(Conf.DATA_SET_SCRIPT);
        SP.Python.exec(Conf.SINGLE_MODEL_SCRIPT);
        SP.Python.exec(Conf.TOTAL_MODEL_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_ENG_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_FORCE_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_STRESS_SCRIPT);
        SP.Python.exec(Conf.LOSS_FN_SCRIPT);
        SP.Python.exec(Conf.TRAIN_STEP_SCRIPT);
        SP.Python.exec(Conf.TEST_LOSS_SCRIPT);
        //noinspection ConcatenationWithEmptyString
        SP.Python.exec("" +
        "def "+FN_CAL_MAE+"(data):\n" +
        "    "+VAL_MODEL+".eval()\n"+
        "    if not "+VAL_HAS_FORCE+" and not "+VAL_HAS_STRESS+":\n" +
        "        with torch.no_grad():\n" +
        "            pred = "+VAL_MODEL+".cal_eng(data.fp, data.eng_indices, data.atom_num)\n" +
        "            return (data.eng - pred).abs().mean().item()*"+VAL_NORM_SIGMA_ENG+", None, None\n" +
        "    pred, pred_f, pred_s = "+VAL_MODEL+".cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume)\n" +
        "    mae = (data.eng - pred).abs().mean().item()*"+VAL_NORM_SIGMA_ENG+"\n" +
        "    mae_f = None if pred_f is None else (data.force - pred_f).abs().mean().item()*"+VAL_NORM_SIGMA_ENG+"\n" +
        "    mae_s = None if pred_s is None else (data.stress - pred_s).abs().mean().item()*"+VAL_NORM_SIGMA_ENG+"\n" +
        "    return mae, mae_f, mae_s"
        );
        //noinspection ConcatenationWithEmptyString
        SP.Python.exec("" +
        "def "+FN_CAL_LOSS_DETAIL+"():\n" +
        "    data = "+VAL_TRAIN_DATA+"\n"+
        "    "+VAL_MODEL+".eval()\n"+
        "    loss_l2 = "+VAL_L2_LOSS_WEIGHT+"*"+VAL_MODEL+".cal_l2_loss()\n" +
        "    if not "+VAL_HAS_FORCE+" and not "+VAL_HAS_STRESS+":\n" +
        "        with torch.no_grad():\n" +
        "            pred = "+VAL_MODEL+".cal_eng(data.fp, data.eng_indices, data.atom_num)\n" +
        "            loss_e = "+VAL_LOSS_FN+"(pred, data.eng)\n" +
        "            return loss_l2.item(), loss_e.item(), None, None\n" +
        "    pred, pred_f, pred_s = "+VAL_MODEL+".cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume)\n" +
        "    loss_e, loss_f, loss_s = "+VAL_LOSS_FN+"(pred, data.eng, pred_f, data.force, pred_s, data.stress, detail=True)\n" +
        "    return loss_l2.item(), loss_e.item(), loss_f.item(), loss_s.item()"
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
    protected double mStressWeight = DEFAULT_STRESS_WEIGHT;
    public Trainer setStressWeight(double aWeight) {mStressWeight = aWeight; return this;}
    protected double mL2LossWeight = DEFAULT_L2_LOSS_WEIGHT;
    public Trainer setL2LossWeight(double aWeight) {mL2LossWeight = aWeight; return this;}
    protected boolean mClearDataOnTraining = DEFAULT_CLEAR_DATA_ON_TRAINING;
    public Trainer setClearDataOnTraining(boolean aFlag) {mClearDataOnTraining = aFlag; return this;}
    
    
    /** 所有训练相关的数据放在这里，同来减少训练集和测试集使用时的重复代码 */
    protected class DataSet {
        /** 按照种类排序，内部是可扩展的具体数据；现在使用这种 DoubleList 展开的形式存 */
        public final DoubleList[] mFp;
        /** mFp 的实际值（按行排列），这个只是缓存结果 */
        public final RowMatrix[] mFpMat;
        /** 每个 fp 对应的能量的索引，按照种类排序 */
        public final IntList[] mEngIndices;
        /** 按照种类排序，内部是每个原子的所有基组偏导值，每个基组偏导值按行排列，和交叉项以及 xyz 共同组成一个矩阵；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mFpPartial;
        /** 每个 fp partial 对应的力的索引，按照种类排序；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mForceIndices;
        /** 每个 fp partial 对应的应力的索引，按照种类排序；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mStressIndices;
        /** 每个 fp partial 对应的用于计算应力的 dxyz 值，按照种类排序；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mStressDxyz;
        /** 每个原子的近邻原子数列表 */
        public final IntList[] mNN;
        /** 每个原子数据结构对应的能量值 */
        public final DoubleList mEng = new DoubleList(64);
        /** 这里的力数值直接展开成单个向量，通过 mForceIndices 来获取对应的索引 */
        public final DoubleList mForce = new DoubleList(64);
        /** 这里的应力数值直接展开成单个向量，通过 mStressIndices 来获取对应的索引 */
        public final DoubleList mStress = new DoubleList(64);
        /** 每个原子数据结构对应的原子数 */
        public final DoubleList mAtomNum = new DoubleList(64);
        /** 每个原子数据结构对应的体积大小，目前主要用来计算应力 */
        public final DoubleList mVolume = new DoubleList(64);
        
        protected DataSet(int aAtomTypeNum) {
            mFp = new DoubleList[aAtomTypeNum];
            mEngIndices = new IntList[aAtomTypeNum];
            mFpPartial = new ArrayList<>(aAtomTypeNum);
            mForceIndices = new ArrayList<>(aAtomTypeNum);
            mStressIndices = new ArrayList<>(aAtomTypeNum);
            mStressDxyz = new ArrayList<>(aAtomTypeNum);
            mNN = new IntList[aAtomTypeNum];
            for (int i = 0; i < aAtomTypeNum; ++i) {
                mFp[i] = new DoubleList(64);
                mEngIndices[i] = new IntList(64);
                mFpPartial.add(new ArrayList<>(64));
                mForceIndices.add(new ArrayList<>(64));
                mStressIndices.add(new ArrayList<>(64));
                mStressDxyz.add(new ArrayList<>(64));
                mNN[i] = new IntList(64);
            }
            mFpMat = new RowMatrix[aAtomTypeNum];
        }
        protected DataSet() {this(mSymbols.length);}
        
        /** 初始化矩阵数据 */
        @ApiStatus.Internal
        protected void initFpMat_() {
            for (int i = 0; i < mFp.length; ++i) {
                final int tColNum = mBasis[i].size();
                final int tRowNum = mFp[i].size() / tColNum;
                mFpMat[i] = new RowMatrix(tRowNum, tColNum, mFp[i].internalData());
            }
        }
        @ApiStatus.Internal
        protected void putData2Py_(String aPyDataSetName, boolean aClearData) {
            SP.Python.setValue(VAL_DATA_J, this);
            String tDType = mTrainInFloat ? "float32" : "float64";
            try {
                SP.Python.exec(aPyDataSetName+" = "+CLASS_DATA_SET+"()");
                SP.Python.exec(aPyDataSetName+".eng = torch.tensor("+VAL_DATA_J+".mEng.asList(), dtype=torch."+tDType+")");
                SP.Python.exec(aPyDataSetName+".atom_num = torch.tensor("+VAL_DATA_J+".mAtomNum.asList(), dtype=torch."+tDType+")");
                for (int i = 0; i < mSymbols.length; ++i) {
                    SP.Python.setValue(VAL_I, i);
                    SP.Python.exec(aPyDataSetName+".eng_indices.append(torch.tensor("+VAL_DATA_J+".mEngIndices["+VAL_I+"].asList(), dtype=torch.int64))");
                    // 这里将最大的 Base 数据也用 numpy 的方式转换
                    NDArray<double[]> tBaseNP = new NDArray<>(mFpMat[i].internalData(), mFpMat[i].rowNumber(), mFpMat[i].columnNumber());
                    SP.Python.setValue(VAL_SUB, tBaseNP);
                    SP.Python.exec(aPyDataSetName+".fp.append(torch.from_numpy("+VAL_SUB+(mTrainInFloat?").float())":"))"));
                }
                SP.Python.removeValue(VAL_SUB);
                if (aClearData) {
                    for (int i = 0; i < mSymbols.length; ++i) {
                        mFp[i].clear();
                        mEngIndices[i].clear();
                        mFp[i].clear();
                        mFpMat[i] = null;
                    }
                    mEng.clear();
                    mAtomNum.clear();
                }
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                "for "+VAL_I+" in range(len("+aPyDataSetName+".fp)):\n" +
                "    "+aPyDataSetName+".fp["+VAL_I+"] -= "+VAL_NORM_MU+"["+VAL_I+"]\n" +
                "    "+aPyDataSetName+".fp["+VAL_I+"] /= "+VAL_NORM_SIGMA+"["+VAL_I+"]\n" +
                "del "+VAL_I
                );
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                aPyDataSetName+".eng /= "+aPyDataSetName+".atom_num\n"+
                aPyDataSetName+".eng -= "+VAL_NORM_MU_ENG+"\n"+
                aPyDataSetName+".eng /= "+VAL_NORM_SIGMA_ENG
                );
                // 训练力和应力需要的额外数据
                if (!mHasForce && !mHasStress) return;
                if (mHasForce) {
                    SP.Python.exec(aPyDataSetName+".force_indices = ([], [])");
                    SP.Python.exec(aPyDataSetName+".force = torch.tensor("+VAL_DATA_J+".mForce.asList(), dtype=torch."+tDType+")");
                    if (aClearData) {
                        mForce.clear();
                    }
                    SP.Python.exec(aPyDataSetName+".force /= "+VAL_NORM_SIGMA_ENG);
                }
                if (mHasStress) {
                    SP.Python.exec(aPyDataSetName+".stress_indices = ([], [])");
                    SP.Python.exec(aPyDataSetName+".stress_dxyz = ([], [])");
                    SP.Python.exec(aPyDataSetName+".stress = torch.tensor("+VAL_DATA_J+".mStress.asList(), dtype=torch."+tDType+")");
                    SP.Python.exec(aPyDataSetName+".volume = torch.tensor("+VAL_DATA_J+".mVolume.asList(), dtype=torch."+tDType+")");
                    if (aClearData) {
                        mStress.clear();
                        mVolume.clear();
                    }
                    SP.Python.exec(aPyDataSetName+".stress /= "+VAL_NORM_SIGMA_ENG);
                }
                for (int i = 0; i < mSymbols.length; ++i) {
                    // 先根据近邻数排序 FpPartial 和 Indices，拆分成两份来减少内存占用
                    final List<PyObject> tSubFpPartial = mFpPartial.get(i);
                    final List<PyObject> tSubForceIndices = mHasForce ? mForceIndices.get(i) : null;
                    final List<PyObject> tSubStressIndices = mHasStress ? mStressIndices.get(i) : null;
                    final List<PyObject> tSubStressDxyz = mHasStress ? mStressDxyz.get(i) : null;
                    IntVector tSubNN = mNN[i].asVec();
                    IntVector tSortIndices = Vectors.range(tSubNN.size());
                    tSubNN.operation().biSort((ii, jj) -> {
                        tSortIndices.swap(ii, jj);
                        Collections.swap(tSubFpPartial, ii, jj);
                        if (mHasForce) {
                            assert tSubForceIndices != null;
                            Collections.swap(tSubForceIndices, ii, jj);
                        }
                        if (mHasStress) {
                            assert tSubStressIndices != null;
                            Collections.swap(tSubStressIndices, ii, jj);
                            Collections.swap(tSubStressDxyz, ii, jj);
                        }
                    });
                    SP.Python.setValue(VAL_INDICES, tSortIndices.internalData());
                    SP.Python.setValue(VAL_S, calBestSplit_(tSubNN));
                    SP.Python.setValue(VAL_I, i);
                    // 基组本身也需要同步排序
                    SP.Python.exec(aPyDataSetName+".fp["+VAL_I+"] = "+aPyDataSetName+".fp["+VAL_I+"]["+VAL_INDICES+", :]");
                    SP.Python.exec(aPyDataSetName+".eng_indices["+VAL_I+"] = "+aPyDataSetName+".eng_indices["+VAL_I+"]["+VAL_INDICES+"]");
                    // 直接通过 torch.nn.utils.rnn.pad_sequence 来填充 0
                    SP.Python.exec(aPyDataSetName+".fp_partial[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mFpPartial["+VAL_I+"][:"+VAL_S+"], batch_first=True))");
                    SP.Python.exec(aPyDataSetName+".fp_partial[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mFpPartial["+VAL_I+"]["+VAL_S+":], batch_first=True))");
                    if (mHasForce) {
                        SP.Python.exec(aPyDataSetName+".force_indices[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mForceIndices["+VAL_I+"][:"+VAL_S+"], batch_first=True).long())");
                        SP.Python.exec(aPyDataSetName+".force_indices[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mForceIndices["+VAL_I+"]["+VAL_S+":], batch_first=True).long())");
                    }
                    if (mHasStress) {
                        SP.Python.exec(aPyDataSetName+".stress_indices[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mStressIndices["+VAL_I+"][:"+VAL_S+"], batch_first=True).long())");
                        SP.Python.exec(aPyDataSetName+".stress_indices[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mStressIndices["+VAL_I+"]["+VAL_S+":], batch_first=True).long())");
                        SP.Python.exec(aPyDataSetName+".stress_dxyz[0].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mStressDxyz["+VAL_I+"][:"+VAL_S+"], batch_first=True))");
                        SP.Python.exec(aPyDataSetName+".stress_dxyz[1].append(torch.nn.utils.rnn.pad_sequence("+VAL_DATA_J+".mStressDxyz["+VAL_I+"]["+VAL_S+":], batch_first=True))");
                    }
                    // 遍历过程中清空数据，进一步减少转换过程的内存占用峰值
                    if (aClearData) {
                        tSubFpPartial.forEach(PyObject::close);
                        tSubFpPartial.clear();
                        mNN[i].clear();
                        if (mHasForce) {
                            assert tSubForceIndices != null;
                            tSubForceIndices.forEach(PyObject::close);
                            tSubForceIndices.clear();
                        }
                        if (mHasStress) {
                            assert tSubStressIndices != null;
                            tSubStressIndices.forEach(PyObject::close);
                            tSubStressIndices.clear();
                            tSubStressDxyz.forEach(PyObject::close);
                            tSubStressDxyz.clear();
                        }
                    }
                }
                SP.Python.removeValue(VAL_INDICES);
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                "for "+VAL_I+" in range(len("+aPyDataSetName+".fp_partial[0])):\n" +
                "    "+aPyDataSetName+".fp_partial[0]["+VAL_I+"] /= "+VAL_NORM_SIGMA+"["+VAL_I+"]\n" +
                "    "+aPyDataSetName+".fp_partial[1]["+VAL_I+"] /= "+VAL_NORM_SIGMA+"["+VAL_I+"]\n" +
                "del "+VAL_I
                );
                // 此时基组值本身需要梯度
                //noinspection ConcatenationWithEmptyString
                SP.Python.exec("" +
                "for "+VAL_SUB+" in "+aPyDataSetName+".fp:\n" +
                "    "+VAL_SUB+".requires_grad_(True)\n" +
                "del "+VAL_SUB
                );
            } finally {
                SP.Python.removeValue(VAL_DATA_J);
            }
        }
    }
    
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final IBasis[] mBasis;
    protected final Vector[] mNormMu, mNormSigma;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 0.0;
    protected final DataSet mTrainData;
    protected final DataSet mTestData;
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
        mNormMu = new Vector[mSymbols.length];
        mNormSigma = new Vector[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            int tSize = mBasis[i].size();
            mNormMu[i] = Vector.zeros(tSize);
            mNormSigma[i] = Vector.zeros(tSize);
        }
        mTrainData = new DataSet();
        mTestData = new DataSet();
        mModelSetting = aModelSetting;
        // 简单遍历 basis 处理 mirror 的情况
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[i] instanceof Mirror) {
            Mirror tBasis = (Mirror)mBasis[i];
            IBasis tMirrorBasis = tBasis.mirrorBasis();
            int tMirrorType = tBasis.mirrorType();
            if ((mBasis[tMirrorType-1]!=tMirrorBasis) || (tBasis.thisType()!=(i+1))) {
                throw new IllegalArgumentException("Mirror Basis mismatch for type: "+(i+1));
            }
            double oRefEng = mRefEngs.get(i);
            double tRefEng = mRefEngs.get(tMirrorType-1);
            if (!Double.isNaN(oRefEng) && !MathEX.Code.numericEqual(oRefEng, tRefEng)) {
                UT.Code.warning("RefEng of mirror mismatch for type: "+(i+1)+", overwrite with mirror values automatically");
            }
            mRefEngs.set(i, tRefEng);
        }
    }
    public Trainer(String[] aSymbols, IVector aRefEngs, IBasis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    public Trainer(String[] aSymbols, double[] aRefEngs, IBasis[] aBasis, Map<String, ?> aModelSetting) {this(aSymbols, Vectors.from(aRefEngs), aBasis, aModelSetting);}
    public Trainer(String[] aSymbols, double[] aRefEngs, IBasis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    private static IBasis[] repeatBasis_(IBasis aBasis, int aLen) {
        IBasis[] rOut = new IBasis[aLen];
        Arrays.fill(rOut, aBasis);
        return rOut;
    }
    
    @Override public int atomTypeNumber() {return mSymbols.length;}
    public PyObject model(int aType) {return SP.Python.getAs(PyObject.class, VAL_MODEL+".sub_models["+(aType-1)+"]");}
    @SuppressWarnings("unchecked")
    public @Unmodifiable List<PyObject> models() {return (List<PyObject>)SP.Python.getAs(List.class, VAL_MODEL+".sub_models");}
    public IBasis basis(int aType) {return mBasis[aType-1];}
    public @Unmodifiable List<IBasis> basis() {return AbstractCollections.from(mBasis);}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public String units() {return mUnits;}
    
    /** 重写实现自定义模型创建 */
    protected void initModel() {
        @Nullable List<?> tHiddenDims = (List<?>)UT.Code.get(mModelSetting, "hidden_dims", "nnarch");
        if (tHiddenDims == null) {
            tHiddenDims = NewCollections.from(mSymbols.length, i -> DEFAULT_HIDDEN_DIMS);
        }
        if (tHiddenDims.get(0) instanceof Integer) {
            final List<?> tSubDims = tHiddenDims;
            tHiddenDims = NewCollections.from(mSymbols.length, i -> tSubDims);
        }
        Map<Integer, Integer> tMirrorMap = new HashMap<>();
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[i] instanceof Mirror) {
            int tMirror = ((Mirror)mBasis[i]).mirrorType()-1;
            tMirrorMap.put(i, tMirror);
            Object oSubDims = tHiddenDims.get(i);
            Object tSubDims = tHiddenDims.get(tMirror);
            if (oSubDims!=null && !oSubDims.equals(tSubDims)) {
                UT.Code.warning("hidden_dims of mirror mismatch for type: "+(i+1)+", overwrite with mirror values automatically");
            }
        }
        SP.Python.setValue(VAL_INPUT_DIMS, NewCollections.map(mBasis, IBasis::size));
        SP.Python.setValue(VAL_HIDDEN_DIMS, tHiddenDims);
        SP.Python.setValue(VAL_MIRROR_MAP, tMirrorMap);
        SP.Python.setValue(VAL_NTYPES, mSymbols.length);
        try {
            SP.Python.exec(VAL_MODEL+" = "+CLASS_TOTAL_MODEL+"("+VAL_INPUT_DIMS+", "+VAL_HIDDEN_DIMS+", "+VAL_MIRROR_MAP+", ntypes="+VAL_NTYPES+")");
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
    /** 重写实现自定义基组归一化方案 */
    protected void initNormBasis() {
        for (int i = 0; i < mSymbols.length; ++i) {
            mNormMu[i].fill(0.0);
            mNormSigma[i].fill(0.0);
        }
        IVector tDiv = VectorCache.getZeros(mSymbols.length);
        for (int i = 0; i < mSymbols.length; ++i) {
            // 这里需要考虑 mirror 的情况，对于 mirror 的同时和对应的数据一起公用归一化向量
            int j = i;
            if (mBasis[i] instanceof Mirror) {
                j = ((Mirror)mBasis[i]).mirrorType()-1;
            }
            for (IVector tRow : mTrainData.mFpMat[i].rows()) {
                mNormMu[j].plus2this(tRow);
                mNormSigma[j].operation().operate2this(tRow, (lhs, rhs) -> lhs + rhs*rhs);
            }
            tDiv.add(j, mTrainData.mFpMat[i].rowNumber());
        }
        for (int i = 0; i < mSymbols.length; ++i) if (!(mBasis[i] instanceof Mirror)) {
            mNormMu[i].div2this(tDiv.get(i));
            mNormSigma[i].div2this(tDiv.get(i));
            mNormSigma[i].operation().operate2this(mNormMu[i], (lhs, rhs) -> lhs - rhs*rhs);
            mNormSigma[i].operation().map2this(MathEX.Fast::sqrt);
        }
        VectorCache.returnVec(tDiv);
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[i] instanceof Mirror) {
            int tMirrorIdx = ((Mirror)mBasis[i]).mirrorType()-1;
            mNormMu[i] = mNormMu[tMirrorIdx];
            mNormSigma[i] = mNormSigma[tMirrorIdx];
        }
    }
    /** 重写实现自定义能量归一化方案 */
    protected void initNormEng() {
        // 这里采用中位数和上下四分位数来归一化能量
        Vector tSortedEng = mTrainData.mEng.copy2vec();
        tSortedEng.div2this(mTrainData.mAtomNum.asVec());
        tSortedEng.sort();
        int tSize = tSortedEng.size();
        int tSize2 = tSize/2;
        mNormMuEng = tSortedEng.get(tSize2);
        if ((tSize&1)==1) {
            mNormMuEng = (mNormMuEng + tSortedEng.get(tSize2+1))*0.5;
        }
        int tSize4 = tSize2/2;
        double tEng14 = tSortedEng.get(tSize4);
        double tEng14R = tSortedEng.get(tSize4+1);
        int tSize34 = tSize2+tSize4;
        if ((tSize&1)==1) ++tSize34;
        double tEng34 = tSortedEng.get(tSize34);
        double tEng34R = tSortedEng.get(tSize34+1);
        if ((tSize&1)==1) {
            if ((tSize2&1)==1) {
                tEng14 = (tEng14 + 3*tEng14R)*0.25;
                tEng34 = (3*tEng34 + tEng34R)*0.25;
            } else {
                tEng14 = (3*tEng14 + tEng14R)*0.25;
                tEng34 = (tEng34 + 3*tEng34R)*0.25;
            }
        } else {
            if ((tSize2&1)==1) {
                tEng14 = (tEng14 + tEng14R)*0.5;
                tEng34 = (tEng34 + tEng34R)*0.5;
            }
        }
        mNormSigmaEng = tEng34 - tEng14;
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
        mTrainData.initFpMat_();
        if (mHasTest) mTestData.initFpMat_();
        // 重新构建归一化参数
        initNormBasis();
        initNormEng();
        // 构造 torch 数据，这里直接把数据放到 python 环境中变成一个 python 的全局变量
        SP.Python.setValue(VAL_HAS_FORCE, mHasForce);
        SP.Python.setValue(VAL_HAS_STRESS, mHasStress);
        SP.Python.setValue(VAL_FORCE_WEIGHT, mForceWeight);
        SP.Python.setValue(VAL_STRESS_WEIGHT, mStressWeight);
        SP.Python.setValue(VAL_L2_LOSS_WEIGHT, mL2LossWeight);
        SP.Python.setValue(VAL_NORM_MU_J, mNormMu);
        SP.Python.setValue(VAL_NORM_SIGMA_J, mNormSigma);
        SP.Python.exec(VAL_NORM_MU+" = [torch.tensor("+VAL_SUB+".asList(), dtype=torch."+(mTrainInFloat?"float32":"float64")+") for "+VAL_SUB+" in "+VAL_NORM_MU_J+"]");
        SP.Python.exec(VAL_NORM_SIGMA+" = [torch.tensor("+VAL_SUB+".asList(), dtype=torch."+(mTrainInFloat?"float32":"float64")+") for "+VAL_SUB+" in "+VAL_NORM_SIGMA_J+"]");
        SP.Python.removeValue(VAL_NORM_MU_J);
        SP.Python.removeValue(VAL_NORM_SIGMA_J);
        SP.Python.setValue(VAL_NORM_MU_ENG, mNormMuEng);
        SP.Python.setValue(VAL_NORM_SIGMA_ENG, mNormSigmaEng);
        mTrainData.putData2Py_(VAL_TRAIN_DATA, mClearDataOnTraining);
        if (mHasTest) mTestData.putData2Py_(VAL_TEST_DATA, mClearDataOnTraining);
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
                if (aPrintLog) System.out.printf("Model at epoch = %d selected, test loss = %.4g\n", tSelectEpoch, tMinLoss);
            }
            if (!aPrintLog) return;
            List<?> tLossDetail = (List<?>)SP.Python.eval(FN_CAL_LOSS_DETAIL+"()");
            double tLossL2 = ((Number)tLossDetail.get(0)).doubleValue();
            double tLossE = ((Number)tLossDetail.get(1)).doubleValue();
            double tLossF = mHasForce ? ((Number)tLossDetail.get(2)).doubleValue() : 0.0;
            double tLossS = mHasStress ? ((Number)tLossDetail.get(3)).doubleValue() : 0.0;
            double tLossTot = tLossL2+tLossE+tLossF+tLossS;
            System.out.printf("Loss-L2: %.4g (%s)\n", tLossL2, IO.Text.percent(tLossL2/tLossTot));
            System.out.printf("Loss-E : %.4g (%s)\n", tLossE, IO.Text.percent(tLossE/tLossTot));
            if (mHasForce) {
                System.out.printf("Loss-F : %.4g (%s)\n", tLossF, IO.Text.percent(tLossF/tLossTot));
            }
            if (mHasStress) {
                System.out.printf("Loss-S : %.4g (%s)\n", tLossS, IO.Text.percent(tLossS/tLossTot));
            }
            List<?> tMAE = (List<?>)SP.Python.eval(FN_CAL_MAE+"("+VAL_TRAIN_DATA+")");
            double tMAE_E = ((Number)tMAE.get(0)).doubleValue();
            double tMAE_F = mHasForce ? ((Number)tMAE.get(1)).doubleValue() : Double.NaN;
            double tMAE_S = mHasStress ? ((Number)tMAE.get(2)).doubleValue() : Double.NaN;
            if (!mHasTest) {
                System.out.printf("MAE-E: %.4g meV\n", tMAE_E*1000);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g meV/A\n", tMAE_F*1000);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g meV/A^3\n", tMAE_S*1000);
                }
                return;
            }
            List<?> tTestMAE = (List<?>)SP.Python.eval(FN_CAL_MAE+"("+VAL_TEST_DATA+")");
            double tTestMAE_E = ((Number)tTestMAE.get(0)).doubleValue();
            double tTestMAE_F = mHasForce ? ((Number)tTestMAE.get(1)).doubleValue() : Double.NaN;
            double tTestMAE_S = mHasStress ? ((Number)tTestMAE.get(2)).doubleValue() : Double.NaN;
            switch(mUnits) {
            case "metal": {
                System.out.printf("MAE-E: %.4g meV | %.4g meV\n", tMAE_E*1000, tTestMAE_E*1000);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g meV/A | %.4g meV/A\n", tMAE_F*1000, tTestMAE_F*1000);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g meV/A^3 | %.4g meV/A^3\n", tMAE_S*1000, tTestMAE_S*1000);
                }
                break;
            }
            case "real":{
                System.out.printf("MAE-E: %.4g kcal/mol | %.4g kcal/mol\n", tMAE_E, tTestMAE_E);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g kcal/mol/A | %.4g kcal/mol/A\n", tMAE_F, tTestMAE_F);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g kcal/mol/A^3 | %.4g kcal/mol/A^3\n", tMAE_S, tTestMAE_S);
                }
                break;
            }
            default: {
                System.out.printf("MAE-E: %.4g | %.4g\n", tMAE_E, tTestMAE_E);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g | %.4g\n", tMAE_F, tTestMAE_F);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g | %.4g\n", tMAE_S, tTestMAE_S);
                }
                break;
            }}

        } finally {
            // 完事移除临时数据
            SP.Python.removeValue(VAL_NORM_MU);
            SP.Python.removeValue(VAL_NORM_SIGMA);
            SP.Python.removeValue(VAL_NORM_MU_ENG);
            SP.Python.removeValue(VAL_NORM_SIGMA_ENG);
            SP.Python.removeValue(VAL_TRAIN_DATA);
            if (mHasTest) {
                SP.Python.removeValue(VAL_TEST_DATA);
            }
        }
    }
    public void train(int aEpochs, boolean aEarlyStop) {train(aEpochs, aEarlyStop, true);}
    public void train(int aEpochs) {train(aEpochs, true);}
    
    @ApiStatus.Internal
    protected void calRefEngFpAndAdd_(IAtomData aAtomData, double aEnergy, DataSet rData) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                IBasis tBasis = basis(tType);
                Vector tFp = tBasis.eval(tAPC, i, tTypeMap);
                rData.mFp[tType-1].addAll(tFp);
                rData.mEngIndices[tType-1].add(rData.mEng.size());
                VectorCache.returnVec(tFp);
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
            }
        }
        // 这里后添加能量，这样 rData.mEng.size() 对应正确的索引
        rData.mEng.add(aEnergy);
    }
    @ApiStatus.Internal
    protected void calRefEngFpPartialAndAdd_(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress, DataSet rData) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                IBasis tBasis = basis(tType);
                List<Vector> tOut = tBasis.evalPartial(true, tAPC, i, tTypeMap);
                // 基组和索引
                rData.mFp[tType-1].addAll(tOut.get(0));
                rData.mEngIndices[tType-1].add(rData.mEng.size());
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
                // 基组偏导和索引
                int tRowNum = tOut.size()-1;
                final RowMatrix tFpPartial = MatrixCache.getMatRow(tRowNum, tBasis.size());
                tFpPartial.row(0).fill(tOut.get(1));
                tFpPartial.row(1).fill(tOut.get(2));
                tFpPartial.row(2).fill(tOut.get(3));
                final IntVector tForceIndices = mHasForce ? IntVectorCache.getVec(tRowNum) : null;
                final int tShiftF = mHasForce ? rData.mForce.size() : -1;
                if (mHasForce) {
                    tForceIndices.set(0, tShiftF + 3*i);
                    tForceIndices.set(1, tShiftF + 3*i + 1);
                    tForceIndices.set(2, tShiftF + 3*i + 2);
                }
                final RowIntMatrix tStressIndices = mHasStress ? IntMatrixCache.getMatRow(tRowNum, 2) : null;
                final RowMatrix tStressDxyz = mHasStress ? MatrixCache.getMatRow(tRowNum, 2) : null;
                final int tShiftS = mHasStress ? rData.mStress.size() : -1;
                if (mHasStress) {
                    // 按照目前展开约定，会是这个顺序
                    tStressIndices.set(0, 0, tShiftS);   tStressIndices.set(0, 1, tShiftS+3);
                    tStressIndices.set(1, 0, tShiftS+1); tStressIndices.set(1, 1, tShiftS+5);
                    tStressIndices.set(2, 0, tShiftS+2); tStressIndices.set(2, 1, tShiftS+4);
                    // 第二列用来计算交叉项，原子自身力不贡献应力
                    tStressDxyz.set(0, 0, 0.0); tStressDxyz.set(0, 1, 0.0);
                    tStressDxyz.set(1, 0, 0.0); tStressDxyz.set(1, 1, 0.0);
                    tStressDxyz.set(2, 0, 0.0); tStressDxyz.set(2, 1, 0.0);
                }
                final int tNN = (tOut.size()-4)/3;
                final int[] j = {0};
                tAPC.nl_().forEachNeighbor(i, tBasis.rcut(), (dx, dy, dz, idx) -> {
                    tFpPartial.row(3 + 3*j[0]).fill(tOut.get(4+j[0]));
                    tFpPartial.row(4 + 3*j[0]).fill(tOut.get(4+tNN+j[0]));
                    tFpPartial.row(5 + 3*j[0]).fill(tOut.get(4+tNN+tNN+j[0]));
                    if (mHasForce) {
                        assert tForceIndices != null;
                        tForceIndices.set(3 + 3*j[0], tShiftF + 3*idx);
                        tForceIndices.set(4 + 3*j[0], tShiftF + 3*idx + 1);
                        tForceIndices.set(5 + 3*j[0], tShiftF + 3*idx + 2);
                    }
                    if (mHasStress) {
                        assert tStressIndices != null;
                        // 按照目前展开约定，会是这个顺序
                        tStressIndices.set(3 + 3*j[0], 0, tShiftS);   tStressIndices.set(3 + 3*j[0], 1, tShiftS+3);
                        tStressIndices.set(4 + 3*j[0], 0, tShiftS+1); tStressIndices.set(4 + 3*j[0], 1, tShiftS+5);
                        tStressIndices.set(5 + 3*j[0], 0, tShiftS+2); tStressIndices.set(5 + 3*j[0], 1, tShiftS+4);
                        assert tStressDxyz != null;
                        // 第二列用来计算交叉项
                        tStressDxyz.set(3 + 3*j[0], 0, dx); tStressDxyz.set(3 + 3*j[0], 1, dy);
                        tStressDxyz.set(4 + 3*j[0], 0, dy); tStressDxyz.set(4 + 3*j[0], 1, dz);
                        tStressDxyz.set(5 + 3*j[0], 0, dz); tStressDxyz.set(5 + 3*j[0], 1, dx);
                    }
                    ++j[0];
                });
                VectorCache.returnVec(tOut);
                // 将数据转换为 torch 的 tensor，这里最快的方式是利用 torch 的 from_numpy 进行转换
                PyObject tPyFpPartial, tPyForceIndices=null, tPyStressIndices=null, tPyStressDxyz=null;
                try (PyCallable tFromNumpy = TORCH.getAttr("from_numpy", PyCallable.class)) {
                    tPyFpPartial = tFromNumpy.callAs(PyObject.class, new NDArray<>(tFpPartial.internalData(), tFpPartial.rowNumber(), tFpPartial.columnNumber()));
                    if (mTrainInFloat) {
                        try (PyObject oPyFpPartial = tPyFpPartial; PyCallable tFloat = oPyFpPartial.getAttr("float", PyCallable.class)) {
                            tPyFpPartial = tFloat.callAs(PyObject.class);
                        }
                    }
                    if (mHasForce) {
                        assert tForceIndices != null;
                        tPyForceIndices = tFromNumpy.callAs(PyObject.class, new NDArray<>(tForceIndices.internalData(), tForceIndices.size()));
                    }
                    if (mHasStress) {
                        assert tStressIndices != null;
                        tPyStressIndices = tFromNumpy.callAs(PyObject.class, new NDArray<>(tStressIndices.internalData(), tStressIndices.rowNumber(), tStressIndices.columnNumber()));
                        assert tStressDxyz != null;
                        tPyStressDxyz = tFromNumpy.callAs(PyObject.class, new NDArray<>(tStressDxyz.internalData(), tStressDxyz.rowNumber(), tStressDxyz.columnNumber()));
                        if (mTrainInFloat) {
                            try (PyObject oPyStressDxyz = tPyStressDxyz; PyCallable tFloat = oPyStressDxyz.getAttr("float", PyCallable.class)) {
                                tPyStressDxyz = tFloat.callAs(PyObject.class);
                            }
                        }
                    }
                }
                rData.mFpPartial.get(tType-1).add(tPyFpPartial);
                rData.mNN[tType-1].add(tNN);
                MatrixCache.returnMat(tFpPartial);
                if (mHasForce) {
                    assert tForceIndices != null;
                    rData.mForceIndices.get(tType-1).add(tPyForceIndices);
                    IntVectorCache.returnVec(tForceIndices);
                }
                if (mHasStress) {
                    assert tStressIndices != null;
                    rData.mStressIndices.get(tType-1).add(tPyStressIndices);
                    IntMatrixCache.returnMat(tStressIndices);
                    assert tStressDxyz != null;
                    rData.mStressDxyz.get(tType-1).add(tPyStressDxyz);
                    MatrixCache.returnMat(tStressDxyz);
                }
            }
        }
        // 这里后添加能量，这样 rData.mEng.size() 对应正确的索引
        rData.mEng.add(aEnergy);
        // 这里后添加力，这样 rData.mForce.size() 对应正确的索引
        if (mHasForce) {
            assert aForces != null;
            rData.mForce.addAll(aForces.asVecRow());
        }
        // 这里后添加应力应力，这样 rData.mStress.size() 对应正确的索引
        if (mHasStress) {
            assert aStress != null;
            rData.mStress.addAll(aStress);
        }
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
        if (mHasForce || mHasStress) {
            calRefEngFpPartialAndAdd_(aAtomData, aEnergy, aForces, aStress, mTrainData);
        } else {
            calRefEngFpAndAdd_(aAtomData, aEnergy, mTrainData);
        }
        mTrainData.mAtomNum.add(aAtomData.atomNumber());
        mTrainData.mVolume.add(aAtomData.volume());
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
        if (mHasForce || mHasStress) {
            calRefEngFpPartialAndAdd_(aAtomData, aEnergy, aForces, aStress, mTestData);
        } else {
            calRefEngFpAndAdd_(aAtomData, aEnergy, mTestData);
        }
        mTestData.mAtomNum.add(aAtomData.atomNumber());
        mTestData.mVolume.add(aAtomData.volume());
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
            if (mBasis[i] instanceof Mirror) {
                rModels.add(Maps.of(
                    "symbol", mSymbols[i],
                    "basis", rBasis
                ));
                continue;
            }
            byte[] tModelByes = (byte[])SP.Python.invoke(FN_MODEL2BYTES, i);
            rModels.add(Maps.of(
                "symbol", mSymbols[i],
                "basis", rBasis,
                "ref_eng", mRefEngs.get(i),
                "norm_mu", mNormMu[i].asList(),
                "norm_sigma", mNormSigma[i].asList(),
                "norm_mu_eng", mNormMuEng,
                "norm_sigma_eng", mNormSigmaEng,
                "torch", Base64.getEncoder().encodeToString(tModelByes)
            ));
        }
        rSaveTo.put("version", NNAP.VERSION);
        rSaveTo.put("units", mUnits);
        rSaveTo.put("models", rModels);
    }
    @SuppressWarnings({"rawtypes"})
    public void save(String aPath, boolean aPretty) throws IOException {
        Map rJson = new LinkedHashMap();
        save(rJson);
        IO.map2json(rJson, aPath, aPretty);
    }
    public void save(String aPath) throws IOException {save(aPath, false);}
}
