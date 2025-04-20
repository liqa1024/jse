package jsex.nep;

import jse.cache.DoubleArrayCache;
import jse.clib.IntCPointer;
import jse.clib.NestedIntCPointer;
import jse.code.IO;
import jse.code.OS;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.matrix.RowMatrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static jse.code.CS.ZL_STR;
import static jse.math.MathEX.Fast.*;

/**
 * jse 实现的 cpu 版 nep，具体定义可以参考：
 * <a href="https://pubs.aip.org/aip/jcp/article-abstract/157/11/114801/2841888/GPUMD-A-package-for-constructing-accurate-machine">
 * GPUMD: A package for constructing accurate machine-learned potentials and performing highly efficient atomistic simulations </a>
 * <p>
 * 这里照搬了 c++ 版本的 nep 实现：
 * <a href="https://github.com/brucefan1983/NEP_CPU">
 * brucefan1983/NEP_CPU: CPU version of NEP </a>
 * 因此同样也基于 GPL-3.0 协议
 * <p>
 * 所有命令照搬 c++ 中的实现，并且命名格式保留原本格式
 * <p>
 * 更新日期：2025-04-19
 * <p>
 * commit SHA: 817208327506fab599f942d5cdc2bff585f34f10
 *
 * @author Zheyong Fan, Junjie Wang, Eric Lindgren，liqa
 */
public class NEP {
    
    public final static class Conf {
        public static boolean USE_TABLE_FOR_RADIAL_FUNCTIONS = OS.envZ("JSE_NEP_USE_TABLE_FOR_RADIAL_FUNCTIONS", false);
    }
    
    NEP() {}
    public NEP(String aPotentialFileName) throws IOException {
        init_from_file(aPotentialFileName);
    }
    
    static class ParaMB {
        boolean use_typewise_cutoff = false;
        boolean use_typewise_cutoff_zbl = false;
        double typewise_cutoff_radial_factor = 2.5;
        double typewise_cutoff_angular_factor = 2.0;
        double typewise_cutoff_zbl_factor = 0.65;
        
        int model_type = 0; // 0=potential, 1=dipole, 2=polarizability
        int version = 4;
        double rc_radial = 0.0;
        double rc_angular = 0.0;
        double rcinv_radial = 0.0;
        double rcinv_angular = 0.0;
        int n_max_radial = 0;
        int n_max_angular = 0;
        int L_max = 0;
        int dim_angular;
        int num_L;
        int basis_size_radial = 8;
        int basis_size_angular = 8;
        int num_types_sq = 0;
        int num_c_radial = 0;
        int num_types = 0;
        double[] q_scaler = new double[140];
        int[] atomic_numbers = new int[94];
    }
    static class ANN {
        int dim = 0;
        int num_neurons1 = 0;
        int num_para = 0;
        int num_para_ann = 0;
        double[][] w0 = new double[94][];
        double[][] b0 = new double[94][];
        double[][] w1 = new double[94][];
        double[] b1;
        double[] c;
        // for the scalar part of polarizability
        double[][] w0_pol = new double[94][];
        double[][] b0_pol = new double[94][];
        double[][] w1_pol = new double[94][];
        double[] b1_pol;
    }
    static class ZBL {
        boolean enabled = false;
        boolean flexibled = false;
        int num_types;
        double rc_inner = 1.0;
        double rc_outer = 2.0;
        double[] para = new double[550];
    }
    
    int num_atoms = 0;
    int[] num_cells = new int[3];
    double[] ebox = new double[18];
    ParaMB paramb = new ParaMB();
    ANN annmb = new ANN();
    ZBL zbl = new ZBL();
    IntList NN_radial = new IntList(), NL_radial = new IntList(), NN_angular = new IntList(), NL_angular = new IntList();
    DoubleList r12 = new DoubleList();
    DoubleList Fp = new DoubleList();
    DoubleList sum_fxyz = new DoubleList();
    DoubleList parameters = new DoubleList();
    ArrayList<String> element_list = new ArrayList<>();
    
    DoubleList gn_radial = new DoubleList();   // tabulated gn_radial functions
    DoubleList gnp_radial = new DoubleList();  // tabulated gnp_radial functions
    DoubleList gn_angular = new DoubleList();  // tabulated gn_angular functions
    DoubleList gnp_angular = new DoubleList(); // tabulated gnp_angular functions
    
    void init_from_file(String potential_filename) throws IOException {
        int num_para_descriptor;
        try (BufferedReader input = IO.toReader(potential_filename)) {
            // nep3 1 C
            String[] tokens = get_tokens(input);
            if (tokens.length < 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("The first line of nep.txt should have at least 3 items.");
            }
            switch(tokens[0]) {
            case "nep3": {
                paramb.model_type = 0;
                paramb.version = 3;
                zbl.enabled = false;
                break;
            }
            case "nep3_zbl": {
                paramb.model_type = 0;
                paramb.version = 3;
                zbl.enabled = true;
                break;
            }
            case "nep3_dipole": {
                paramb.model_type = 1;
                paramb.version = 3;
                zbl.enabled = false;
                break;
            }
            case "nep3_polarizability": {
                paramb.model_type = 2;
                paramb.version = 3;
                zbl.enabled = false;
                break;
            }
            case "nep4": {
                paramb.model_type = 0;
                paramb.version = 4;
                zbl.enabled = false;
                break;
            }
            case "nep4_zbl": {
                paramb.model_type = 0;
                paramb.version = 4;
                zbl.enabled = true;
                break;
            }
            case "nep4_dipole": {
                paramb.model_type = 1;
                paramb.version = 4;
                zbl.enabled = false;
                break;
            }
            case "nep4_polarizability": {
                paramb.model_type = 2;
                paramb.version = 4;
                zbl.enabled = false;
                break;
            }
            case "nep5": {
                paramb.model_type = 0;
                paramb.version = 5;
                zbl.enabled = false;
                break;
            }
            case "nep5_zbl": {
                paramb.model_type = 0;
                paramb.version = 5;
                zbl.enabled = true;
                break;
            }
            }
            
            paramb.num_types = Integer.parseInt(tokens[1]);
            if (tokens.length != 2 + paramb.num_types) {
                print_tokens(tokens);
                throw new IllegalArgumentException("The first line of nep.txt should have " + paramb.num_types + " atom symbols.");
            }
            
            element_list.clear();
            element_list.ensureCapacity(paramb.num_types);
            for (int n = 0; n < paramb.num_types; ++n) {
                int atomic_number = 0;
                element_list.add(tokens[2 + n]);
                for (int m = 0; m < NUM_ELEMENTS; ++m) {
                    if (ELEMENTS[m].equals(tokens[2 + n])) {
                        atomic_number = m;
                        break;
                    }
                }
                paramb.atomic_numbers[n] = atomic_number;
            }
            
            // zbl 0.7 1.4
            if (zbl.enabled) {
                tokens = get_tokens(input);
                if (tokens.length != 3) {
                    print_tokens(tokens);
                    throw new IllegalArgumentException("This line should be zbl rc_inner rc_outer.");
                }
                zbl.rc_inner = Double.parseDouble(tokens[1]);
                zbl.rc_outer = Double.parseDouble(tokens[2]);
                if (zbl.rc_inner == 0 && zbl.rc_outer == 0) {
                    zbl.flexibled = true;
                }
            }
            
            // cutoff 4.2 3.7 80 47
            tokens = get_tokens(input);
            if (tokens.length != 5 && tokens.length != 8) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be cutoff rc_radial rc_angular MN_radial MN_angular [radial_factor] [angular_factor] [zbl_factor].");
            }
            paramb.rc_radial = Double.parseDouble(tokens[1]);
            paramb.rc_angular = Double.parseDouble(tokens[2]);
            int MN_radial = Integer.parseInt(tokens[3]);  // not used
            int MN_angular = Integer.parseInt(tokens[4]); // not used
            if (tokens.length == 8) {
                paramb.typewise_cutoff_radial_factor = Double.parseDouble(tokens[5]);
                paramb.typewise_cutoff_angular_factor = Double.parseDouble(tokens[6]);
                paramb.typewise_cutoff_zbl_factor = Double.parseDouble(tokens[7]);
                if (paramb.typewise_cutoff_radial_factor > 0.0) {
                    paramb.use_typewise_cutoff = true;
                }
                if (paramb.typewise_cutoff_zbl_factor > 0.0) {
                    paramb.use_typewise_cutoff_zbl = true;
                }
            }
            
            // n_max 10 8
            tokens = get_tokens(input);
            if (tokens.length != 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be n_max n_max_radial n_max_angular.");
            }
            paramb.n_max_radial = Integer.parseInt(tokens[1]);
            paramb.n_max_angular = Integer.parseInt(tokens[2]);
            
            // basis_size 10 8
            tokens = get_tokens(input);
            if (tokens.length != 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be basis_size basis_size_radial basis_size_angular.");
            }
            paramb.basis_size_radial = Integer.parseInt(tokens[1]);
            paramb.basis_size_angular = Integer.parseInt(tokens[2]);
            
            // l_max
            tokens = get_tokens(input);
            if (tokens.length != 4) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be l_max l_max_3body l_max_4body l_max_5body.");
            }
            paramb.L_max = Integer.parseInt(tokens[1]);
            paramb.num_L = paramb.L_max;
            
            int L_max_4body = Integer.parseInt(tokens[2]);
            int L_max_5body = Integer.parseInt(tokens[3]);
            if (L_max_4body == 2) {
                paramb.num_L += 1;
            }
            if (L_max_5body == 1) {
                paramb.num_L += 1;
            }
            paramb.dim_angular = (paramb.n_max_angular + 1) * paramb.num_L;
            
            // ANN
            tokens = get_tokens(input);
            if (tokens.length != 3) {
                print_tokens(tokens);
                throw new IllegalArgumentException("This line should be ANN num_neurons 0.");
            }
            annmb.num_neurons1 = Integer.parseInt(tokens[1]);
            annmb.dim = (paramb.n_max_radial + 1) + paramb.dim_angular;
            
