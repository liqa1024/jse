import copy
import io
from typing import Optional

import torch

class SingleModel(torch.nn.Module):
    def __init__(self, input_dim, hidden_dims):
        super().__init__()
        self.in_layer = torch.nn.Linear(input_dim, hidden_dims[0])
        self.hidden_layers = torch.nn.ModuleList([torch.nn.Linear(hidden_dims[i], hidden_dims[i+1]) for i in range(len(hidden_dims)-1)])
        self.out_layer = torch.nn.Linear(hidden_dims[-1], 1)
        self.active = torch.nn.SiLU()
    
    def forward(self, x):
        x = self.active(self.in_layer(x))
        for hidden_layer in self.hidden_layers:
            x = self.active(hidden_layer(x))
        x = self.out_layer(x)
        return torch.reshape(x, x.shape[:-1])


class TotalModel(torch.nn.Module):
    def __init__(self, input_dims, hidden_dims, mirror_map, ntypes):
        super().__init__()
        self.input_dims = input_dims
        self.ntypes = ntypes
        self.sub_models = torch.nn.ModuleList([(None if i in mirror_map else SingleModel(input_dims[i], hidden_dims[i])) for i in range(ntypes)])
        for k, v in mirror_map.items():
             self.sub_models[k] = self.sub_models[v]
    
    def forward(self, x):
        return [self.sub_models[i](x[i]) for i in range(self.ntypes)]
    
    def cal_l2_loss(self):
        l2_loss = 0.0
        nparams = 0
        for name, param in self.named_parameters():
            if 'weight' in name:
                param = param.flatten()
                l2_loss += param.dot(param)
                nparams += param.numel()
        l2_loss /= nparams
        return l2_loss
    
    def cal_eng(self, fp, indices, atom_nums):
        eng, _, _ = self.cal_eng_force_stress(fp, indices, atom_nums)
        return eng
    
    def cal_eng_force_stress(self, fp, indices, atom_nums, xyz_grad=None, indices_f=None, indices_s=None, dxyz=None, volumes=None, create_graph=False):
        eng_all = self.forward(fp)
        eng_len = len(atom_nums)
        eng = torch.zeros(eng_len, device=atom_nums.device, dtype=atom_nums.dtype)
        for i in range(self.ntypes):
            eng.scatter_add_(0, indices[i], eng_all[i])
        eng /= atom_nums
        if xyz_grad is None:
            return eng, None, None
        force, stress = None, None
        if indices_f is not None:
            force_len = atom_nums.sum().long().item()*3
            force = torch.zeros(force_len, device=atom_nums.device, dtype=atom_nums.dtype)
        if indices_s is not None:
            stress_len = eng_len*6
            stress = torch.zeros(stress_len, device=atom_nums.device, dtype=atom_nums.dtype)
        for i in range(self.ntypes):
            fp_grad = torch.autograd.grad(eng_all[i].sum(), fp[i], create_graph=create_graph)[0]
            split_ = xyz_grad[0][i].shape[0]
            force_all = torch.bmm(xyz_grad[0][i], fp_grad[:split_, :].unsqueeze(-1))
            if indices_f is not None:
                force.scatter_add_(0, indices_f[0][i].flatten(), force_all.flatten())
            if indices_s is not None:
                stress_all = force_all * dxyz[0][i]
                stress.scatter_add_(0, indices_s[0][i].flatten(), stress_all.flatten())
            force_all = torch.bmm(xyz_grad[1][i], fp_grad[split_:, :].unsqueeze(-1))
            if indices_f is not None:
                force.scatter_add_(0, indices_f[1][i].flatten(), force_all.flatten())
            if indices_s is not None:
                stress_all = force_all * dxyz[1][i]
                stress.scatter_add_(0, indices_s[1][i].flatten(), stress_all.flatten())
        if indices_s is not None:
            stress = stress.reshape(eng_len, 6)
            stress /= volumes.unsqueeze(-1)
            stress = stress.flatten()
        return eng, force, stress


