package atom

import jtool.code.SP
import jtool.math.matrix.RowMatrix

/**
 * 涉及的功能较多，
 * 这里使用一个类来存储
 */
class ClassifyCe {
    
    final static def read_vasp_xdatcar;
    final static def basisCalculator;
    
    static {
        // 导入需要的 python 包
        SP.Python.runText('from ase.io.vasp import read_vasp_xdatcar');
        SP.Python.runText('from libenv.spherical_chebyshev import SphericalChebyshev');
        
        // 获取需要的类/方法名，存为静态变量
        read_vasp_xdatcar = SP.Python.getClass('read_vasp_xdatcar');
        basisCalculator = SP.Python.newInstance('SphericalChebyshev', ['Ce'], 5, 6, (double)6.5);
    }
    
    /** 计算对应的基，这里暂时使用现有的 python 脚本计算 */
    static def calBasis(String path, int index=-1) {
        // 使用 ase 的读取 xdatcar 方法，读取成 ase 的实例
        def data = read_vasp_xdatcar(path, [index: index]);
        // 使用 basisCalculator 来计算基，输出为 numpy 的数组，注意需要调用一次 unwrap 来获取到 JEP 可以识别到的 python 对象
        def basis = basisCalculator.evaluate(data.unwrap());
        // 获取的是 jep.NDArray，这样获取内部数据，直接转为 IMatrix，这里获取到的是按行排列的
        double[] basisData = basis.data;
        int[] basisDim = basis.dimensions;
        return new RowMatrix(basisDim[0], basisDim[1], basisData);
    }
    
    static def main(args) {
        def basis = calBasis('vasp/.Ce/20U/2nd400/XDATCAR');
        println(basis);
        println(basis.class);
    }
}