            // calculated parameters:
            paramb.rcinv_radial = 1.0 / paramb.rc_radial;
            paramb.rcinv_angular = 1.0 / paramb.rc_angular;
            paramb.num_types_sq = paramb.num_types * paramb.num_types;
            if (paramb.version == 3) {
                annmb.num_para_ann = (annmb.dim + 2) * annmb.num_neurons1 + 1;
            } else if (paramb.version == 4) {
                annmb.num_para_ann = (annmb.dim + 2) * annmb.num_neurons1 * paramb.num_types + 1;
            } else {
                annmb.num_para_ann = ((annmb.dim + 2) * annmb.num_neurons1 + 1) * paramb.num_types + 1;
            }
            if (paramb.model_type == 2) {
                annmb.num_para_ann *= 2;
            }
            num_para_descriptor = paramb.num_types_sq * ((paramb.n_max_radial + 1) * (paramb.basis_size_radial + 1) +
                                                         (paramb.n_max_angular + 1) * (paramb.basis_size_angular + 1));
            annmb.num_para = annmb.num_para_ann + num_para_descriptor;
            paramb.num_c_radial = paramb.num_types_sq * (paramb.n_max_radial + 1) * (paramb.basis_size_radial + 1);
            
            // NN and descriptor parameters
            parameters.clear();
            parameters.ensureCapacity(annmb.num_para);
            for (int n = 0; n < annmb.num_para; ++n) {
                tokens = get_tokens(input);
                parameters.add(Double.parseDouble(tokens[0]));
            }
            update_potential(parameters.internalData(), annmb);
            for (int d = 0; d < annmb.dim; ++d) {
                tokens = get_tokens(input);
                paramb.q_scaler[d] = Double.parseDouble(tokens[0]);
            }
            
