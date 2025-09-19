package jse.code;

import com.google.common.collect.ImmutableMap;
import jse.atom.AbstractXYZ;
import jse.atom.IXYZ;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.random.IRandom;
import jse.math.SliceType;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jse.parallel.CompletedFuture;
import org.jetbrains.annotations.ApiStatus;

import java.awt.*;
import java.io.*;
import java.nio.file.OpenOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * 包含许多全局常量（constants）的工具类
 * @see UT UT: 通用方法工具类
 * @see IO IO: 文件操作工具类
 * @author liqa
 */
public class CS {
    /** version of jse */
    public final static String VERSION = "3.11.1f";
    public final static int VERSION_NUMBER = 3_11_01_05;
    
    /** 内部使用的全局随机数生成器 */
    @ApiStatus.Internal public final static Random RANDOM_ = new Random();
    /** 全局的随机数生成器，可以通过 {@link UT.Math#rng(long)} 来控制全局的随机流 */
    public final static IRandom RANDOM = IRandom.of(RANDOM_);
    /** {@link UT.Code#randSeed} 生成的最大种子数（900000000），主要用于 lammps 输入，这是因为 Marsaglia 随机数生成器需要一个较小的种子输入 */
    public final static int MAX_SEED = 900000000;
    
    /** 全为 0 的 {@link IXYZ} 常量，不可修改 */
    public final static IXYZ XYZ_ZERO = new AbstractXYZ() {
        @Override public double x() {return 0.0;}
        @Override public double y() {return 0.0;}
        @Override public double z() {return 0.0;}
    };
    
    /** {@link jse.math.MathEX} 中的使用常量，用于数组切片时传入表明这个维度全保留 */
    public final static SliceType ALL = SliceType.ALL;
    
    /** Sleep time stuff, ms */
    public final static long
          INTERNAL_SLEEP_TIME = 1
        , SYNC_SLEEP_TIME = 10
        , SYNC_SLEEP_TIME_2 = 20
        , FILE_SYSTEM_SLEEP_TIME = 100
        , FILE_SYSTEM_SLEEP_TIME_2 = 200
        , SSH_SLEEP_TIME = 500
        , SSH_SLEEP_TIME_2 = 1000
        , SYNC_TIMEOUT = 500
        , FILE_SYSTEM_TIMEOUT = 10000
        ;
    
    
    /// AtomData stuffs
    /** {@code ["x", "y", "z", "id", "type", "vx", "vy", "vz"]} */
    public final static String[] ATOM_DATA_KEYS = {"x", "y", "z", "id", "type", "vx", "vy", "vz"};
    /** {@code ["vx", "vy", "vz"]} */
    public final static String[] ATOM_DATA_KEYS_VELOCITY = {"vx", "vy", "vz"};
    /** {@code ["type", "id1", "id2"]} */
    public final static String[] ATOM_DATA_KEYS_BOND = {"type", "id1", "id2"};
    /** {@code ["x", "y", "z"]} */
    public final static String[] ATOM_DATA_KEYS_XYZ = {"x", "y", "z"};
    /** {@code ["x", "y", "z", "id"]} */
    public final static String[] ATOM_DATA_KEYS_XYZID = {"x", "y", "z", "id"};
    /** {@code ["type", "x", "y", "z"]} */
    public final static String[] ATOM_DATA_KEYS_TYPE_XYZ = {"type", "x", "y", "z"};
    /** {@code ["id", "type", "x", "y", "z"]} */
    public final static String[] ATOM_DATA_KEYS_ID_TYPE_XYZ = {"id", "type", "x", "y", "z"};
    /** {@code ["id", "type", "mol", "charge", "x", "y", "z"]} */
    public final static String[] ATOM_DATA_KEYS_ID_TYPE_MOL_CHARGE_XYZ = {"id", "type", "mol", "charge", "x", "y", "z"};
    /** {@code ["id", "type", "x", "y", "z", "vx", "vy", "vz"]} */
    public final static String[] ALL_ATOM_DATA_KEYS = {"id", "type", "x", "y", "z", "vx", "vy", "vz"};
    /** {@link #ATOM_DATA_KEYS_ID_TYPE_XYZ} */
    public final static String[] STD_ATOM_DATA_KEYS = ATOM_DATA_KEYS_ID_TYPE_XYZ; // 标准 AtomData 包含信息格式为 id type x y z
    /** index of {@link #ATOM_DATA_KEYS} */
    public final static int DATA_X_COL = 0, DATA_Y_COL = 1, DATA_Z_COL = 2, DATA_ID_COL = 3, DATA_TYPE_COL = 4, DATA_VX_COL = 5, DATA_VY_COL = 6, DATA_VZ_COL = 7;
    /** index of {@link #ATOM_DATA_KEYS_XYZ} */
    public final static int XYZ_X_COL = 0, XYZ_Y_COL = 1, XYZ_Z_COL = 2;
    /** index of {@link #ATOM_DATA_KEYS_XYZID} */
    public final static int XYZID_X_COL = 0, XYZID_Y_COL = 1, XYZID_Z_COL = 2, XYZID_ID_COL = 3;
    /** index of {@link #ATOM_DATA_KEYS_TYPE_XYZ} */
    public final static int TYPE_XYZ_TYPE_COL = 0, TYPE_XYZ_X_COL = 1, TYPE_XYZ_Y_COL = 2, TYPE_XYZ_Z_COL = 3;
    /** index of {@link #ALL_ATOM_DATA_KEYS} */
    public final static int ALL_ID_COL = 0, ALL_TYPE_COL = 1, ALL_X_COL = 2, ALL_Y_COL = 3, ALL_Z_COL = 4, ALL_VX_COL = 5, ALL_VY_COL = 6, ALL_VZ_COL = 7;
    /** index of {@link #STD_ATOM_DATA_KEYS} */
    public final static int STD_ID_COL = 0, STD_TYPE_COL = 1, STD_X_COL = 2, STD_Y_COL = 3, STD_Z_COL = 4;
    /** index of {@link #ATOM_DATA_KEYS_VELOCITY} */
    public final static int STD_VX_COL = 0, STD_VY_COL = 1, STD_VZ_COL = 2;
    /** index of {@link #ATOM_DATA_KEYS_BOND} */
    public final static int STD_BOND_TYPE_COL = 0, STD_BOND_ID1_COL = 1, STD_BOND_ID2_COL = 2;
    