class DataSet:
    def __init__(self, trainer):
        self.trainer = trainer
        self.fp = []
        self.eng_indices = []
        self.fp_partial = ([], [])
        self.force_indices = None
        self.stress_indices = None
        self.stress_dxyz = None
        self.eng = None
        self.force = None
        self.stress = None
        self.atom_num = None
        self.volume = None
    
    
    def init_eng_part(self, data_j):
        if self.trainer.train_in_float:
            dtype_ = torch.float32
        else:
            dtype_ = torch.float64
        
        self.eng = torch.tensor(data_j.mEng.asList(), dtype=dtype_)
        self.atom_num = torch.tensor(data_j.mAtomNum.asList(), dtype=dtype_)
        
        ntypes = data_j.mAtomTypeNumber
        for i in range(ntypes):
            self.eng_indices.append(torch.tensor(data_j.mEngIndices[i].asList(), dtype=torch.int64))
            # use numpy to speed up
            fp_ = torch.from_numpy(data_j.mFpMat[i].numpy())
            if self.trainer.train_in_float:
                fp_ = fp_.float()
            self.fp.append(fp_)
            
        for i in range(ntypes):
            self.fp[i] -= self.trainer.norm_mu[i]
            self.fp[i] /= self.trainer.norm_sigma[i]
        
        self.eng /= self.atom_num
        self.eng -= self.trainer.norm_mu_eng
        self.eng /= self.trainer.norm_sigma_eng
    
    
    def init_force_part1(self, data_j):
        if self.trainer.train_in_float:
            dtype_ = torch.float32
        else:
            dtype_ = torch.float64
        
        if self.trainer.has_force:
            self.force_indices = ([], [])
            self.force = torch.tensor(data_j.mForce.asList(), dtype=dtype_)
            self.force /= self.trainer.norm_sigma_eng
        
        if self.trainer.has_stress:
            self.stress_indices = ([], [])
            self.stress_dxyz = ([], [])
            self.stress = torch.tensor(data_j.mStress.asList(), dtype=dtype_)
            self.volume = torch.tensor(data_j.mVolume.asList(), dtype=dtype_)
            self.stress /= self.trainer.norm_sigma_eng
            self.stress = -self.stress # negative here
    
    
    def init_force_part2(self, data_j, i: int, sort_indices, split_: int):
        self.fp[i] = self.fp[i][sort_indices, :]
        self.eng_indices[i] = self.eng_indices[i][sort_indices]
        
        self.fp_partial[0].append(torch.nn.utils.rnn.pad_sequence(data_j.mFpPartial[i][:split_], batch_first=True))
        self.fp_partial[1].append(torch.nn.utils.rnn.pad_sequence(data_j.mFpPartial[i][split_:], batch_first=True))
        
        if self.trainer.has_force:
            self.force_indices[0].append(torch.nn.utils.rnn.pad_sequence(data_j.mForceIndices[i][:split_], batch_first=True).long())
            self.force_indices[1].append(torch.nn.utils.rnn.pad_sequence(data_j.mForceIndices[i][split_:], batch_first=True).long())
        
        if self.trainer.has_stress:
            self.stress_indices[0].append(torch.nn.utils.rnn.pad_sequence(data_j.mStressIndices[i][:split_], batch_first=True).long())
            self.stress_indices[1].append(torch.nn.utils.rnn.pad_sequence(data_j.mStressIndices[i][split_:], batch_first=True).long())
            self.stress_dxyz[0].append(torch.nn.utils.rnn.pad_sequence(data_j.mStressDxyz[i][:split_], batch_first=True))
            self.stress_dxyz[1].append(torch.nn.utils.rnn.pad_sequence(data_j.mStressDxyz[i][split_:], batch_first=True))
    
    
    def init_force_part3(self, data_j):
        for i in range(data_j.mAtomTypeNumber):
            self.fp_partial[0][i] /= self.trainer.norm_sigma[i]
            self.fp_partial[1][i] /= self.trainer.norm_sigma[i]
        
        for sub_fp in self.fp:
            sub_fp.requires_grad_(True)


