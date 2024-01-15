package test.mathex

import jtool.math.MathEX
import jtool.math.vector.ComplexVector

import static jtool.code.UT.Math.*

/**
 * 测试球谐函数计算结果
 */

rng(123456789);

double theta = rand()*pi;
double phi = rand()*pi;

def shTLAll = MathEX.Func.sphericalHarmonicsTL(true, 4, theta, phi);
println(shTLAll);
// 25-length Complex Vector:
//    0.2821 + 0.000i   0.04053 - 0.2979i   -0.2408 + 0.000i   -0.04053 - 0.2979i   -0.2818 - 0.07814i   -0.04467 + 0.3283i   -0.08558 + 0.000i   0.04467 + 0.3283i   -0.2818 + 0.07814i   -0.1085 + 0.2525i   0.3675 + 0.1019i   0.008129 - 0.05974i   0.3284 + 0.000i   -0.008129 - 0.05974i   0.3675 - 0.1019i   0.1085 + 0.2525i   0.2175 + 0.1306i   0.1604 - 0.3734i   -0.1709 - 0.04738i   0.03555 - 0.2613i   -0.2350 + 0.000i   -0.03555 - 0.2613i   -0.1709 + 0.04738i   -0.1604 - 0.3734i   0.2175 - 0.1306i


def shTLBuilder = ComplexVector.builder();
for (l in 0..4) {
    def shTL = MathEX.Func.sphericalHarmonicsTL(l, theta, phi);
    for (Y in shTL.asList()) shTLBuilder.add(Y);
}
def shTL = shTLBuilder.build();
println(shTL);
// 25-length Complex Vector:
//    0.2821 + 0.000i   0.04053 - 0.2979i   -0.2408 + 0.000i   -0.04053 - 0.2979i   -0.2818 - 0.07814i   -0.04467 + 0.3283i   -0.08558 + 0.000i   0.04467 + 0.3283i   -0.2818 + 0.07814i   -0.1085 + 0.2525i   0.3675 + 0.1019i   0.008129 - 0.05974i   0.3284 + 0.000i   -0.008129 - 0.05974i   0.3675 - 0.1019i   0.1085 + 0.2525i   0.2175 + 0.1306i   0.1604 - 0.3734i   -0.1709 - 0.04738i   0.03555 - 0.2613i   -0.2350 + 0.000i   -0.03555 - 0.2613i   -0.1709 + 0.04738i   -0.1604 - 0.3734i   0.2175 - 0.1306i

def shBuilder = ComplexVector.builder();
for (l in 0..4) {
    for (m in -l..l) {
        shBuilder.add(MathEX.Func.sphericalHarmonics(l, m, theta, phi));
    }
}
def sh = shBuilder.build();
println(sh);
// 25-length Complex Vector:
//    0.2821 + 0.000i   0.04053 - 0.2979i   -0.2408 - 0.000i   -0.04053 - 0.2979i   -0.2818 - 0.07814i   -0.04467 + 0.3283i   -0.08558 - 0.000i   0.04467 + 0.3283i   -0.2818 + 0.07814i   -0.1085 + 0.2525i   0.3675 + 0.1019i   0.008129 - 0.05974i   0.3284 + 0.000i   -0.008129 - 0.05974i   0.3675 - 0.1019i   0.1085 + 0.2525i   0.2175 + 0.1306i   0.1604 - 0.3734i   -0.1709 - 0.04738i   0.03555 - 0.2613i   -0.2350 - 0.000i   -0.03555 - 0.2613i   -0.1709 + 0.04738i   -0.1604 - 0.3734i   0.2175 - 0.1306i

