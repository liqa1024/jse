package jse.cptr;

import jse.gpu.*;
import org.jetbrains.annotations.ApiStatus;

/**
 * 任意指针通用接口，方便进行抽象的使用
 * @author liqa
 */
@ApiStatus.Experimental
public interface IPointer {
    /** @return 内部存储的指针值 */
    @ApiStatus.Internal long ptr_();
    /** @return 内部存储的指针是否是空的 */
    default boolean isNull() {return ptr_()==0 || ptr_()==-1;}
    
    /**
     * 将此接口转换成标准的 c 指针 {@link CPointer} 包装类，方便一些场景使用
     * @return 标准 c 指针包装类
     */
    default CPointer asCPointer() {return new CPointer(ptr_());}
    /**
     * 将此接口转换成一个整数的 c 指针 {@link IntCPointer}，类似在 c
     * 中使用 {@code (int *)ptr} 来进行强制类型转换
     * @return 整数的 c 指针包装类
     */
    default IntCPointer asIntCPointer() {return new IntCPointer(ptr_());}
    /**
     * 将此接口转换成一个整数的 c 指针 {@link Int64CPointer}，类似在 c
     * 中使用 {@code (int64_t *)ptr} 来进行强制类型转换
     * @return 整数的 c 指针包装类
     */
    default Int64CPointer asInt64CPointer() {return new Int64CPointer(ptr_());}
    /**
     * 将此接口转换成一个双精度浮点的 c 指针 {@link DoubleCPointer}，类似在 c
     * 中使用 {@code (double *)ptr} 来进行强制类型转换
     * @return 双精度浮点的 c 指针包装类
     */
    default DoubleCPointer asDoubleCPointer() {return new DoubleCPointer(ptr_());}
    /**
     * 将此接口转换成一个单精度浮点的 c 指针 {@link FloatCPointer}，类似在 c
     * 中使用 {@code (float *)ptr} 来进行强制类型转换
     * @return 双精度浮点的 c 指针包装类
     */
    default FloatCPointer asFloatCPointer() {return new FloatCPointer(ptr_());}
    /**
     * 将此接口转换成一个嵌套指针的 c 指针 {@link AnyCPointer}，类似在 c
     * 中使用 {@code (void **)ptr} 来进行强制类型转换
     * @return 嵌套指针的 c 指针包装类
     */
    default AnyCPointer asAnyCPointer() {return new AnyCPointer(ptr_());}
    /**
     * 将此接口转换成一个嵌套整数指针的 c 指针 {@link AnyCPointer}，类似在 c
     * 中使用 {@code (int **)ptr} 来进行强制类型转换
     * @return 嵌套整数指针的 c 指针包装类
     */
    default NestedIntCPointer asNestedIntCPointer() {return new NestedIntCPointer(ptr_());}
    /**
     * 将此接口转换成一个嵌套双精度浮点指针的 c 指针 {@link NestedDoubleCPointer}，类似在 c
     * 中使用 {@code (double **)ptr} 来进行强制类型转换
     * @return 嵌套双精度浮点指针的 c 指针包装类
     */
    default NestedDoubleCPointer asNestedDoubleCPointer() {return new NestedDoubleCPointer(ptr_());}
    
    /**
     * 将此接口转换成标准的 cuda 指针 {@link CudaPointer} 包装类，方便一些场景使用
     * @return 标准 cuda 指针包装类
     */
    default CudaPointer asCudaPointer() {return new CudaPointer(ptr_());}
    /**
     * 将此接口转换成一个整数的 cuda 指针 {@link IntCudaPointer}，类似在 c
     * 中使用 {@code (int *)ptr} 来进行强制类型转换
     * @return 整数的 cuda 指针包装类
     */
    default IntCudaPointer asIntCudaPointer() {return new IntCudaPointer(ptr_());}
    /**
     * 将此接口转换成一个整数的 cuda 指针 {@link IntCudaPointer}，类似在 c
     * 中使用 {@code (int64 *)ptr} 来进行强制类型转换
     * @return 整数的 cuda 指针包装类
     */
    default Int64CudaPointer asInt64CudaPointer() {return new Int64CudaPointer(ptr_());}
    /**
     * 将此接口转换成一个单精度浮点的 cuda 指针 {@link FloatCudaPointer}，类似在 c
     * 中使用 {@code (float *)ptr} 来进行强制类型转换
     * @return 双精度浮点的 cuda 指针包装类
     */
    default FloatCudaPointer asFloatCudaPointer() {return new FloatCudaPointer(ptr_());}
    /**
     * 将此接口转换成一个双精度浮点的 cuda 指针 {@link DoubleCudaPointer}，类似在 c
     * 中使用 {@code (double *)ptr} 来进行强制类型转换
     * @return 双精度浮点的 cuda 指针包装类
     */
    default DoubleCudaPointer asDoubleCudaPointer() {return new DoubleCudaPointer(ptr_());}
}