    /** {@link jse.atom.AtomicParameterCalculator} 参数计算默认使用的截断半径倍率 */
    public final static double R_NEAREST_MUL = 1.5;
    
    /// const arrays
    /** {@code OpenOption[0]} */
    public final static OpenOption[] ZL_OO = new OpenOption[0];
    /** {@code String[0]} */
    public final static String[] ZL_STR = new String[0];
    /** {@code Object[0]} */
    public final static Object[] ZL_OBJ = new Object[0];
    /** {@code double[0][]} */
    public final static double[][] ZL_DOUBLE_BI = new double[0][];
    /** {@code double[0]} */
    public final static double[] ZL_DOUBLE = new double[0];
    /** {@code float[0]} */
    public final static float[]   ZL_FLOAT = new float[0];
    /** {@code int[0]} */
    public final static int[]       ZL_INT = new int[0];
    /** {@code long[0]} */
    public final static long[]     ZL_LONG = new long[0];
    /** {@code short[0]} */
    public final static short[]   ZL_SHORT = new short[0];
    /** {@code byte[0]} */
    public final static byte[]     ZL_BYTE = new byte[0];
    /** {@code boolean[0]} */
    public final static boolean[]  ZL_BOOL = new boolean[0];
    /** {@code Vectors.zeros(0)} */
    public final static Vector ZL_VEC = Vectors.zeros(0);
    /** {@code Matrices.zeros(0)} */
    public final static RowMatrix ZL_MAT = Matrices.zeros(0);
    
    /// Patterns
    /** 匹配空字符串或者空格的正则表达式，{@code \s*} */
    public final static Pattern BLANKS_OR_EMPTY = Pattern.compile("\\s*");
    /** 匹配空字符串的正则表达式，{@code \s+} */
    public final static Pattern BLANKS = Pattern.compile("\\s+");
    /** 匹配周围有任意空格的逗号的正则表达式，{@code \s*,\s*} */
    public final static Pattern COMMA = Pattern.compile("\\s*,\\s*");
    /** 匹配周围有任意空格的逗号或者空格的正则表达式，{@code \s*[,\s]\s*} */
    public final static Pattern COMMA_OR_BLANKS = Pattern.compile("\\s*[,\\s]\\s*");
    
