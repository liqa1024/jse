package com.jtool.math;

/**
 * 复数类，实际是类似结构体的设计，因此所有成员直接 public，
 * 为了使用方便这里不使用常规结构使用的命名规范
 * @author liqa
 */
public class ComplexDouble {
    public double real = 0.0;
    public double imag = 0.0;
    public ComplexDouble() {}
    public ComplexDouble(double real, double imag) {this.real = real; this.imag = imag;}
}