class Trainer:
    def __init__(self, force_weight: float, stress_weight: float, l2_loss_weight: float, train_in_float: bool):
        
        self.force_weight = force_weight
        self.stress_weight = stress_weight
        self.l2_loss_weight = l2_loss_weight
        self.train_in_float = train_in_float
        
        self.model: Optional[TotalModel] = None
        self.model_state_dict = None
        self.train_data = DataSet(self)
        self.test_data = DataSet(self)
        
        self.has_force = False
        self.has_stress = False
        
        self.norm_mu = []
        self.norm_sigma = []
        self.norm_mu_eng = 0.0
        self.norm_sigma_eng = 1.0
        
        self.loss_fn_eng = torch.nn.SmoothL1Loss()
        self.loss_fn_force = torch.nn.SmoothL1Loss()
        self.loss_fn_stress = torch.nn.SmoothL1Loss()
        
        self.optimizer: Optional[torch.optim.Optimizer] = None
        
        
    def init_model(self, input_dims, hidden_dims, mirror_map, ntypes):
        self.model = TotalModel(input_dims, hidden_dims, mirror_map, ntypes)
        if not self.train_in_float:
            self.model = self.model.double()
        self.optimizer = torch.optim.LBFGS(self.model.parameters(), history_size=100, max_iter=5, line_search_fn='strong_wolfe')
    
    def init_norm(self, norm_mu_j, norm_sigma_j):
        if self.train_in_float:
            self.norm_mu = [torch.tensor(sub_vec.asList(), dtype=torch.float32) for sub_vec in norm_mu_j]
            self.norm_sigma = [torch.tensor(sub_vec.asList(), dtype=torch.float32) for sub_vec in norm_sigma_j]
        else:
            self.norm_mu = [torch.tensor(sub_vec.asList(), dtype=torch.float64) for sub_vec in norm_mu_j]
            self.norm_sigma = [torch.tensor(sub_vec.asList(), dtype=torch.float64) for sub_vec in norm_sigma_j]
    
    
    def loss_fn(self, pred_e, target_e, pred_f=None, target_f=None, pred_s=None, target_s=None, detail=False):
        loss_e = self.loss_fn_eng(pred_e, target_e)
        loss_f = torch.tensor(0.0) if pred_f is None else self.loss_fn_force(pred_f, target_f)
        loss_s = torch.tensor(0.0) if pred_s is None else self.loss_fn_stress(pred_s, target_s)
        if detail:
            return loss_e,self.force_weight*loss_f, self.stress_weight*loss_s
        return loss_e + self.force_weight*loss_f + self.stress_weight*loss_s
    
    def train_step(self):
        data = self.train_data
        self.model.train()
        def closure():
            self.optimizer.zero_grad()
            if not self.has_force and not self.has_stress:
                pred = self.model.cal_eng(data.fp, data.eng_indices, data.atom_num)
                loss_ = self.loss_fn(pred, data.eng)
            else:
                pred, pred_f, pred_s = self.model.cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume, create_graph=True)
                loss_ = self.loss_fn(pred, data.eng, pred_f, data.force, pred_s, data.stress)
            loss_ += self.l2_loss_weight * self.model.cal_l2_loss()
            loss_.backward()
            return loss_
        loss = closure()
        self.optimizer.step(closure)
        return loss.item()
    
    def test_loss(self):
        data = self.test_data
        self.model.eval()
        if not self.has_force and not self.has_stress:
            with torch.no_grad():
                pred = self.model.cal_eng(data.fp, data.eng_indices, data.atom_num)
                loss = self.loss_fn(pred, data.eng)
                return loss.item()
        pred, pred_f, pred_s = self.model.cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume)
        loss = self.loss_fn(pred, data.eng, pred_f, data.force, pred_s, data.stress)
        return loss.item()
    
    def save_checkpoint(self):
        self.model_state_dict = copy.deepcopy(self.model.state_dict())
    
    def load_checkpoint(self):
        self.model.load_state_dict(self.model_state_dict)
    
    def model_at(self, i: int):
        return self.model.sub_models[i]
    
    def models(self):
        return list(self.model.sub_models)
    
    def save_model(self, i: int):
        return save_model_(self.model.sub_models[i])
    
    def cal_mae(self, is_test: bool):
        if is_test:
            data = self.test_data
        else:
            data = self.train_data
        self.model.eval()
        if not self.has_force and not self.has_stress:
            with torch.no_grad():
                pred = self.model.cal_eng(data.fp, data.eng_indices, data.atom_num)
                return (data.eng - pred).abs().mean().item() * self.norm_sigma_eng, None, None
        pred, pred_f, pred_s = self.model.cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume)
        mae = (data.eng - pred).abs().mean().item() * self.norm_sigma_eng
        mae_f = None if pred_f is None else (data.force - pred_f).abs().mean().item() * self.norm_sigma_eng
        mae_s = None if pred_s is None else (data.stress - pred_s).abs().mean().item() * self.norm_sigma_eng
        return mae, mae_f, mae_s
    
    def cal_loss_detail(self):
        data = self.train_data
        self.model.eval()
        loss_l2 = self.l2_loss_weight * self.model.cal_l2_loss()
        if not self.has_force and not self.has_stress:
            with torch.no_grad():
                pred = self.model.cal_eng(data.fp, data.eng_indices, data.atom_num)
                loss_e = self.loss_fn(pred, data.eng)
                return loss_l2.item(), loss_e.item(), None, None
        pred, pred_f, pred_s = self.model.cal_eng_force_stress(data.fp, data.eng_indices, data.atom_num, data.fp_partial, data.force_indices, data.stress_indices, data.stress_dxyz, data.volume)
        loss_e, loss_f, loss_s = self.loss_fn(pred, data.eng, pred_f, data.force, pred_s, data.stress, detail=True)
        return loss_l2.item(), loss_e.item(), loss_f.item(), loss_s.item()


