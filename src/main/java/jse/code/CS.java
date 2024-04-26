package jse.code;

import com.google.common.collect.ImmutableMap;
import jse.atom.AbstractXYZ;
import jse.atom.IXYZ;
import jse.code.collection.AbstractCollections;
import jse.io.IIOFiles;
import jse.io.IOFiles;
import jse.io.SettingType;
import jse.math.SliceType;
import jse.parallel.CompletedFuture;
import jse.system.*;

import java.awt.*;
import java.io.*;
import java.nio.file.OpenOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * @author liqa
 * <p> Class containing useful Constants </p>
 */
public class CS {
    /** version of jse */
    public final static String VERSION = "2.8.3c";
    
    /** a Random generator so I don't need to instantiate a new one all the time. */
    public final static Random RANDOM = new Random();
    public final static int MAX_SEED = 2147483647;
    
    public final static Object NULL = null;
    
    public final static IXYZ XYZ_ZERO = new AbstractXYZ() {
        @Override public double x() {return 0.0;}
        @Override public double y() {return 0.0;}
        @Override public double z() {return 0.0;}
    };
    
    /** MathEX stuffs */
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
        , FILE_SYSTEM_TIMEOUT = 10000
        ;
    
    
    /** AtomData stuffs */
    public final static String[] ATOM_DATA_KEYS = {"x", "y", "z", "id", "type", "vx", "vy", "vz"};
    public final static String[] ATOM_DATA_KEYS_VELOCITY = {"vx", "vy", "vz"};
    public final static String[] ATOM_DATA_KEYS_XYZ = {"x", "y", "z"};
    public final static String[] ATOM_DATA_KEYS_XYZID = {"x", "y", "z", "id"};
    public final static String[] ATOM_DATA_KEYS_TYPE_XYZ = {"type", "x", "y", "z"};
    public final static String[] ATOM_DATA_KEYS_ID_TYPE_XYZ = {"id", "type", "x", "y", "z"};
    public final static String[] ALL_ATOM_DATA_KEYS = {"id", "type", "x", "y", "z", "vx", "vy", "vz"};
    public final static String[] STD_ATOM_DATA_KEYS = ATOM_DATA_KEYS_ID_TYPE_XYZ; // 标准 AtomData 包含信息格式为 id type x y z
    public final static int DATA_X_COL = 0, DATA_Y_COL = 1, DATA_Z_COL = 2, DATA_ID_COL = 3, DATA_TYPE_COL = 4, DATA_VX_COL = 5, DATA_VY_COL = 6, DATA_VZ_COL = 7;
    public final static int XYZ_X_COL = 0, XYZ_Y_COL = 1, XYZ_Z_COL = 2;
    public final static int XYZID_X_COL = 0, XYZID_Y_COL = 1, XYZID_Z_COL = 2, XYZID_ID_COL = 3;
    public final static int TYPE_XYZ_TYPE_COL = 0, TYPE_XYZ_X_COL = 1, TYPE_XYZ_Y_COL = 2, TYPE_XYZ_Z_COL = 3;
    public final static int ALL_ID_COL = 0, ALL_TYPE_COL = 1, ALL_X_COL = 2, ALL_Y_COL = 3, ALL_Z_COL = 4, ALL_VX_COL = 5, ALL_VY_COL = 6, ALL_VZ_COL = 7;
    public final static int STD_ID_COL = 0, STD_TYPE_COL = 1, STD_X_COL = 2, STD_Y_COL = 3, STD_Z_COL = 4;
    public final static int STD_VX_COL = 0, STD_VY_COL = 1, STD_VZ_COL = 2;
    
    public final static double R_NEAREST_MUL = 1.5;
    