    /** units from ase, use 2018 codata version */
    public final static Map<String, Double> UNITS = (new ImmutableMap.Builder<String, Double>())
        .put("_c", 299792458.0)
        .put("_mu0", 1.2566370614359173e-06)
        .put("_Grav", 6.6743e-11)
        .put("_hplanck", 6.62607015e-34)
        .put("_e", 1.602176634e-19)
        .put("_me", 9.1093837015e-31)
        .put("_mp", 1.67262192369e-27)
        .put("_Nav", 6.02214076e+23)
        .put("_k", 1.380649e-23)
        .put("_amu", 1.6605390666e-27)
        .put("_eps0", 8.85418781762039e-12)
        .put("_hbar", 1.0545718176461565e-34)
        .put("Ang", 1.0)
        .put("Angstrom", 1.0)
        .put("nm", 10.0)
        .put("Bohr", 0.5291772111941798)
        .put("eV", 1.0)
        .put("Hartree", 27.21138621614287)
        .put("kJ", 6.241509074460763e+21)
        .put("kcal", 2.6114473967543833e+22)
        .put("mol", 6.02214076e+23)
        .put("Rydberg", 13.605693108071435)
        .put("Ry", 13.605693108071435)
        .put("Ha", 27.21138621614287)
        .put("second", 98226947502532.77)
        .put("fs", 0.09822694750253277)
        .put("kB", 8.617333262145179e-05)
        .put("Pascal", 6.241509074460763e-12)
        .put("GPa", 0.006241509074460762)
        .put("bar", 6.241509074460762e-07)
        .put("Debye", 0.20819433270935597)
        .put("alpha", 0.0072973525653052115)
        .put("invcm", 0.0001239841984332003)
        .put("_aut", 2.4188843292387268e-17)
        .put("_auv", 2187691.262445455)
        .put("_auf", 8.238723484684572e-08)
        .put("_aup", 29421015615686.684)
        .put("AUT", 0.002375996240228316)
        .put("m", 10000000000.0)
        .put("kg", 6.022140762081123e+26)
        .put("s", 98226947502532.77)
        .put("A", 63541.71877630451)
        .put("J", 6.241509074460763e+18)
        .put("C", 6.241509074460763e+18)
        .build();
    
    /** units from ase, use 2014 codata version (ase default value) */
    public final static Map<String, Double> UNITS_ASE = (new ImmutableMap.Builder<String, Double>())
        .put("_c", 299792458.0)
        .put("_mu0", 1.2566370614359173e-06)
        .put("_Grav", 6.67408e-11)
        .put("_hplanck", 6.62607004e-34)
        .put("_e", 1.6021766208e-19)
        .put("_me", 9.10938356e-31)
        .put("_mp", 1.672621898e-27)
        .put("_Nav", 6.022140857e+23)
        .put("_k", 1.38064852e-23)
        .put("_amu", 1.66053904e-27)
        .put("_eps0", 8.85418781762039e-12)
        .put("_hbar", 1.0545718001391127e-34)
        .put("Ang", 1.0)
        .put("Angstrom", 1.0)
        .put("nm", 10.0)
        .put("Bohr", 0.5291772105638411)
        .put("eV", 1.0)
        .put("Hartree", 27.211386024367243)
        .put("kJ", 6.241509125883258e+21)
        .put("kcal", 2.611447418269555e+22)
        .put("mol", 6.022140857e+23)
        .put("Rydberg", 13.605693012183622)
        .put("Ry", 13.605693012183622)
        .put("Ha", 27.211386024367243)
        .put("second", 98226947884640.62)
        .put("fs", 0.09822694788464063)
        .put("kB", 8.617330337217213e-05)
        .put("Pascal", 6.241509125883258e-12)
        .put("GPa", 0.006241509125883258)
        .put("bar", 6.241509125883258e-07)
        .put("Debye", 0.20819433442462576)
        .put("alpha", 0.007297352566206496)
        .put("invcm", 0.0001239841973964072)
        .put("_aut", 2.418884326058678e-17)
        .put("_auv", 2187691.262715653)
        .put("_auf", 8.238723368557715e-08)
        .put("_aup", 29421015271080.86)
        .put("AUT", 0.0023759962463473982)
        .put("m", 10000000000.0)
        .put("kg", 6.0221408585491615e+26)
        .put("s", 98226947884640.62)
        .put("A", 63541.719052630964)
        .put("J", 6.241509125883258e+18)
        .put("C", 6.241509125883258e+18)
        .build();
    
