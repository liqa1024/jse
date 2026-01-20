package jse.clib;

import java.lang.annotation.*;

/**
 * 用于标记不安全的 JNI 接口。
 * <p>
 * 被标注的方法 / 构造器通常直接或间接调用 JNI，
 * 在参数非法、生命周期错误、线程不匹配等情况下，
 * <b>可能导致 JVM 发生段错误（SIGSEGV / SIGBUS）并直接崩溃</b>。
 * </p>
 * <p>
 * 本注解仅用于 <b>文档和开发期提示</b>，不产生任何编译期或运行期行为。
 * </p>
 * @author liqa
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface UnsafeJNI {
    /** 对不安全原因或使用的要求的简要说明段落 */
    String value() default "";
}