    /** const arrays */
    public final static OpenOption[] ZL_OO = new OpenOption[0];
    public final static String[] ZL_STR = new String[0];
    public final static Object[] ZL_OBJ = new Object[0];
    public final static double[][] ZL_MAT = new double[0][];
    public final static double[]   ZL_VEC = new double[0];
    public final static int[]      ZL_INT = new int[0];
    public final static long[]    ZL_LONG = new long[0];
    public final static byte[]    ZL_BYTE = new byte[0];
    public final static boolean[] ZL_BOOL = new boolean[0];
    
    /** IOFiles Keys */
    public final static String
          OUTPUT_FILE_KEY = "<out>"
        , INFILE_SELF_KEY = "<self>"
        , OFILE_KEY = "<o>"
        , IFILE_KEY = "<i>"
        ;
    public final static SettingType
          REMOVE = SettingType.REMOVE
        , KEEP = SettingType.KEEP
        ;
    
    /** Patterns */
    public final static Pattern BLANKS_OR_EMPTY = Pattern.compile("\\s*");
    public final static Pattern BLANKS = Pattern.compile("\\s+");
    public final static Pattern COMMA = Pattern.compile("\\s*,\\s*");
    public final static Pattern COMMA_OR_BLANKS = Pattern.compile("\\s*[,\\s]\\s*");
    
    /** Boltzmann constant */
    public final static double K_B = 0.0000861733262; // eV / K
    
    /** All atom name, start from 0 */
    public final static String[] ATOM_TYPE_NAMES = {
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
    /** All atomic mass */
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
    /** All atomic color, from ovito */
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
    /** All atomic size, from ovito */
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
    
    
    /** SystemExecutor Stuffs */
    public final static IIOFiles EPT_IOF = IOFiles.immutable();
    public final static IFutureJob SUC_FUTURE = new CompletedFutureJob(0);
    public final static IFutureJob ERR_FUTURE = new CompletedFutureJob(-1);
    public final static Future<List<Integer>> ERR_FUTURES = new CompletedFuture<>(Collections.singletonList(-1));
    public final static Future<List<String>> EPT_STR_FUTURE = new CompletedFuture<>(AbstractCollections.zl());
    public final static PrintStream NUL_PRINT_STREAM = new PrintStream(new OutputStream() {public void write(int b) {/**/}});
    
    
    /** @deprecated use {@link OS} */
    @Deprecated public final static class Exec extends OS {
        public final static boolean IS_WINDOWS = OS.IS_WINDOWS;
        public final static boolean IS_MAC = OS.IS_MAC;
        public final static String NO_LOG_LINUX = OS.NO_LOG_LINUX;
        public final static String NO_LOG_WIN = OS.NO_LOG_WIN;
        public final static String NO_LOG = OS.NO_LOG;
        
        public final static ISystemExecutor EXE = OS.EXEC;
        public final static String JAR_PATH = OS.JAR_PATH;
        public final static String JAR_DIR = OS.JAR_DIR;
        public final static String USER_HOME = OS.USER_HOME;
        public final static String USER_HOME_DIR = OS.USER_HOME_DIR;
        public final static String WORKING_DIR = OS.WORKING_DIR;
    }
    
    /** @deprecated use {@link OS.Slurm} */
    @Deprecated public final static class Slurm extends OS.Slurm {
        public final static boolean IS_SLURM = OS.Slurm.IS_SLURM;
        public final static int PROCID = OS.Slurm.PROCID;
        public final static int NTASKS = OS.Slurm.NTASKS;
        public final static int CORES_PER_NODE = OS.Slurm.CORES_PER_NODE;
        public final static int CORES_PER_TASK = OS.Slurm.CORES_PER_TASK;
        public final static int MAX_STEP_COUNT = OS.Slurm.MAX_STEP_COUNT;
        public final static int JOB_ID = OS.Slurm.JOB_ID;
        public final static int NODEID = OS.Slurm.NODEID;
        public final static String NODENAME = OS.Slurm.NODENAME;
        public final static List<String> NODE_LIST = OS.Slurm.NODE_LIST;
        public final static ResourcesManager RESOURCES_MANAGER = OS.Slurm.RESOURCES_MANAGER;
    }
}