    /** Boltzmann constant, eV/K */
    public final static double K_B = UNITS.get("kB"); // eV / K
    /** Reduced Planck constant, eV*ps */
    public final static double H_BAR = UNITS.get("_hbar") / UNITS.get("_e") * 1e12; // eV * ps
    /** Avogadro constant */
    public final static double N_A = UNITS.get("_Nav");
    /** Electron volt, {@code g * Å^2 / ps^2 == 0.1J} */
    public final static double E_V = UNITS.get("_e") * 0.1; // g * Å^2 / ps^2 == 0.1J
    /** Electron volt to Joule */
    public final static double EV_TO_J = UNITS.get("_e");
    /** Electron volt to kcal/mol */
    public final static double EV_TO_KCAL = UNITS.get("mol") / UNITS.get("kcal");
    
    /** All atom name, start from 0, {@code SYMBOLS[0] == "H"} */
    public final static String[] SYMBOLS = {
        "H" , "He",
        "Li", "Be", "B" , "C" , "N" , "O" , "F" , "Ne",
        "Na", "Mg", "Al", "Si", "P" , "S" , "Cl", "Ar",
        "K" , "Ca", "Sc", "Ti", "V" , "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr",
        "Rb", "Sr", "Y" , "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I" , "Xe",
        "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf",
        "Ta", "W" , "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn",
        "Fr", "Ra", "Ac", "Th", "Pa", "U" , "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr", "Rf",
        "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Cn", "Nh", "Fl", "Mc", "Lv", "Ts", "Og"
    };
    /** @deprecated use {@link #SYMBOLS} */
    public final static @Deprecated String[] ATOM_TYPE_NAMES = SYMBOLS;
    /** convert atomic number to symbol, {@code ATOMIC_NUMBER_TO_SYMBOL[1] == "H"} */
    public final static List<String> ATOMIC_NUMBER_TO_SYMBOL = new AbstractRandomAccessList<String>() {
        @Override public String get(int index) {return SYMBOLS[index-1];}
        @Override public int size() {return SYMBOLS.length+1;}
    };
    /** convert symbol to atomic number, {@code SYMBOL_TO_ATOMIC_NUMBER["H"] == 1} */
    public final static Map<String, Integer> SYMBOL_TO_ATOMIC_NUMBER = (new ImmutableMap.Builder<String, Integer>())
        .put("H" , 1)
        .put("He", 2)
        .put("Li", 3)
        .put("Be", 4)
        .put("B" , 5)
        .put("C" , 6)
        .put("N" , 7)
        .put("O" , 8)
        .put("F" , 9)
        .put("Ne", 10)
        .put("Na", 11)
        .put("Mg", 12)
        .put("Al", 13)
        .put("Si", 14)
        .put("P" , 15)
        .put("S" , 16)
        .put("Cl", 17)
        .put("Ar", 18)
        .put("K" , 19)
        .put("Ca", 20)
        .put("Sc", 21)
        .put("Ti", 22)
        .put("V" , 23)
        .put("Cr", 24)
        .put("Mn", 25)
        .put("Fe", 26)
        .put("Co", 27)
        .put("Ni", 28)
        .put("Cu", 29)
        .put("Zn", 30)
        .put("Ga", 31)
        .put("Ge", 32)
        .put("As", 33)
        .put("Se", 34)
        .put("Br", 35)
        .put("Kr", 36)
        .put("Rb", 37)
        .put("Sr", 38)
        .put("Y" , 39)
        .put("Zr", 40)
        .put("Nb", 41)
        .put("Mo", 42)
        .put("Tc", 43)
        .put("Ru", 44)
        .put("Rh", 45)
        .put("Pd", 46)
        .put("Ag", 47)
        .put("Cd", 48)
        .put("In", 49)
        .put("Sn", 50)
        .put("Sb", 51)
        .put("Te", 52)
        .put("I" , 53)
        .put("Xe", 54)
        .put("Cs", 55)
        .put("Ba", 56)
        .put("La", 57)
        .put("Ce", 58)
        .put("Pr", 59)
        .put("Nd", 60)
        .put("Pm", 61)
        .put("Sm", 62)
        .put("Eu", 63)
        .put("Gd", 64)
        .put("Tb", 65)
        .put("Dy", 66)
        .put("Ho", 67)
        .put("Er", 68)
        .put("Tm", 69)
        .put("Yb", 70)
        .put("Lu", 71)
        .put("Hf", 72)
        .put("Ta", 73)
        .put("W" , 74)
        .put("Re", 75)
        .put("Os", 76)
        .put("Ir", 77)
        .put("Pt", 78)
        .put("Au", 79)
        .put("Hg", 80)
        .put("Tl", 81)
        .put("Pb", 82)
        .put("Bi", 83)
        .put("Po", 84)
        .put("At", 85)
        .put("Rn", 86)
        .put("Fr", 87)
        .put("Ra", 88)
        .put("Ac", 89)
        .put("Th", 90)
        .put("Pa", 91)
        .put("U" , 92)
        .put("Np", 93)
        .put("Pu", 94)
        .put("Am", 95)
        .put("Cm", 96)
        .put("Bk", 97)
        .put("Cf", 98)
        .put("Es", 99)
        .put("Fm", 100)
        .put("Md", 101)
        .put("No", 102)
        .put("Lr", 103)
        .put("Rf", 104)
        .put("Db", 105)
        .put("Sg", 106)
        .put("Bh", 107)
        .put("Hs", 108)
        .put("Mt", 109)
        .put("Ds", 110)
        .put("Rg", 111)
        .put("Cn", 112)
        .put("Nh", 113)
        .put("Fl", 114)
        .put("Mc", 115)
        .put("Lv", 116)
        .put("Ts", 117)
        .put("Og", 118)
        .build();
    