def save_model_(model):
    sub_model = model.double()
    state_dict_ = sub_model.state_dict()
    
    hidden_num = 0
    for name in state_dict_.keys():
        if name.endswith('weight') and ('hidden_layers' in name):
            hidden_num = hidden_num + 1
    
    input_dim = state_dict_['in_layer.weight'].shape[1]
    hidden_dims = [len(state_dict_['in_layer.bias'])]
    hidden_weights = [state_dict_['in_layer.weight'].tolist()]
    hidden_biases = [state_dict_['in_layer.bias'].tolist()]
    
    for i in range(hidden_num):
        hidden_dims.append(len(state_dict_['hidden_layers.'+str(i)+'.bias']))
        hidden_weights.append(state_dict_['hidden_layers.'+str(i)+'.weight'].tolist())
        hidden_biases.append(state_dict_['hidden_layers.'+str(i)+'.bias'].tolist())
    
    output_weight = state_dict_['out_layer.weight'].flatten().tolist()
    output_bias = float(state_dict_['out_layer.bias'][0])
    
    return input_dim, hidden_dims, hidden_weights, hidden_biases, output_weight, output_bias

def save_model_bytes_(model):
    sub_model = model.double()
    sub_model.eval()
    script_module = torch.jit.script(sub_model)
    
    buffer = io.BytesIO()
    torch.jit.save(script_module, buffer)
    buffer.seek(0)
    return buffer.read()

def load_model_bytes_(jbytes):
    pybytes = bytes([(b if b >= 0 else b + 256) for b in jbytes])
    return torch.jit.load(io.BytesIO(pybytes))

def convert_bytes_(jbytes):
    return save_model_(load_model_bytes_(jbytes))