            // flexible zbl potential parameters if (zbl.flexibled)
            if (zbl.flexibled) {
                int num_type_zbl = (paramb.num_types * (paramb.num_types + 1)) / 2;
                for (int d = 0; d < 10 * num_type_zbl; ++d) {
                    tokens = get_tokens(input);
                    zbl.para[d] = Double.parseDouble(tokens[0]);
                }
                zbl.num_types = paramb.num_types;
            }
        }

        if (Conf.USE_TABLE_FOR_RADIAL_FUNCTIONS) {
            if (paramb.use_typewise_cutoff) {
                throw new IllegalStateException("Cannot use tabulated radial functions with typewise cutoff.");
            }
            construct_table(parameters.internalData());
        }
    }
    
    void update_type_map(int ntype, int[] type_map, String[] elements) {
        int n = 0;
        for (int itype = 0; itype < ntype + 1; ++itype) {
            // check if set NULL in lammps input file
            if (type_map[itype] == -1) {
                continue;
            }
            // find the same element name in potential file
            String element_name = elements[type_map[itype]];
            for (n = 0; n < paramb.num_types; ++n) {
                if (element_name.equals(element_list.get(n))) {
                    type_map[itype] = n;
                    break;
                }
            }
            // check if no corresponding element
            if (n == paramb.num_types) {
                throw new IllegalArgumentException("There is no element " + element_name + " in the potential file.");
            }
        }
    }
    
    void update_potential(double[] parameters, ANN ann) {
        int start = 0;
        double[] buf;
        for (int t = 0; t < paramb.num_types; ++t) {
            if (t > 0 && paramb.version == 3) { // Use the same set of NN parameters for NEP3
                start -= (ann.dim+2) * ann.num_neurons1;
            }
            buf = new double[ann.num_neurons1 * ann.dim]; System.arraycopy(parameters, start, buf, 0, buf.length);
            ann.w0[t] = buf;
            start += buf.length;
            buf = new double[ann.num_neurons1]; System.arraycopy(parameters, start, buf, 0, buf.length);
            ann.b0[t] = buf;
            start += buf.length;
            buf = new double[paramb.version==5 ? ann.num_neurons1+1 : ann.num_neurons1]; // one extra bias for NEP5 stored in ann.w1[t]
            System.arraycopy(parameters, start, buf, 0, buf.length);
            ann.w1[t] = buf;
            start += buf.length;
        }
        ann.b1 = new double[] {parameters[start]};
        start += 1;
        
        if (paramb.model_type == 2) {
            for (int t = 0; t < paramb.num_types; ++t) {
                if (t > 0 && paramb.version == 3) { // Use the same set of NN parameters for NEP3
                    start -= (ann.dim + 2) * ann.num_neurons1;
                }
                buf = new double[ann.num_neurons1 * ann.dim]; System.arraycopy(parameters, start, buf, 0, buf.length);
                ann.w0_pol[t] = buf;
                start += buf.length;
                buf = new double[ann.num_neurons1]; System.arraycopy(parameters, start, buf, 0, buf.length);
                ann.b0_pol[t] = buf;
                start += buf.length;
                buf = new double[ann.num_neurons1]; System.arraycopy(parameters, start, buf, 0, buf.length);
                ann.w1_pol[t] = buf;
                start += buf.length;
            }
            ann.b1_pol = new double[] {parameters[start]};
            start += 1;
        }
        buf = new double[parameters.length-start]; System.arraycopy(parameters, start, buf, 0, buf.length);
        ann.c = buf;
    }
    
    void construct_table(double[] parameters) {
        gn_radial.clear(); gn_radial.addZeros(table_length * paramb.num_types_sq * (paramb.n_max_radial + 1));
        gnp_radial.clear(); gnp_radial.addZeros(table_length * paramb.num_types_sq * (paramb.n_max_radial + 1));
        gn_angular.clear(); gn_angular.addZeros(table_length * paramb.num_types_sq * (paramb.n_max_angular + 1));
        gnp_angular.clear(); gnp_angular.addZeros(table_length * paramb.num_types_sq * (paramb.n_max_angular + 1));
        construct_table_radial_or_angular(
            paramb.version, paramb.num_types, paramb.num_types_sq, paramb.n_max_radial,
            paramb.basis_size_radial, paramb.rc_radial, paramb.rcinv_radial, parameters, annmb.num_para_ann, gn_radial.internalData(),
            gnp_radial.internalData()
        );
        construct_table_radial_or_angular(
            paramb.version, paramb.num_types, paramb.num_types_sq, paramb.n_max_angular,
            paramb.basis_size_angular, paramb.rc_angular, paramb.rcinv_angular,
            parameters, annmb.num_para_ann+paramb.num_c_radial, gn_angular.internalData(), gnp_angular.internalData()
        );
    }
    
    void compute_for_lammps(int nlocal, int N, IntCPointer ilist, IntCPointer NN, NestedIntCPointer NL,
                            int[] type, int[] type_map, RowMatrix pos,
                            double[] total_potential, double[] total_virial,
                            double[] potential, RowMatrix force, RowMatrix virial) {
        if (num_atoms < nlocal) {
            Fp.clear(); Fp.addZeros(nlocal * annmb.dim);
            sum_fxyz.clear(); sum_fxyz.addZeros(nlocal * (paramb.n_max_angular + 1) * NUM_OF_ABC);
            num_atoms = nlocal;
        }
        find_descriptor_for_lammps(
            paramb, annmb, nlocal, N, ilist, NN, NL, type, type_map, pos,
            gn_radial.internalData(), gn_angular.internalData(),
            Fp.internalData(), sum_fxyz.internalData(), total_potential, potential
        );
        find_force_radial_for_lammps(
            paramb, annmb, nlocal, N, ilist, NN, NL, type, type_map, pos, Fp.internalData(),
            gnp_radial.internalData(),
            force, total_virial, virial
        );
        find_force_angular_for_lammps(
            paramb, annmb, nlocal, N, ilist, NN, NL, type, type_map, pos, Fp.internalData(), sum_fxyz.internalData(),
            gn_angular.internalData(), gnp_angular.internalData(),
            force, total_virial, virial
        );
        if (zbl.enabled) {
            find_force_ZBL_for_lammps(
                paramb, zbl, N, ilist, NN, NL, type, type_map, pos, force, total_virial, virial,
                total_potential, potential
            );
        }
    }
    
    
    static final int MAX_NEURON = 200; // maximum number of neurons in the hidden layer
    static final int MN = 1000;        // maximum number of neighbors for one atom
    static final int NUM_OF_ABC = 80;  // 3 + 5 + 7 + 9 + 11 + 13 + 15 + 17 for L_max = 8
    static final int MAX_NUM_N = 20;   // n_max+1 = 19+1
    static final int MAX_DIM = MAX_NUM_N * 7;
    static final int MAX_DIM_ANGULAR = MAX_NUM_N * 6;
    static final double[] C3B = {
        0.238732414637843, 0.119366207318922, 0.119366207318922, 0.099471839432435, 0.596831036594608,
        0.596831036594608, 0.149207759148652, 0.149207759148652, 0.139260575205408, 0.104445431404056,
        0.104445431404056, 1.044454314040563, 1.044454314040563, 0.174075719006761, 0.174075719006761,
        0.011190581936149, 0.223811638722978, 0.223811638722978, 0.111905819361489, 0.111905819361489,
        1.566681471060845, 1.566681471060845, 0.195835183882606, 0.195835183882606, 0.013677377921960,
        0.102580334414698, 0.102580334414698, 2.872249363611549, 2.872249363611549, 0.119677056817148,
        0.119677056817148, 2.154187022708661, 2.154187022708661, 0.215418702270866, 0.215418702270866,
        0.004041043476943, 0.169723826031592, 0.169723826031592, 0.106077391269745, 0.106077391269745,
        0.424309565078979, 0.424309565078979, 0.127292869523694, 0.127292869523694, 2.800443129521260,
        2.800443129521260, 0.233370260793438, 0.233370260793438, 0.004662742473395, 0.004079899664221,
        0.004079899664221, 0.024479397985326, 0.024479397985326, 0.012239698992663, 0.012239698992663,
        0.538546755677165, 0.538546755677165, 0.134636688919291, 0.134636688919291, 3.500553911901575,
        3.500553911901575, 0.250039565135827, 0.250039565135827, 0.000082569397966, 0.005944996653579,
        0.005944996653579, 0.104037441437634, 0.104037441437634, 0.762941237209318, 0.762941237209318,
        0.114441185581398, 0.114441185581398, 5.950941650232678, 5.950941650232678, 0.141689086910302,
        0.141689086910302, 4.250672607309055, 4.250672607309055, 0.265667037956816, 0.265667037956816
    };
    static final double[] C4B = {
        -0.007499480826664, -0.134990654879954, 0.067495327439977, 0.404971964639861, -0.809943929279723
    };
    static final double[] C5B = {0.026596810706114, 0.053193621412227, 0.026596810706114};
    static final double[][] Z_COEFFICIENT_1 = {{0.0, 1.0}, {1.0, 0.0}};
    static final double[][] Z_COEFFICIENT_2 = {{-1.0, 0.0, 3.0}, {0.0, 1.0, 0.0}, {1.0, 0.0, 0.0}};
    static final double[][] Z_COEFFICIENT_3 = {
        {0.0, -3.0, 0.0, 5.0}, {-1.0, 0.0, 5.0, 0.0}, {0.0, 1.0, 0.0, 0.0}, {1.0, 0.0, 0.0, 0.0}
    };
    static final double[][] Z_COEFFICIENT_4 = {
        {3.0, 0.0, -30.0, 0.0, 35.0},
        {0.0, -3.0, 0.0, 7.0, 0.0},
        {-1.0, 0.0, 7.0, 0.0, 0.0},
        {0.0, 1.0, 0.0, 0.0, 0.0},
        {1.0, 0.0, 0.0, 0.0, 0.0}
    };
    static final double[][] Z_COEFFICIENT_5 = {
        {0.0, 15.0, 0.0, -70.0, 0.0, 63.0}, {1.0, 0.0, -14.0, 0.0, 21.0, 0.0},
        {0.0, -1.0, 0.0, 3.0, 0.0, 0.0},    {-1.0, 0.0, 9.0, 0.0, 0.0, 0.0},
        {0.0, 1.0, 0.0, 0.0, 0.0, 0.0},     {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };
    static final double[][] Z_COEFFICIENT_6 = {
        {-5.0, 0.0, 105.0, 0.0, -315.0, 0.0, 231.0}, {0.0, 5.0, 0.0, -30.0, 0.0, 33.0, 0.0},
        {1.0, 0.0, -18.0, 0.0, 33.0, 0.0, 0.0},      {0.0, -3.0, 0.0, 11.0, 0.0, 0.0, 0.0},
        {-1.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0},       {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };
    static final double[][] Z_COEFFICIENT_7 = {
        {0.0, -35.0, 0.0, 315.0, 0.0, -693.0, 0.0, 429.0},
        {-5.0, 0.0, 135.0, 0.0, -495.0, 0.0, 429.0, 0.0},
        {0.0, 15.0, 0.0, -110.0, 0.0, 143.0, 0.0, 0.0},
        {3.0, 0.0, -66.0, 0.0, 143.0, 0.0, 0.0, 0.0},
        {0.0, -3.0, 0.0, 13.0, 0.0, 0.0, 0.0, 0.0},
        {-1.0, 0.0, 13.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };
    static final double[][] Z_COEFFICIENT_8 = {
        {35.0, 0.0, -1260.0, 0.0, 6930.0, 0.0, -12012.0, 0.0, 6435.0},
        {0.0, -35.0, 0.0, 385.0, 0.0, -1001.0, 0.0, 715.0, 0.0},
        {-1.0, 0.0, 33.0, 0.0, -143.0, 0.0, 143.0, 0.0, 0.0},
        {0.0, 3.0, 0.0, -26.0, 0.0, 39.0, 0.0, 0.0, 0.0},
        {1.0, 0.0, -26.0, 0.0, 65.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, -1.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {-1.0, 0.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };
    static final double K_C_SP = 14.399645; // 1/(4*PI*epsilon_0)
    static final double PI = 3.141592653589793;
    static final double PI_HALF = 1.570796326794897;
    static final int NUM_ELEMENTS = 94;
    static final String[] ELEMENTS = {
        "H",  "He", "Li", "Be", "B",  "C",  "N",  "O",  "F",  "Ne", "Na", "Mg", "Al", "Si", "P",  "S",
        "Cl", "Ar", "K",  "Ca", "Sc", "Ti", "V",  "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge",
        "As", "Se", "Br", "Kr", "Rb", "Sr", "Y",  "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd",
        "In", "Sn", "Sb", "Te", "I",  "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd",
        "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta", "W",  "Re", "Os", "Ir", "Pt", "Au", "Hg",
        "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Th", "Pa", "U",  "Np", "Pu"
    };
    static final double[] COVALENT_RADIUS = {
        0.426667, 0.613333, 1.6,     1.25333, 1.02667, 1.0,     0.946667, 0.84,    0.853333, 0.893333,
        1.86667,  1.66667,  1.50667, 1.38667, 1.46667, 1.36,    1.32,     1.28,    2.34667,  2.05333,
        1.77333,  1.62667,  1.61333, 1.46667, 1.42667, 1.38667, 1.33333,  1.32,    1.34667,  1.45333,
        1.49333,  1.45333,  1.53333, 1.46667, 1.52,    1.56,    2.52,     2.22667, 1.96,     1.85333,
        1.76,     1.65333,  1.53333, 1.50667, 1.50667, 1.44,    1.53333,  1.64,    1.70667,  1.68,
        1.68,     1.64,     1.76,    1.74667, 2.78667, 2.34667, 2.16,     1.96,    2.10667,  2.09333,
        2.08,     2.06667,  2.01333, 2.02667, 2.01333, 2.0,     1.98667,  1.98667, 1.97333,  2.04,
        1.94667,  1.82667,  1.74667, 1.64,    1.57333, 1.54667, 1.48,     1.49333, 1.50667,  1.76,
        1.73333,  1.73333,  1.81333, 1.74667, 1.84,    1.89333, 2.68,     2.41333, 2.22667,  2.10667,
        2.02667,  2.04,     2.05333, 2.06667
    };
    
    static void complex_product(double a, double b, double[] real_part, double[] imag_part) {
        double real_temp = real_part[0];
        real_part[0] = a * real_temp - b * imag_part[0];
        imag_part[0] = a * imag_part[0] + b * real_temp;
    }
    
    static void apply_ann_one_layer(int dim, int num_neurons1, 
                                    double[] w0, double[] b0, double[] w1, double[] b1, double[] q,
                                    double[] energy, double[] energy_derivative, double[] latent_space,
                                    boolean need_B_projection, double[] B_projection, int shift) {
        for (int n = 0; n < num_neurons1; ++n) {
            double w0_times_q = 0.0;
            for (int d = 0; d < dim; ++d) {
                w0_times_q += w0[n * dim + d] * q[d];
            }
            double x1 = tanh(w0_times_q - b0[n]);
            double tan_der = 1.0 - x1 * x1;
            
            if (need_B_projection) {
                // calculate B_projection:
                // dE/dw0
                for (int d = 0; d < dim; ++d)
                    B_projection[shift + n * (dim + 2) + d] = tan_der * q[d] * w1[n];
                // dE/db0
                B_projection[shift + n * (dim + 2) + dim] = -tan_der * w1[n];
                // dE/dw1
                B_projection[shift + n * (dim + 2) + dim + 1] = x1;
            }
            
            latent_space[n] = w1[n] * x1; // also try x1
            energy[0] += w1[n] * x1;
            for (int d = 0; d < dim; ++d) {
                double y1 = tan_der * w0[n * dim + d];
                energy_derivative[d] += w1[n] * y1;
            }
        }
        energy[0] -= b1[0];
    }
    
    static void apply_ann_one_layer_nep5(int dim, int num_neurons1,
                                         double[] w0, double[] b0, double[] w1, double[] b1, double[] q,
                                         double[] energy, double[] energy_derivative, double[] latent_space) {
        for (int n = 0; n < num_neurons1; ++n) {
            double w0_times_q = 0.0;
            for (int d = 0; d < dim; ++d) {
                w0_times_q += w0[n * dim + d] * q[d];
            }
            double x1 = tanh(w0_times_q - b0[n]);
            latent_space[n] = w1[n] * x1; // also try x1
            energy[0] += w1[n] * x1;
            for (int d = 0; d < dim; ++d) {
                double y1 = (1.0 - x1 * x1) * w0[n * dim + d];
                energy_derivative[d] += w1[n] * y1;
            }
        }
        energy[0] -= w1[num_neurons1] + b1[0]; // typewise bias + common bias
    }
    
    static void find_fc(double rc, double rcinv, double d12, double[] fc) {
        if (d12 < rc) {
            double x = d12 * rcinv;
            fc[0] = 0.5 * cos(PI * x) + 0.5;
        } else {
            fc[0] = 0.0;
        }
    }
    
    static void find_fc_and_fcp(double rc, double rcinv, double d12, double[] fc, double[] fcp) {
        if (d12 < rc) {
            double x = d12 * rcinv;
            fc[0] = 0.5 * cos(PI * x) + 0.5;
            fcp[0] = -PI_HALF * sin(PI * x);
            fcp[0] *= rcinv;
        } else {
            fc[0] = 0.0;
            fcp[0] = 0.0;
        }
    }
    
    static void find_fc_and_fcp_zbl(double r1, double r2, double d12, double[] fc, double[] fcp) {
        if (d12 < r1) {
            fc[0] = 1.0;
            fcp[0] = 0.0;
        } else if (d12 < r2) {
            double pi_factor = PI / (r2 - r1);
            fc[0] = cos(pi_factor * (d12 - r1)) * 0.5 + 0.5;
            fcp[0] = -sin(pi_factor * (d12 - r1)) * pi_factor * 0.5;
        } else {
            fc[0] = 0.0;
            fcp[0] = 0.0;
        }
    }
    
    static void find_phi_and_phip_zbl(double a, double b, double x, double[] phi, double[] phip) {
        double tmp = a * exp(-b * x);
        phi[0] += tmp;
        phip[0] -= b * tmp;
    }
    
    static void find_f_and_fp_zbl(double zizj, double a_inv, 
                                  double rc_inner, double rc_outer, 
                                  double d12, double d12inv, 
                                  double[] f, double[] fp) {
        double x = d12 * a_inv;
        f[0] = fp[0] = 0.0;
        double[] Zbl_para = {0.18175, 3.1998, 0.50986, 0.94229, 0.28022, 0.4029, 0.02817, 0.20162};
        find_phi_and_phip_zbl(Zbl_para[0], Zbl_para[1], x, f, fp);
        find_phi_and_phip_zbl(Zbl_para[2], Zbl_para[3], x, f, fp);
        find_phi_and_phip_zbl(Zbl_para[4], Zbl_para[5], x, f, fp);
        find_phi_and_phip_zbl(Zbl_para[6], Zbl_para[7], x, f, fp);
        f[0] *= zizj;
        fp[0] *= zizj * a_inv;
        fp[0] = fp[0] * d12inv - f[0] * d12inv * d12inv;
        f[0] *= d12inv;
        double[] fc = {0.0}, fcp = {0.0};
        find_fc_and_fcp_zbl(rc_inner, rc_outer, d12, fc, fcp);
        fp[0] = fp[0] * fc[0] + f[0] * fcp[0];
        f[0] *= fc[0];
    }
    
    static void find_f_and_fp_zbl(double[] zbl_para, double zizj, double a_inv, 
                                  double d12, double d12inv, 
                                  double[] f, double[] fp) {
        double x = d12 * a_inv;
        f[0] = fp[0] = 0.0;
        find_phi_and_phip_zbl(zbl_para[2], zbl_para[3], x, f, fp);
        find_phi_and_phip_zbl(zbl_para[4], zbl_para[5], x, f, fp);
        find_phi_and_phip_zbl(zbl_para[6], zbl_para[7], x, f, fp);
        find_phi_and_phip_zbl(zbl_para[8], zbl_para[9], x, f, fp);
        f[0] *= zizj;
        fp[0] *= zizj * a_inv;
        fp[0] = fp[0] * d12inv - f[0] * d12inv * d12inv;
        f[0] *= d12inv;
        double[] fc = {0.0}, fcp = {0.0};
        find_fc_and_fcp_zbl(zbl_para[0], zbl_para[1], d12, fc, fcp);
        fp[0] = fp[0] * fc[0] + f[0] * fcp[0];
        f[0] *= fc[0];
    }
    
    static void find_fn0(int n, double rcinv, double d12, double fc12, double[] fn) {
        if (n == 0) {
            fn[0] = fc12;
        } else if (n == 1) {
            double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
            fn[0] = (x + 1.0) * 0.5 * fc12;
        } else {
            double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
            double t0 = 1.0;
            double t1 = x;
            double t2 = 0.0;
            for (int m = 2; m <= n; ++m) {
                t2 = 2.0 * x * t1 - t0;
                t0 = t1;
                t1 = t2;
            }
            fn[0] = (t2 + 1.0) * 0.5 * fc12;
        }
    }
    
    static void find_fn_and_fnp0(int n, double rcinv, 
                                 double d12, double fc12, double fcp12, 
                                 double[] fn, double[] fnp) {
        if (n == 0) {
            fn[0] = fc12;
            fnp[0] = fcp12;
        } else if (n == 1) {
            double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
            fn[0] = (x + 1.0) * 0.5;
            fnp[0] = 2.0 * (d12 * rcinv - 1.0) * rcinv * fc12 + fn[0] * fcp12;
            fn[0] *= fc12;
        } else {
            double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
            double t0 = 1.0;
            double t1 = x;
            double t2 = 0.0;
            double u0 = 1.0;
            double u1 = 2.0 * x;
            double u2;
            for (int m = 2; m <= n; ++m) {
                t2 = 2.0 * x * t1 - t0;
                t0 = t1;
                t1 = t2;
                u2 = 2.0 * x * u1 - u0;
                u0 = u1;
                u1 = u2;
            }
            fn[0] = (t2 + 1.0) * 0.5;
            fnp[0] = n * u0 * 2.0 * (d12 * rcinv - 1.0) * rcinv;
            fnp[0] = fnp[0] * fc12 + fn[0] * fcp12;
            fn[0] *= fc12;
        }
    }
    
    static void find_fn(int n_max, double rcinv, double d12, double fc12, double[] fn) {
        double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
        fn[0] = 1.0;
        fn[1] = x;
        for (int m = 2; m <= n_max; ++m) {
            fn[m] = 2.0 * x * fn[m - 1] - fn[m - 2];
        }
        for (int m = 0; m <= n_max; ++m) {
            fn[m] = (fn[m] + 1.0) * 0.5 * fc12;
        }
    }
    
    static void find_fn_and_fnp(int n_max, double rcinv, 
                                double d12, double fc12, double fcp12, 
                                double[] fn, double[] fnp) {
        double x = 2.0 * (d12 * rcinv - 1.0) * (d12 * rcinv - 1.0) - 1.0;
        fn[0] = 1.0;
        fnp[0] = 0.0;
        fn[1] = x;
        fnp[1] = 1.0;
        double u0 = 1.0;
        double u1 = 2.0 * x;
        double u2;
        for (int m = 2; m <= n_max; ++m) {
            fn[m] = 2.0 * x * fn[m - 1] - fn[m - 2];
            fnp[m] = m * u1;
            u2 = 2.0 * x * u1 - u0;
            u0 = u1;
            u1 = u2;
        }
        for (int m = 0; m <= n_max; ++m) {
            fn[m] = (fn[m] + 1.0) * 0.5;
            fnp[m] *= 2.0 * (d12 * rcinv - 1.0) * rcinv;
            fnp[m] = fnp[m] * fc12 + fn[m] * fcp12;
            fn[m] *= fc12;
        }
    }
    
    static void get_f12_4body(double d12, double d12inv, 
                              double fn, double fnp, double Fp, 
                              double[] s, double[] r12, double[] f12) {
        double fn_factor = Fp * fn;
        double fnp_factor = Fp * fnp * d12inv;
        double y20 = (3.0 * r12[2] * r12[2] - d12 * d12);
        
        // derivative wrt s[0]
        double tmp0 = C4B[0] * 3.0 * s[0] * s[0] + C4B[1] * (s[1] * s[1] + s[2] * s[2]) +
            C4B[2] * (s[3] * s[3] + s[4] * s[4]);
        double tmp1 = tmp0 * y20 * fnp_factor;
        double tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0] - tmp2 * 2.0 * r12[0];
        f12[1] += tmp1 * r12[1] - tmp2 * 2.0 * r12[1];
        f12[2] += tmp1 * r12[2] + tmp2 * 4.0 * r12[2];
        
        // derivative wrt s[1]
        tmp0 = C4B[1] * s[0] * s[1] * 2.0 - C4B[3] * s[3] * s[1] * 2.0 + C4B[4] * s[2] * s[4];
        tmp1 = tmp0 * r12[0] * r12[2] * fnp_factor;
        tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0] + tmp2 * r12[2];
        f12[1] += tmp1 * r12[1];
        f12[2] += tmp1 * r12[2] + tmp2 * r12[0];
        
        // derivative wrt s[2]
        tmp0 = C4B[1] * s[0] * s[2] * 2.0 + C4B[3] * s[3] * s[2] * 2.0 + C4B[4] * s[1] * s[4];
        tmp1 = tmp0 * r12[1] * r12[2] * fnp_factor;
        tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0];
        f12[1] += tmp1 * r12[1] + tmp2 * r12[2];
        f12[2] += tmp1 * r12[2] + tmp2 * r12[1];
        
        // derivative wrt s[3]
        tmp0 = C4B[2] * s[0] * s[3] * 2.0 + C4B[3] * (s[2] * s[2] - s[1] * s[1]);
        tmp1 = tmp0 * (r12[0] * r12[0] - r12[1] * r12[1]) * fnp_factor;
        tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0] + tmp2 * 2.0 * r12[0];
        f12[1] += tmp1 * r12[1] - tmp2 * 2.0 * r12[1];
        f12[2] += tmp1 * r12[2];
        
        // derivative wrt s[4]
        tmp0 = C4B[2] * s[0] * s[4] * 2.0 + C4B[4] * s[1] * s[2];
        tmp1 = tmp0 * (2.0 * r12[0] * r12[1]) * fnp_factor;
        tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0] + tmp2 * 2.0 * r12[1];
        f12[1] += tmp1 * r12[1] + tmp2 * 2.0 * r12[0];
        f12[2] += tmp1 * r12[2];
    }
    
    static void get_f12_5body(double d12, double d12inv, double fn, double fnp, double Fp, 
                              double[] s, double[] r12, double[] f12) {
        double fn_factor = Fp * fn;
        double fnp_factor = Fp * fnp * d12inv;
        double s1_sq_plus_s2_sq = s[1] * s[1] + s[2] * s[2];
        
        // derivative wrt s[0]
        double tmp0 = C5B[0] * 4.0 * s[0] * s[0] * s[0] + C5B[1] * s1_sq_plus_s2_sq * 2.0 * s[0];
        double tmp1 = tmp0 * r12[2] * fnp_factor;
        double tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0];
        f12[1] += tmp1 * r12[1];
        f12[2] += tmp1 * r12[2] + tmp2;
        
        // derivative wrt s[1]
        tmp0 = C5B[1] * s[0] * s[0] * s[1] * 2.0 + C5B[2] * s1_sq_plus_s2_sq * s[1] * 4.0;
        tmp1 = tmp0 * r12[0] * fnp_factor;
        tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0] + tmp2;
        f12[1] += tmp1 * r12[1];
        f12[2] += tmp1 * r12[2];
        
        // derivative wrt s[2]
        tmp0 = C5B[1] * s[0] * s[0] * s[2] * 2.0 + C5B[2] * s1_sq_plus_s2_sq * s[2] * 4.0;
        tmp1 = tmp0 * r12[1] * fnp_factor;
        tmp2 = tmp0 * fn_factor;
        f12[0] += tmp1 * r12[0];
        f12[1] += tmp1 * r12[1] + tmp2;
        f12[2] += tmp1 * r12[2];
    }
    
    static void calculate_s_one(int L, int n, int n_max_angular_plus_1, 
                                double[] Fp, double[] sum_fxyz, double[] s) {
        int L_minus_1 = L - 1;
        int L_twice_plus_1 = 2 * L + 1;
        int L_square_minus_1 = L * L - 1;
        double Fp_factor = 2.0 * Fp[L_minus_1 * n_max_angular_plus_1 + n];
        s[0] = sum_fxyz[n * NUM_OF_ABC + L_square_minus_1] * C3B[L_square_minus_1] * Fp_factor;
        Fp_factor *= 2.0;
        for (int k = 1; k < L_twice_plus_1; ++k) {
            s[k] = sum_fxyz[n * NUM_OF_ABC + L_square_minus_1 + k] * C3B[L_square_minus_1 + k] * Fp_factor;
        }
    }
    
    static void accumulate_f12_one(int L, double d12inv, double fn, double fnp, 
                                   double[] s, double[] r12, double[] f12) {
        double[] dx = {(1.0 - r12[0] * r12[0]) * d12inv, -r12[0] * r12[1] * d12inv, -r12[0] * r12[2] * d12inv};
        double[] dy = {-r12[0] * r12[1] * d12inv, (1.0 - r12[1] * r12[1]) * d12inv, -r12[1] * r12[2] * d12inv};
        double[] dz = {-r12[0] * r12[2] * d12inv, -r12[1] * r12[2] * d12inv, (1.0 - r12[2] * r12[2]) * d12inv};
        
        double[] z_pow = DoubleArrayCache.getArray(L+1);
        z_pow[0] = 1.0;
        for (int n = 1; n <= L; ++n) {
            z_pow[n] = r12[2] * z_pow[n-1];
        }
        
        double[] real_part = {1.0};
        double[] imag_part = {0.0};
        for (int n1 = 0; n1 <= L; ++n1) {
            int n2_start = (L + n1) % 2 == 0 ? 0 : 1;
            double z_factor = 0.0;
            double dz_factor = 0.0;
            for (int n2 = n2_start; n2 <= L - n1; n2 += 2) {
                if (L == 1) {
                    z_factor += Z_COEFFICIENT_1[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_1[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
                if (L == 2) {
                    z_factor += Z_COEFFICIENT_2[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_2[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
                if (L == 3) {
                    z_factor += Z_COEFFICIENT_3[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_3[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
                if (L == 4) {
                    z_factor += Z_COEFFICIENT_4[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_4[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
                if (L == 5) {
                    z_factor += Z_COEFFICIENT_5[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_5[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
                if (L == 6) {
                    z_factor += Z_COEFFICIENT_6[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_6[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
                if (L == 7) {
                    z_factor += Z_COEFFICIENT_7[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_7[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
                if (L == 8) {
                    z_factor += Z_COEFFICIENT_8[n1][n2] * z_pow[n2];
                    if (n2 > 0) {
                        dz_factor += Z_COEFFICIENT_8[n1][n2] * n2 * z_pow[n2 - 1];
                    }
                }
            }
            if (n1 == 0) {
                for (int d = 0; d < 3; ++d) {
                    f12[d] += s[0] * (z_factor * fnp * r12[d] + fn * dz_factor * dz[d]);
                }
            } else {
                double real_part_n1 = n1 * real_part[0];
                double imag_part_n1 = n1 * imag_part[0];
                for (int d = 0; d < 3; ++d) {
                    double[] real_part_dx = {dx[d]};
                    double[] imag_part_dy = {dy[d]};
                    complex_product(real_part_n1, imag_part_n1, real_part_dx, imag_part_dy);
                    f12[d] += (s[2 * n1 - 1] * real_part_dx[0] + s[2 * n1 - 0] * imag_part_dy[0]) * z_factor * fn;
                }
                complex_product(r12[0], r12[1], real_part, imag_part);
                double xy_temp = s[2 * n1 - 1] * real_part[0] + s[2 * n1 - 0] * imag_part[0];
                for (int d = 0; d < 3; ++d) {
                    f12[d] += xy_temp * (z_factor * fnp * r12[d] + fn * dz_factor * dz[d]);
                }
            }
        }
        DoubleArrayCache.returnArray(z_pow);
    }
    
    static void accumulate_f12(int L_max, int num_L, int n, int n_max_angular_plus_1, 
                               double d12, double[] r12, double fn, double fnp, 
                               double[] Fp, double[] sum_fxyz, double[] f12) {
        double fn_original = fn;
        double fnp_original = fnp;
        double d12inv = 1.0 / d12;
        double[] r12unit = {r12[0] * d12inv, r12[1] * d12inv, r12[2] * d12inv};
        
        fnp = fnp * d12inv - fn * d12inv * d12inv;
        fn = fn * d12inv;
        if (num_L >= L_max + 2) {
            double[] s1 = {sum_fxyz[n * NUM_OF_ABC + 0], sum_fxyz[n * NUM_OF_ABC + 1], sum_fxyz[n * NUM_OF_ABC + 2]};
            get_f12_5body(d12, d12inv, fn, fnp, Fp[(L_max + 1) * n_max_angular_plus_1 + n], s1, r12, f12);
        }
        
        if (L_max >= 1) {
            double[] s1 = {0.0, 0.0, 0.0};
            calculate_s_one(1, n, n_max_angular_plus_1, Fp, sum_fxyz, s1);
            accumulate_f12_one(1, d12inv, fn_original, fnp_original, s1, r12unit, f12);
        }
        
        fnp = fnp * d12inv - fn * d12inv * d12inv;
        fn = fn * d12inv;
        if (num_L >= L_max + 1) {
            double[] s2 = {sum_fxyz[n * NUM_OF_ABC + 3], sum_fxyz[n * NUM_OF_ABC + 4], sum_fxyz[n * NUM_OF_ABC + 5], sum_fxyz[n * NUM_OF_ABC + 6], sum_fxyz[n * NUM_OF_ABC + 7]};
            get_f12_4body(d12, d12inv, fn, fnp, Fp[L_max * n_max_angular_plus_1 + n], s2, r12, f12);
        }
        
        if (L_max >= 2) {
            double[] s2 = {0.0, 0.0, 0.0, 0.0, 0.0};
            calculate_s_one(2, n, n_max_angular_plus_1, Fp, sum_fxyz, s2);
            accumulate_f12_one(2, d12inv, fn_original, fnp_original, s2, r12unit, f12);
        }
        
        if (L_max >= 3) {
            double[] s3 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            calculate_s_one(3, n, n_max_angular_plus_1, Fp, sum_fxyz, s3);
            accumulate_f12_one(3, d12inv, fn_original, fnp_original, s3, r12unit, f12);
        }
        
        if (L_max >= 4) {
            double[] s4 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            calculate_s_one(4, n, n_max_angular_plus_1, Fp, sum_fxyz, s4);
            accumulate_f12_one(4, d12inv, fn_original, fnp_original, s4, r12unit, f12);
        }
        
        if (L_max >= 5) {
            double[] s5 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            calculate_s_one(5, n, n_max_angular_plus_1, Fp, sum_fxyz, s5);
            accumulate_f12_one(5, d12inv, fn_original, fnp_original, s5, r12unit, f12);
        }
        
        if (L_max >= 6) {
            double[] s6 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            calculate_s_one(6, n, n_max_angular_plus_1, Fp, sum_fxyz, s6);
            accumulate_f12_one(6, d12inv, fn_original, fnp_original, s6, r12unit, f12);
        }
        
        if (L_max >= 7) {
            double[] s7 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            calculate_s_one(7, n, n_max_angular_plus_1, Fp, sum_fxyz, s7);
            accumulate_f12_one(7, d12inv, fn_original, fnp_original, s7, r12unit, f12);
        }
        
        if (L_max >= 8) {
            double[] s8 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            calculate_s_one(8, n, n_max_angular_plus_1, Fp, sum_fxyz, s8);
            accumulate_f12_one(8, d12inv, fn_original, fnp_original, s8, r12unit, f12);
        }
    }
    
    static void accumulate_s_one(int L, double x12, double y12, double z12, double fn, double[] s) {
        int s_index = L * L - 1;
        
        double[] z_pow = DoubleArrayCache.getArray(L+1);
        z_pow[0] = 1.0;
        for (int n = 1; n <= L; ++n) {
            z_pow[n] = z12 * z_pow[n-1];
        }
        double[] real_part = {x12};
        double[] imag_part = {y12};
        for (int n1 = 0; n1 <= L; ++n1) {
            int n2_start = (L + n1) % 2 == 0 ? 0 : 1;
            double z_factor = 0.0;
            for (int n2 = n2_start; n2 <= L - n1; n2 += 2) {
                if (L == 1) {
                    z_factor += Z_COEFFICIENT_1[n1][n2] * z_pow[n2];
                }
                if (L == 2) {
                    z_factor += Z_COEFFICIENT_2[n1][n2] * z_pow[n2];
                }
                if (L == 3) {
                    z_factor += Z_COEFFICIENT_3[n1][n2] * z_pow[n2];
                }
                if (L == 4) {
                    z_factor += Z_COEFFICIENT_4[n1][n2] * z_pow[n2];
                }
                if (L == 5) {
                    z_factor += Z_COEFFICIENT_5[n1][n2] * z_pow[n2];
                }
                if (L == 6) {
                    z_factor += Z_COEFFICIENT_6[n1][n2] * z_pow[n2];
                }
                if (L == 7) {
                    z_factor += Z_COEFFICIENT_7[n1][n2] * z_pow[n2];
                }
                if (L == 8) {
                    z_factor += Z_COEFFICIENT_8[n1][n2] * z_pow[n2];
                }
            }
            z_factor *= fn;
            if (n1 == 0) {
                s[s_index++] += z_factor;
            } else {
                s[s_index++] += z_factor * real_part[0];
                s[s_index++] += z_factor * imag_part[0];
                complex_product(x12, y12, real_part, imag_part);
            }
        }
        DoubleArrayCache.returnArray(z_pow);
    }
    
    static void accumulate_s(int L_max, double d12, double x12, double y12, double z12, double fn, double[] s) {
        double d12inv = 1.0 / d12;
        x12 *= d12inv;
        y12 *= d12inv;
        z12 *= d12inv;
        if (L_max >= 1) {
            accumulate_s_one(1, x12, y12, z12, fn, s);
        }
        if (L_max >= 2) {
            accumulate_s_one(2, x12, y12, z12, fn, s);
        }
        if (L_max >= 3) {
            accumulate_s_one(3, x12, y12, z12, fn, s);
        }
        if (L_max >= 4) {
            accumulate_s_one(4, x12, y12, z12, fn, s);
        }
        if (L_max >= 5) {
            accumulate_s_one(5, x12, y12, z12, fn, s);
        }
        if (L_max >= 6) {
            accumulate_s_one(6, x12, y12, z12, fn, s);
        }
        if (L_max >= 7) {
            accumulate_s_one(7, x12, y12, z12, fn, s);
        }
        if (L_max >= 8) {
            accumulate_s_one(8, x12, y12, z12, fn, s);
        }
    }
    
    static double find_q_one(int L, double[] s) {
        int start_index = L * L - 1;
        int num_terms = 2 * L + 1;
        double q = 0.0;
        for (int k = 1; k < num_terms; ++k) {
            q += C3B[start_index + k] * s[start_index + k] * s[start_index + k];
        }
        q *= 2.0;
        q += C3B[start_index] * s[start_index] * s[start_index];
        return q;
    }
    
    static void find_q(int L_max, int num_L, int n_max_angular_plus_1, int n, 
                       double[] s, double[] q, int shift) {
        if (L_max >= 1) {
            q[shift + 0 * n_max_angular_plus_1 + n] = find_q_one(1, s);
        }
        if (L_max >= 2) {
            q[shift + 1 * n_max_angular_plus_1 + n] = find_q_one(2, s);
        }
        if (L_max >= 3) {
            q[shift + 2 * n_max_angular_plus_1 + n] = find_q_one(3, s);
        }
        if (L_max >= 4) {
            q[shift + 3 * n_max_angular_plus_1 + n] = find_q_one(4, s);
        }
        if (L_max >= 5) {
            q[shift + 4 * n_max_angular_plus_1 + n] = find_q_one(5, s);
        }
        if (L_max >= 6) {
            q[shift + 5 * n_max_angular_plus_1 + n] = find_q_one(6, s);
        }
        if (L_max >= 7) {
            q[shift + 6 * n_max_angular_plus_1 + n] = find_q_one(7, s);
        }
        if (L_max >= 8) {
            q[shift + 7 * n_max_angular_plus_1 + n] = find_q_one(8, s);
        }
        if (num_L >= L_max + 1) {
            q[shift + L_max * n_max_angular_plus_1 + n] =
                C4B[0] * s[3] * s[3] * s[3] + C4B[1] * s[3] * (s[4] * s[4] + s[5] * s[5]) +
                C4B[2] * s[3] * (s[6] * s[6] + s[7] * s[7]) + C4B[3] * s[6] * (s[5] * s[5] - s[4] * s[4]) +
                C4B[4] * s[4] * s[5] * s[7];
        }
        if (num_L >= L_max + 2) {
            double s0_sq = s[0] * s[0];
            double s1_sq_plus_s2_sq = s[1] * s[1] + s[2] * s[2];
            q[shift + (L_max + 1) * n_max_angular_plus_1 + n] =
                C5B[0] * s0_sq * s0_sq +
                C5B[1] * s0_sq * s1_sq_plus_s2_sq +
                C5B[2] * s1_sq_plus_s2_sq * s1_sq_plus_s2_sq;
        }
    }
    
    static final int table_length = 2001;
    static final int table_segments = table_length - 1;
    static final double table_resolution = 0.0005;
    
    static void find_index_and_weight(double d12_reduced, 
                                      int[] index_left, int[] index_right, 
                                      double[] weight_left, double[] weight_right) {
        double d12_index = d12_reduced * table_segments;
        index_left[0] = (int)d12_index;
        if (index_left[0] == table_segments) {
            --index_left[0];
        }
        index_right[0] = index_left[0] + 1;
        weight_right[0] = d12_index - index_left[0];
        weight_left[0] = 1.0 - weight_right[0];
    }
    
    static void construct_table_radial_or_angular(int version, int num_types, int num_types_sq, 
                                                  int n_max, int basis_size, double rc, double rcinv, 
                                                  double[] c, int shift, double[] gn, double[] gnp) {
        double[] fn12 = DoubleArrayCache.getArray(MAX_NUM_N);
        double[] fnp12 = DoubleArrayCache.getArray(MAX_NUM_N);
        for (int table_index = 0; table_index < table_length; ++table_index) {
            double d12 = table_index * table_resolution * rc;
            double[] fc12 = {0.0}, fcp12 = {0.0};
            find_fc_and_fcp(rc, rcinv, d12, fc12, fcp12);
            for (int t1 = 0; t1 < num_types; ++t1) {
                for (int t2 = 0; t2 < num_types; ++t2) {
                    int t12 = t1 * num_types + t2;
                    find_fn_and_fnp(basis_size, rcinv, d12, fc12[0], fcp12[0], fn12, fnp12);
                    for (int n = 0; n <= n_max; ++n) {
                        double gn12 = 0.0;
                        double gnp12 = 0.0;
                        for (int k = 0; k <= basis_size; ++k) {
                            gn12 += fn12[k] * c[shift + (n * (basis_size + 1) + k) * num_types_sq + t12];
                            gnp12 += fnp12[k] * c[shift + (n * (basis_size + 1) + k) * num_types_sq + t12];
                        }
                        int index_all = (table_index * num_types_sq + t12) * (n_max + 1) + n;
                        gn[index_all] = gn12;
                        gnp[index_all] = gnp12;
                    }
                }
            }
        }
        DoubleArrayCache.returnArray(fn12);
        DoubleArrayCache.returnArray(fnp12);
    }
    
    static void find_descriptor_for_lammps(ParaMB paramb, ANN annmb, int nlocal, int N,
                                           IntCPointer g_ilist, IntCPointer g_NN, NestedIntCPointer g_NL,
                                           int[] g_type, int[] type_map, RowMatrix g_pos,
                                           double[] g_gn_radial, double[] g_gn_angular,
                                           double[] g_Fp, double[] g_sum_fxyz, double[] g_total_potential, double[] g_potential) {
        double[] q = DoubleArrayCache.getArray(MAX_DIM);
        double[] fn12 = DoubleArrayCache.getArray(MAX_NUM_N);
        double[] s = DoubleArrayCache.getArray(NUM_OF_ABC);
        double[] Fp = DoubleArrayCache.getArray(MAX_DIM), latent_space = DoubleArrayCache.getArray(MAX_NEURON);
        for (int ii = 0; ii < N; ++ii) {
            int n1 = g_ilist.getAt(ii);
            int t1 = type_map[g_type[n1]]; // from LAMMPS to NEP convention
            Arrays.fill(q, 0.0);
            
            int g_NNn1 = g_NN.getAt(n1);
            for (int i1 = 0; i1 < g_NNn1; ++i1) {
                int n2 = g_NL.getAt(n1, i1);
                double[] r12 = {g_pos.get(n2, 0) - g_pos.get(n1, 0), g_pos.get(n2, 1) - g_pos.get(n1, 1), g_pos.get(n2, 2) - g_pos.get(n1, 2)};
                double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
                if (d12sq >= paramb.rc_radial * paramb.rc_radial) {
                    continue;
                }
                double d12 = sqrt(d12sq);
                int t2 = type_map[g_type[n2]]; // from LAMMPS to NEP convention
                
                if (Conf.USE_TABLE_FOR_RADIAL_FUNCTIONS) {
                    int[] index_left = {0}, index_right = {0};
                    double[] weight_left = {0.0}, weight_right = {0.0};
                    find_index_and_weight(d12 * paramb.rcinv_radial, index_left, index_right, weight_left, weight_right);
                    int t12 = t1 * paramb.num_types + t2;
                    for (int n = 0; n <= paramb.n_max_radial; ++n) {
                        q[n] += g_gn_radial[(index_left[0] * paramb.num_types_sq + t12) * (paramb.n_max_radial + 1) + n] * weight_left[0] +
                                g_gn_radial[(index_right[0] * paramb.num_types_sq + t12) * (paramb.n_max_radial + 1) + n] * weight_right[0];
                    }
                } else {
                    double[] fc12 = {0.0};
                    double rc = paramb.rc_radial;
                    double rcinv = paramb.rcinv_radial;
                    if (paramb.use_typewise_cutoff) {
                        rc = Math.min((COVALENT_RADIUS[paramb.atomic_numbers[t1]] + COVALENT_RADIUS[paramb.atomic_numbers[t2]]) * paramb.typewise_cutoff_radial_factor, rc);
                        rcinv = 1.0 / rc;
                    }
                    find_fc(rc, rcinv, d12, fc12);
                    find_fn(paramb.basis_size_radial, rcinv, d12, fc12[0], fn12);
                    for (int n = 0; n <= paramb.n_max_radial; ++n) {
                        double gn12 = 0.0;
                        for (int k = 0; k <= paramb.basis_size_radial; ++k) {
                            int c_index = (n * (paramb.basis_size_radial + 1) + k) * paramb.num_types_sq;
                            c_index += t1 * paramb.num_types + t2;
                            gn12 += fn12[k] * annmb.c[c_index];
                        }
                        q[n] += gn12;
                    }
                }
            }
            
            for (int n = 0; n <= paramb.n_max_angular; ++n) {
                Arrays.fill(s, 0.0);
                for (int i1 = 0; i1 < g_NNn1; ++i1) {
                    int n2 = g_NL.getAt(n1, i1);
                    double[] r12 = {g_pos.get(n2, 0) - g_pos.get(n1, 0), g_pos.get(n2, 1) - g_pos.get(n1, 1), g_pos.get(n2, 2) - g_pos.get(n1, 2)};
                    double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
                    if (d12sq >= paramb.rc_angular * paramb.rc_angular) {
                        continue;
                    }
                    double d12 = sqrt(d12sq);
                    int t2 = type_map[g_type[n2]]; // from LAMMPS to NEP convention
                    
                    if (Conf.USE_TABLE_FOR_RADIAL_FUNCTIONS) {
                        int[] index_left = {0}, index_right = {0};
                        double[] weight_left = {0.0}, weight_right = {0.0};
                        find_index_and_weight(d12 * paramb.rcinv_angular, index_left, index_right, weight_left, weight_right);
                        int t12 = t1 * paramb.num_types + t2;
                        double gn12 =
                            g_gn_angular[(index_left[0] * paramb.num_types_sq + t12) * (paramb.n_max_angular + 1) + n] * weight_left[0] +
                            g_gn_angular[(index_right[0] * paramb.num_types_sq + t12) * (paramb.n_max_angular + 1) + n] * weight_right[0];
                        accumulate_s(paramb.L_max, d12, r12[0], r12[1], r12[2], gn12, s);
                    } else {
                        double[] fc12 = {0.0};
                        double rc = paramb.rc_angular;
                        double rcinv = paramb.rcinv_angular;
                        if (paramb.use_typewise_cutoff) {
                            rc = Math.min((COVALENT_RADIUS[paramb.atomic_numbers[t1]] + COVALENT_RADIUS[paramb.atomic_numbers[t2]]) * paramb.typewise_cutoff_angular_factor, rc);
                            rcinv = 1.0 / rc;
                        }
                        find_fc(rc, rcinv, d12, fc12);
                        find_fn(paramb.basis_size_angular, rcinv, d12, fc12[0], fn12);
                        double gn12 = 0.0;
                        for (int k = 0; k <= paramb.basis_size_angular; ++k) {
                            int c_index = (n * (paramb.basis_size_angular + 1) + k) * paramb.num_types_sq;
                            c_index += t1 * paramb.num_types + t2 + paramb.num_c_radial;
                            gn12 += fn12[k] * annmb.c[c_index];
                        }
                        accumulate_s(paramb.L_max, d12, r12[0], r12[1], r12[2], gn12, s);
                    }
                }
                find_q(paramb.L_max, paramb.num_L, paramb.n_max_angular + 1, n, s, q, (paramb.n_max_radial + 1));
                for (int abc = 0; abc < NUM_OF_ABC; ++abc) {
                    g_sum_fxyz[(n * NUM_OF_ABC + abc) * nlocal + n1] = s[abc];
                }
            }
            for (int d = 0; d < annmb.dim; ++d) {
                q[d] = q[d] * paramb.q_scaler[d];
            }
            double[] F = {0.0};
            Arrays.fill(Fp, 0.0);
            Arrays.fill(latent_space, 0.0);
            
            if (paramb.version == 5) {
                apply_ann_one_layer_nep5(annmb.dim, annmb.num_neurons1, annmb.w0[t1], annmb.b0[t1], annmb.w1[t1], annmb.b1, q, F, Fp, latent_space);
            } else {
                apply_ann_one_layer(annmb.dim, annmb.num_neurons1, annmb.w0[t1], annmb.b0[t1], annmb.w1[t1], annmb.b1, q, F, Fp, latent_space, false, null, 0);
            }
            
            g_total_potential[0] += F[0]; // always calculate this
            if (g_potential != null) {    // only calculate when required
                g_potential[n1] += F[0];
            }
            for (int d = 0; d < annmb.dim; ++d) {
                g_Fp[d * nlocal + n1] = Fp[d] * paramb.q_scaler[d];
            }
        }
        DoubleArrayCache.returnArray(latent_space);
        DoubleArrayCache.returnArray(Fp);
        DoubleArrayCache.returnArray(s);
        DoubleArrayCache.returnArray(fn12);
        DoubleArrayCache.returnArray(q);
    }
    
    static void find_force_radial_for_lammps(ParaMB paramb, ANN annmb, int nlocal, int N,
                                             IntCPointer g_ilist, IntCPointer g_NN, NestedIntCPointer g_NL,
                                             int[] g_type, int[] type_map, RowMatrix g_pos, double[] g_Fp,
                                             double[] g_gnp_radial,
                                             RowMatrix g_force, double[] g_total_virial, RowMatrix g_virial) {
        double[] fn12 = DoubleArrayCache.getArray(MAX_NUM_N);
        double[] fnp12 = DoubleArrayCache.getArray(MAX_NUM_N);
        for (int ii = 0; ii < N; ++ii) {
            int n1 = g_ilist.getAt(ii);
            int t1 = type_map[g_type[n1]]; // from LAMMPS to NEP convention
            int g_NNn1 = g_NN.getAt(n1);
            for (int i1 = 0; i1 < g_NNn1; ++i1) {
                int n2 = g_NL.getAt(n1, i1);
                int t2 = type_map[g_type[n2]]; // from LAMMPS to NEP convention
                double[] r12 = {g_pos.get(n2, 0) - g_pos.get(n1, 0), g_pos.get(n2, 1) - g_pos.get(n1, 1), g_pos.get(n2, 2) - g_pos.get(n1, 2)};
                double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
                if (d12sq >= paramb.rc_radial * paramb.rc_radial) {
                    continue;
                }
                double d12 = sqrt(d12sq);
                double d12inv = 1.0 / d12;
                double[] f12 = {0.0, 0.0, 0.0};
                if (Conf.USE_TABLE_FOR_RADIAL_FUNCTIONS) {
                    int[] index_left = {0}, index_right = {0};
                    double[] weight_left = {0.0}, weight_right = {0.0};
                    find_index_and_weight(d12 * paramb.rcinv_radial, index_left, index_right, weight_left, weight_right);
                    int t12 = t1 * paramb.num_types + t2;
                    for (int n = 0; n <= paramb.n_max_radial; ++n) {
                        double gnp12 =
                            g_gnp_radial[(index_left[0] * paramb.num_types_sq + t12) * (paramb.n_max_radial + 1) + n] * weight_left[0] +
                            g_gnp_radial[(index_right[0] * paramb.num_types_sq + t12) * (paramb.n_max_radial + 1) + n] * weight_right[0];
                        double tmp12 = g_Fp[n1 + n * nlocal] * gnp12 * d12inv;
                        for (int d = 0; d < 3; ++d) {
                            f12[d] += tmp12 * r12[d];
                        }
                    }
                } else {
                    double[] fc12 = {0.0}, fcp12 = {0.0};
                    double rc = paramb.rc_radial;
                    double rcinv = paramb.rcinv_radial;
                    if (paramb.use_typewise_cutoff) {
                        rc = Math.min((COVALENT_RADIUS[paramb.atomic_numbers[t1]] + COVALENT_RADIUS[paramb.atomic_numbers[t2]]) * paramb.typewise_cutoff_radial_factor, rc);
                        rcinv = 1.0 / rc;
                    }
                    find_fc_and_fcp(rc, rcinv, d12, fc12, fcp12);
                    find_fn_and_fnp(paramb.basis_size_radial, rcinv, d12, fc12[0], fcp12[0], fn12, fnp12);
                    for (int n = 0; n <= paramb.n_max_radial; ++n) {
                        double gnp12 = 0.0;
                        for (int k = 0; k <= paramb.basis_size_radial; ++k) {
                            int c_index = (n * (paramb.basis_size_radial + 1) + k) * paramb.num_types_sq;
                            c_index += t1 * paramb.num_types + t2;
                            gnp12 += fnp12[k] * annmb.c[c_index];
                        }
                        double tmp12 = g_Fp[n1 + n * nlocal] * gnp12 * d12inv;
                        for (int d = 0; d < 3; ++d) {
                            f12[d] += tmp12 * r12[d];
                        }
                    }
                }
                g_force.update(n1, 0, v -> v + f12[0]);
                g_force.update(n1, 1, v -> v + f12[1]);
                g_force.update(n1, 2, v -> v + f12[2]);
                g_force.update(n2, 0, v -> v - f12[0]);
                g_force.update(n2, 1, v -> v - f12[1]);
                g_force.update(n2, 2, v -> v - f12[2]);
                
                // always calculate the total virial:
                g_total_virial[0] -= r12[0] * f12[0]; // xx
                g_total_virial[1] -= r12[1] * f12[1]; // yy
                g_total_virial[2] -= r12[2] * f12[2]; // zz
                g_total_virial[3] -= r12[0] * f12[1]; // xy
                g_total_virial[4] -= r12[0] * f12[2]; // xz
                g_total_virial[5] -= r12[1] * f12[2]; // yz
                if (g_virial != null) {               // only calculate the per-atom virial when required
                    g_virial.update(n2, 0, v -> v - r12[0]*f12[0]); // xx
                    g_virial.update(n2, 1, v -> v - r12[1]*f12[1]); // yy
                    g_virial.update(n2, 2, v -> v - r12[2]*f12[2]); // zz
                    g_virial.update(n2, 3, v -> v - r12[0]*f12[1]); // xy
                    g_virial.update(n2, 4, v -> v - r12[0]*f12[2]); // xz
                    g_virial.update(n2, 5, v -> v - r12[1]*f12[2]); // yz
                    g_virial.update(n2, 6, v -> v - r12[1]*f12[0]); // yx
                    g_virial.update(n2, 7, v -> v - r12[2]*f12[0]); // zx
                    g_virial.update(n2, 8, v -> v - r12[2]*f12[1]); // zy
                }
            }
        }
        DoubleArrayCache.returnArray(fn12);
        DoubleArrayCache.returnArray(fnp12);
    }
    
    static void find_force_angular_for_lammps(ParaMB paramb, ANN annmb, int nlocal, int N,
                                              IntCPointer g_ilist, IntCPointer g_NN, NestedIntCPointer g_NL,
                                              int[] g_type, int[] type_map, RowMatrix g_pos, double[] g_Fp, double[] g_sum_fxyz,
                                              double[] g_gn_angular, double[] g_gnp_angular,
                                              RowMatrix g_force, double[] g_total_virial, RowMatrix g_virial) {
        double[] Fp = DoubleArrayCache.getArray(MAX_DIM_ANGULAR);
        double[] sum_fxyz = DoubleArrayCache.getArray(NUM_OF_ABC * MAX_NUM_N);
        double[] fn12 = DoubleArrayCache.getArray(MAX_NUM_N);
        double[] fnp12 = DoubleArrayCache.getArray(MAX_NUM_N);
        for (int ii = 0; ii < N; ++ii) {
            int n1 = g_ilist.getAt(ii);
            for (int d = 0; d < paramb.dim_angular; ++d) {
                Fp[d] = g_Fp[(paramb.n_max_radial + 1 + d) * nlocal + n1];
            }
            for (int d = 0; d < (paramb.n_max_angular + 1) * NUM_OF_ABC; ++d) {
                sum_fxyz[d] = g_sum_fxyz[d * nlocal + n1];
            }
            
            int t1 = type_map[g_type[n1]]; // from LAMMPS to NEP convention
            
            int g_NNn1 = g_NN.getAt(n1);
            for (int i1 = 0; i1 < g_NNn1; ++i1) {
                int n2 = g_NL.getAt(n1, i1);
                double[] r12 = {g_pos.get(n2, 0) - g_pos.get(n1, 0), g_pos.get(n2, 1) - g_pos.get(n1, 1), g_pos.get(n2, 2) - g_pos.get(n1, 2)};
                double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
                if (d12sq >= paramb.rc_angular * paramb.rc_angular) {
                    continue;
                }
                double d12 = sqrt(d12sq);
                int t2 = type_map[g_type[n2]]; // from LAMMPS to NEP convention
                double[] f12 = {0.0, 0.0, 0.0};
                
                if (Conf.USE_TABLE_FOR_RADIAL_FUNCTIONS) {
                    int[] index_left = {0}, index_right = {0};
                    double[] weight_left = {0.0}, weight_right = {0.0};
                    find_index_and_weight(d12 * paramb.rcinv_angular, index_left, index_right, weight_left, weight_right);
                    int t12 = t1 * paramb.num_types + t2;
                    for (int n = 0; n <= paramb.n_max_angular; ++n) {
                        int index_left_all = (index_left[0] * paramb.num_types_sq + t12) * (paramb.n_max_angular + 1) + n;
                        int index_right_all = (index_right[0] * paramb.num_types_sq + t12) * (paramb.n_max_angular + 1) + n;
                        double gn12 = g_gn_angular[index_left_all] * weight_left[0] + g_gn_angular[index_right_all] * weight_right[0];
                        double gnp12 = g_gnp_angular[index_left_all] * weight_left[0] + g_gnp_angular[index_right_all] * weight_right[0];
                        accumulate_f12(paramb.L_max, paramb.num_L, n, paramb.n_max_angular + 1, d12, r12, gn12, gnp12, Fp, sum_fxyz, f12);
                    }
                } else {
                    double[] fc12 = {0.0}, fcp12 = {0.0};
                    double rc = paramb.rc_angular;
                    double rcinv = paramb.rcinv_angular;
                    if (paramb.use_typewise_cutoff) {
                        rc = Math.min((COVALENT_RADIUS[paramb.atomic_numbers[t1]] + COVALENT_RADIUS[paramb.atomic_numbers[t2]]) * paramb.typewise_cutoff_angular_factor, rc);
                        rcinv = 1.0 / rc;
                    }
                    find_fc_and_fcp(rc, rcinv, d12, fc12, fcp12);
                    find_fn_and_fnp(paramb.basis_size_angular, rcinv, d12, fc12[0], fcp12[0], fn12, fnp12);
                    for (int n = 0; n <= paramb.n_max_angular; ++n) {
                        double gn12 = 0.0;
                        double gnp12 = 0.0;
                        for (int k = 0; k <= paramb.basis_size_angular; ++k) {
                            int c_index = (n * (paramb.basis_size_angular + 1) + k) * paramb.num_types_sq;
                            c_index += t1 * paramb.num_types + t2 + paramb.num_c_radial;
                            gn12 += fn12[k] * annmb.c[c_index];
                            gnp12 += fnp12[k] * annmb.c[c_index];
                        }
                        accumulate_f12(paramb.L_max, paramb.num_L, n, paramb.n_max_angular + 1, d12, r12, gn12, gnp12, Fp, sum_fxyz, f12);
                    }
                }
                g_force.update(n1, 0, v -> v + f12[0]);
                g_force.update(n1, 1, v -> v + f12[1]);
                g_force.update(n1, 2, v -> v + f12[2]);
                g_force.update(n2, 0, v -> v - f12[0]);
                g_force.update(n2, 1, v -> v - f12[1]);
                g_force.update(n2, 2, v -> v - f12[2]);
                // always calculate the total virial:
                g_total_virial[0] -= r12[0] * f12[0]; // xx
                g_total_virial[1] -= r12[1] * f12[1]; // yy
                g_total_virial[2] -= r12[2] * f12[2]; // zz
                g_total_virial[3] -= r12[0] * f12[1]; // xy
                g_total_virial[4] -= r12[0] * f12[2]; // xz
                g_total_virial[5] -= r12[1] * f12[2]; // yz
                if (g_virial != null) {               // only calculate the per-atom virial when required
                    g_virial.update(n2, 0, v -> v - r12[0]*f12[0]); // xx
                    g_virial.update(n2, 1, v -> v - r12[1]*f12[1]); // yy
                    g_virial.update(n2, 2, v -> v - r12[2]*f12[2]); // zz
                    g_virial.update(n2, 3, v -> v - r12[0]*f12[1]); // xy
                    g_virial.update(n2, 4, v -> v - r12[0]*f12[2]); // xz
                    g_virial.update(n2, 5, v -> v - r12[1]*f12[2]); // yz
                    g_virial.update(n2, 6, v -> v - r12[1]*f12[0]); // yx
                    g_virial.update(n2, 7, v -> v - r12[2]*f12[0]); // zx
                    g_virial.update(n2, 8, v -> v - r12[2]*f12[1]); // zy
                }
            }
        }
        DoubleArrayCache.returnArray(Fp);
        DoubleArrayCache.returnArray(sum_fxyz);
        DoubleArrayCache.returnArray(fn12);
        DoubleArrayCache.returnArray(fnp12);
    }
    
    static void find_force_ZBL_for_lammps(ParaMB paramb, ZBL zbl, int N,
                                          IntCPointer g_ilist, IntCPointer g_NN, NestedIntCPointer g_NL,
                                          int[] g_type, int[] type_map, RowMatrix g_pos, RowMatrix g_force,
                                          double[] g_total_virial, RowMatrix g_virial, double[] g_total_potential, double[] g_potential) {
        for (int ii = 0; ii < N; ++ii) {
            int n1 = g_ilist.getAt(ii);
            int type1 = type_map[g_type[n1]]; // from LAMMPS to NEP convention
            int zi = paramb.atomic_numbers[type1] + 1;
            double pow_zi = pow(zi, 0.23);
            int g_NNn1 = g_NN.getAt(n1);
            for (int i1 = 0; i1 < g_NNn1; ++i1) {
                int n2 = g_NL.getAt(n1, i1);
                double[] r12 = {g_pos.get(n2, 0) - g_pos.get(n1, 0), g_pos.get(n2, 1) - g_pos.get(n1, 1), g_pos.get(n2, 2) - g_pos.get(n1, 2)};
                double d12sq = r12[0] * r12[0] + r12[1] * r12[1] + r12[2] * r12[2];
                double max_rc_outer = 2.5;
                if (d12sq >= max_rc_outer * max_rc_outer) {
                    continue;
                }
                double d12 = sqrt(d12sq);
                
                double d12inv = 1.0 / d12;
                double[] f = {0.0}, fp = {0.0};
                int type2 = type_map[g_type[n2]]; // from LAMMPS to NEP convention
                int zj = paramb.atomic_numbers[type2] + 1;
                double a_inv = (pow_zi + pow(zj, 0.23)) * 2.134563;
                double zizj = K_C_SP * zi * zj;
                if (zbl.flexibled) {
                    int t1, t2;
                    if (type1 < type2) {
                        t1 = type1;
                        t2 = type2;
                    } else {
                        t1 = type2;
                        t2 = type1;
                    }
                    int zbl_index = t1 * zbl.num_types - (t1 * (t1 - 1)) / 2 + (t2 - t1);
                    double[] ZBL_para = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
                    for (int i = 0; i < 10; ++i) {
                        ZBL_para[i] = zbl.para[10 * zbl_index + i];
                    }
                    find_f_and_fp_zbl(ZBL_para, zizj, a_inv, d12, d12inv, f, fp);
                } else {
                    double rc_inner = zbl.rc_inner;
                    double rc_outer = zbl.rc_outer;
                    if (paramb.use_typewise_cutoff_zbl) {
                        // zi and zj start from 1, so need to minus 1 here
                        rc_outer = Math.min((COVALENT_RADIUS[zi - 1] + COVALENT_RADIUS[zj - 1]) * paramb.typewise_cutoff_zbl_factor, rc_outer);
                        rc_inner = rc_outer * 0.5f;
                    }
                    find_f_and_fp_zbl(zizj, a_inv, rc_inner, rc_outer, d12, d12inv, f, fp);
                }
                double f2 = fp[0] * d12inv * 0.5;
                double[] f12 = {r12[0] * f2, r12[1] * f2, r12[2] * f2};
                g_force.update(n1, 0, v -> v + f12[0]); // accumulation here
                g_force.update(n1, 1, v -> v + f12[1]);
                g_force.update(n1, 2, v -> v + f12[2]);
                g_force.update(n2, 0, v -> v - f12[0]);
                g_force.update(n2, 1, v -> v - f12[1]);
                g_force.update(n2, 2, v -> v - f12[2]);
                // always calculate the total virial:
                g_total_virial[0] -= r12[0] * f12[0]; // xx
                g_total_virial[1] -= r12[1] * f12[1]; // yy
                g_total_virial[2] -= r12[2] * f12[2]; // zz
                g_total_virial[3] -= r12[0] * f12[1]; // xy
                g_total_virial[4] -= r12[0] * f12[2]; // xz
                g_total_virial[5] -= r12[1] * f12[2]; // yz
                if (g_virial != null) {               // only calculate the per-atom virial when required
                    g_virial.update(n2, 0, v -> v - r12[0]*f12[0]); // xx
                    g_virial.update(n2, 1, v -> v - r12[1]*f12[1]); // yy
                    g_virial.update(n2, 2, v -> v - r12[2]*f12[2]); // zz
                    g_virial.update(n2, 3, v -> v - r12[0]*f12[1]); // xy
                    g_virial.update(n2, 4, v -> v - r12[0]*f12[2]); // xz
                    g_virial.update(n2, 5, v -> v - r12[1]*f12[2]); // yz
                    g_virial.update(n2, 6, v -> v - r12[1]*f12[0]); // yx
                    g_virial.update(n2, 7, v -> v - r12[2]*f12[0]); // zx
                    g_virial.update(n2, 8, v -> v - r12[2]*f12[1]); // zy
                }
                g_total_potential[0] += f[0] * 0.5; // always calculate this
                if (g_potential != null) {    // only calculate when required
                    g_potential[n1] += f[0] * 0.5;
                }
            }
        }
    }
    
    static String[] get_tokens(BufferedReader input) throws IOException {
        String line = input.readLine();
        if (line == null) return ZL_STR;
        return IO.Text.splitBlank(line);
    }
    
    static void print_tokens(String[] tokens) {
        System.err.print("Line:");
        for (String token : tokens) {
            System.err.print(" ");
            System.err.print(token);
        }
        System.err.println();
    }
}