    /** All atomic mass, {@code MASS["H"] == 1.00794} */
    public final static Map<String, Double> MASS = (new ImmutableMap.Builder<String, Double>())
        .put("H" , 1.00794)
        .put("He", 4.002602)
        .put("Li", 6.941)
        .put("Be", 9.012182)
        .put("B" , 10.811)
        .put("C" , 12.0107)
        .put("N" , 14.0067)
        .put("O" , 15.9994)
        .put("F" , 18.998403)
        .put("Ne", 20.1797)
        .put("Na", 22.98976)
        .put("Mg", 24.3050)
        .put("Al", 26.98153)
        .put("Si", 28.0855)
        .put("P" , 30.97696)
        .put("S" , 32.065)
        .put("Cl", 35.453)
        .put("Ar", 39.948)
        .put("K" , 39.0983)
        .put("Ca", 40.078)
        .put("Sc", 44.95591)
        .put("Ti", 47.867)
        .put("V" , 50.9415)
        .put("Cr", 51.9962)
        .put("Mn", 54.93804)
        .put("Fe", 55.845)
        .put("Co", 58.93319)
        .put("Ni", 58.6934)
        .put("Cu", 63.546)
        .put("Zn", 65.38)
        .put("Ga", 69.723)
        .put("Ge", 72.64)
        .put("As", 74.92160)
        .put("Se", 78.96)
        .put("Br", 79.904)
        .put("Kr", 83.798)
        .put("Rb", 85.4678)
        .put("Sr", 87.62)
        .put("Y" , 88.90585)
        .put("Zr", 91.224)
        .put("Nb", 92.90638)
        .put("Mo", 95.96)
        .put("Tc", 98.0)
        .put("Ru", 101.07)
        .put("Rh", 102.9055)
        .put("Pd", 106.42)
        .put("Ag", 107.8682)
        .put("Cd", 112.441)
        .put("In", 114.818)
        .put("Sn", 118.710)
        .put("Sb", 121.760)
        .put("Te", 127.60)
        .put("I" , 126.9044)
        .put("Xe", 131.293)
        .put("Cs", 132.9054)
        .put("Ba", 137.327)
        .put("La", 138.9054)
        .put("Ce", 140.116)
        .put("Pr", 140.9076)
        .put("Nd", 144.242)
        .put("Pm", 145.0)
        .put("Sm", 150.36)
        .put("Eu", 151.964)
        .put("Gd", 157.25)
        .put("Tb", 158.9253)
        .put("Dy", 162.500)
        .put("Ho", 164.9303)
        .put("Er", 167.259)
        .put("Tm", 168.9342)
        .put("Yb", 173.054)
        .put("Lu", 174.9668)
        .put("Hf", 178.49)
        .put("Ta", 180.9478)
        .put("W" , 183.84)
        .put("Re", 186.207)
        .put("Os", 190.23)
        .put("Ir", 192.217)
        .put("Pt", 195.084)
        .put("Au", 196.9665)
        .put("Hg", 200.59)
        .put("Tl", 20.3833)
        .put("Pb", 207.2)
        .put("Bi", 208.9804)
        .put("Po", 210.0)
        .put("At", 210.0)
        .put("Rn", 220.0)
        .put("Fr", 223.0)
        .put("Ra", 226.0)
        .put("Ac", 227.0)
        .put("Th", 232.0380)
        .put("Pa", 231.0358)
        .put("U" , 238.0289)
        .put("Np", 237.0)
        .put("Pu", 244.0)
        .put("Am", 243.0)
        .put("Cm", 247.0)
        .put("Bk", 247.0)
        .put("Cf", 251.0)
        .put("Es", 252.0)
        .put("Fm", 257.0)
        .put("Md", 258.0)
        .put("No", 259.0)
        .put("Lr", 262.0)
        .put("Rf", 261.0)
        .put("Db", 262.0)
        .put("Sg", 266.0)
        .put("Bh", 264.0)
        .put("Hs", 277.0)
        .put("Mt", 268.0)
        .put("Ds", 271.0)
        .put("Rg", 272.0)
        .put("Cn", 285.0)
        .put("Nh", 286.0)
        .put("Fl", 289.0)
        .put("Mc", 290.0)
        .put("Lv", 293.0)
        .put("Ts", 294.0)
        .put("Og", 294.0)
        .build();
    /** All atomic color, from ovito, {@code COLOR["H"] == new Color(0xffffff)} */
    public final static Map<String, Color> COLOR = (new ImmutableMap.Builder<String, Color>())
        .put("H" , new Color(0xffffff))
        .put("He", new Color(0xd9ffff))
        .put("Li", new Color(0xcc80ff))
        .put("Be", new Color(0xc2ff00))
        .put("B" , new Color(0xffb5b5))
        .put("C" , new Color(0x909090))
        .put("N" , new Color(0x3050f8))
        .put("O" , new Color(0xff0d0d))
        .put("F" , new Color(0x80b3ff))
        .put("Ne", new Color(0xb3e3f5))
        .put("Na", new Color(0xab5cf2))
        .put("Mg", new Color(0x8aff00))
        .put("Al", new Color(0xbfa6a6))
        .put("Si", new Color(0xf0c8a0))
        .put("P" , new Color(0xff8000))
        .put("S" , new Color(0xb3b300))
        .put("Cl", new Color(0x1ff01f))
        .put("Ar", new Color(0x80d1e3))
        .put("K" , new Color(0x8f40d4))
        .put("Ca", new Color(0x3dff00))
        .put("Sc", new Color(0xe6e6e6))
        .put("Ti", new Color(0xbfc2c7))
        .put("V" , new Color(0xa6a6ab))
        .put("Cr", new Color(0x8a99c7))
        .put("Mn", new Color(0x9c7ac7))
        .put("Fe", new Color(0xe06633))
        .put("Co", new Color(0xf090a0))
        .put("Ni", new Color(0x50d050))
        .put("Cu", new Color(0xc88033))
        .put("Zn", new Color(0x7d80b0))
        .put("Ga", new Color(0xc28f8f))
        .put("Ge", new Color(0x668f8f))
        .put("As", new Color(0xbd80e3))
        .put("Se", new Color(0xffa100))
        .put("Br", new Color(0xa62929))
        .put("Kr", new Color(0x5cb8d1))
        .put("Rb", new Color(0x702eb0))
        .put("Sr", new Color(0x00ff27))
        .put("Y" , new Color(0x67988e))
        .put("Zr", new Color(0x00ff00))
        .put("Nb", new Color(0x4cb376))
        .put("Mo", new Color(0x54b5b5))
        .put("Tc", new Color(0x3b9e9e))
        .put("Ru", new Color(0x248f8f))
        .put("Rh", new Color(0x0a7d8c))
        .put("Pd", new Color(0x006985))
        .put("Ag", new Color(0xe0e0ff))
        .put("Cd", new Color(0xffd98f))
        .put("In", new Color(0xa67573))
        .put("Sn", new Color(0x668080))
        .put("Sb", new Color(0x9e63b5))
        .put("Te", new Color(0xd47a00))
        .put("I" , new Color(0x940094))
        .put("Xe", new Color(0x429eb0))
        .put("Cs", new Color(0x57178f))
        .put("Ba", new Color(0x00c900))
        .put("La", new Color(0x70d4ff))
        .put("Ce", new Color(0xffffc7))
        .put("Pr", new Color(0xd9ffc7))
        .put("Nd", new Color(0xc7ffc7))
        .put("Pm", new Color(0xa3ffc7))
        .put("Sm", new Color(0x8fffc7))
        .put("Eu", new Color(0x61ffc7))
        .put("Gd", new Color(0x45ffc7))
        .put("Tb", new Color(0x30ffc7))
        .put("Dy", new Color(0x1fffc7))
        .put("Ho", new Color(0x00ff9c))
        .put("Er", new Color(0x00e675))
        .put("Tm", new Color(0x00d452))
        .put("Yb", new Color(0x00bf38))
        .put("Lu", new Color(0x00ab24))
        .put("Hf", new Color(0x4dc2ff))
        .put("Ta", new Color(0x4da6ff))
        .put("W" , new Color(0x2194d6))
        .put("Re", new Color(0x267dab))
        .put("Os", new Color(0x266696))
        .put("Ir", new Color(0x175487))
        .put("Pt", new Color(0xe6d9ad))
        .put("Au", new Color(0xffd123))
        .put("Hg", new Color(0xb5b5c2))
        .put("Tl", new Color(0xa6544d))
        .put("Pb", new Color(0x575961))
        .put("Bi", new Color(0x9e4fb5))
        .put("Po", new Color(0xab5c00))
        .put("At", new Color(0x754f45))
        .put("Rn", new Color(0x428296))
        .put("Fr", new Color(0x420066))
        .put("Ra", new Color(0x007d00))
        .put("Ac", new Color(0x70abfa))
        .put("Th", new Color(0x00baff))
        .put("Pa", new Color(0x00a1ff))
        .put("U" , new Color(0x008fff))
        .put("Np", new Color(0x0080ff))
        .put("Pu", new Color(0x006bff))
        .put("Am", new Color(0x545cf2))
        .put("Cm", new Color(0x785ce3))
        .put("Bk", new Color(0x8a4fe3))
        .put("Cf", new Color(0xa136d4))
        .put("Es", new Color(0xb31fd4))
        .put("Fm", new Color(0xb31fba))
        .put("Md", new Color(0xb30da6))
        .put("No", new Color(0xbd0d87))
        .put("Lr", new Color(0xc70066))
        .put("Rf", new Color(0xcc0059))
        .put("Db", new Color(0xd1004f))
        .put("Sg", new Color(0xd90045))
        .put("Bh", new Color(0xe00038))
        .put("Hs", new Color(0xe6002e))
        .put("Mt", new Color(0xeb0026))
        .put("Ds", new Color(0xeb0026))
        .put("Rg", new Color(0xeb0026))
        .put("Cn", new Color(0xeb0026))
        .put("Nh", new Color(0xeb0026))
        .put("Fl", new Color(0xeb0026))
        .put("Mc", new Color(0xeb0026))
        .put("Lv", new Color(0xeb0026))
        .put("Ts", new Color(0xeb0026))
        .put("Og", new Color(0xeb0026))
        .build();
    /** All atomic size, from ovito, {@code SIZE["H"] == 0.46} */
    public final static Map<String, Double> SIZE = (new ImmutableMap.Builder<String, Double>())
        .put("H" , 0.46)
        .put("He", 1.22)
        .put("Li", 1.57)
        .put("Be", 1.47)
        .put("B" , 2.01)
        .put("C" , 0.77)
        .put("N" , 0.74)
        .put("O" , 0.74)
        .put("F" , 0.74)
        .put("Ne", 0.74)
        .put("Na", 1.91)
        .put("Mg", 1.60)
        .put("Al", 1.43)
        .put("Si", 1.18)
        .put("P" , 1.07)
        .put("S" , 1.05)
        .put("Cl", 1.02)
        .put("Ar", 1.06)
        .put("K" , 2.03)
        .put("Ca", 1.97)
        .put("Sc", 1.70)
        .put("Ti", 1.47)
        .put("V" , 1.53)
        .put("Cr", 1.29)
        .put("Mn", 1.39)
        .put("Fe", 1.26)
        .put("Co", 1.25)
        .put("Ni", 1.25)
        .put("Cu", 1.28)
        .put("Zn", 1.37)
        .put("Ga", 1.53)
        .put("Ge", 1.22)
        .put("As", 1.19)
        .put("Se", 1.20)
        .put("Br", 1.20)
        .put("Kr", 1.98)
        .put("Rb", 2.20)
        .put("Sr", 2.15)
        .put("Y" , 1.82)
        .put("Zr", 1.60)
        .put("Nb", 1.47)
        .put("Mo", 1.54)
        .put("Tc", 1.47)
        .put("Ru", 1.46)
        .put("Rh", 1.42)
        .put("Pd", 1.37)
        .put("Ag", 1.45)
        .put("Cd", 1.44)
        .put("In", 1.42)
        .put("Sn", 1.39)
        .put("Sb", 1.39)
        .put("Te", 1.38)
        .put("I" , 1.39)
        .put("Xe", 1.40)
        .put("Cs", 2.44)
        .put("Ba", 2.15)
        .put("La", 2.07)
        .put("Ce", 2.04)
        .put("Pr", 2.03)
        .put("Nd", 2.01)
        .put("Pm", 1.99)
        .put("Sm", 1.98)
        .put("Eu", 1.98)
        .put("Gd", 1.96)
        .put("Tb", 1.94)
        .put("Dy", 1.92)
        .put("Ho", 1.92)
        .put("Er", 1.89)
        .put("Tm", 1.90)
        .put("Yb", 1.87)
        .put("Lu", 1.87)
        .put("Hf", 1.75)
        .put("Ta", 1.70)
        .put("W" , 1.62)
        .put("Re", 1.51)
        .put("Os", 1.44)
        .put("Ir", 1.41)
        .put("Pt", 1.39)
        .put("Au", 1.44)
        .put("Hg", 1.32)
        .put("Tl", 1.45)
        .put("Pb", 1.47)
        .put("Bi", 1.46)
        .put("Po", 1.40)
        .put("At", 1.50)
        .put("Rn", 1.50)
        .put("Fr", 2.60)
        .put("Ra", 2.00)
        .put("Ac", 2.00)
        .put("Th", 2.00)
        .put("Pa", 2.00)
        .put("U" , 2.00)
        .put("Np", 2.00)
        .put("Pu", 2.00)
        .put("Am", 2.00)
        .put("Cm", 2.00)
        .put("Bk", 2.00)
        .put("Cf", 2.00)
        .put("Es", 2.00)
        .put("Fm", 2.00)
        .put("Md", 2.00)
        .put("No", 2.00)
        .put("Lr", 2.00)
        .put("Rf", 2.00)
        .put("Db", 2.00)
        .put("Sg", 2.00)
        .put("Bh", 2.00)
        .put("Hs", 2.00)
        .put("Mt", 2.00)
        .put("Ds", 2.00)
        .put("Rg", 2.00)
        .put("Cn", 2.00)
        .put("Nh", 2.00)
        .put("Fl", 2.00)
        .put("Mc", 2.00)
        .put("Lv", 2.00)
        .put("Ts", 2.00)
        .put("Og", 2.00)
        .build();
    
    
    /// SystemExecutor Stuffs
    @ApiStatus.Internal public final static Future<Integer> SUC_FUTURE = new CompletedFuture<>(0);
    @ApiStatus.Internal public final static Future<Integer> ERR_FUTURE = new CompletedFuture<>(-1);
    @ApiStatus.Internal public final static Future<List<Integer>> ERR_FUTURES = new CompletedFuture<>(Collections.singletonList(-1));
    @ApiStatus.Internal public final static Future<List<String>> EPT_STR_FUTURE = new CompletedFuture<>(AbstractCollections.zl());
    @ApiStatus.Internal public final static PrintStream NUL_PRINT_STREAM = new PrintStream(new OutputStream() {public void write(int b) {/**/}});
}
