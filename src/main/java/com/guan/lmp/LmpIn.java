package com.guan.lmp;


/**
 * @author liqa
 * <p> lammps 的输入文件，可以传入 IN emun 来使用预设，
 * 或者使用 String 的路径来使用自定义的输入文件 </p>
 * <p> 可以设定输入文件的 variable，然后使用 toFile 来根据修改创建输入文件，
 * 默认会存储在运行目录下的 .temp/lmp/in 文件夹下，文件名为输入文件的 hash 值，
 * 可以在 toFile 函数指定需要存储的文件名，或者使用 set 函数设置属性 </p>
 * <p> 还会存储 Map<{FileKey, FilePath}> 用来指明这个输入文件会有哪些其他的输入输出文件，
 * 在设置 variable 时会自动进行检测其格式是否发生改变（但是不会检测是否真的有这个输入或者输出文件），
 * 对于预设的 in 文件会存储对应的输出文件，而自定的输入文件则需要在创建时指定或者使用 set 来设置 </p>
 */
public class LmpIn {

}
