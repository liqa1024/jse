package com.guan.ssh;

import com.guan.code.Pair;
import com.guan.code.Task;
import com.guan.code.UT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author liqa
 * <p> 可以序列化的 Task，目前仅用于 ssh 端的一些 task 的读写上 </p>
 */
public class SerializableTask extends Task {
    public SerializableTask(Callable<Boolean> aCall) {super(aCall);}
    /** override to get serialized string */
    @Override public String toString() {return Type.NULL.name();}
    
    // add to here to get task from String
    public enum Type {
          NULL
        , MERGE
        // SSH stuff
        , SYSTEM
        , PUT_DIR
        , GET_DIR
        , CLEAR_DIR
        , REMOVE_DIR
        , RMDIR // 兼容旧版
        , MAKE_DIR
        , MKDIR // 兼容旧版
        , PUT_FILE
        , GET_FILE
        , PUT_DIR_PAR
        , GET_DIR_PAR
        , CLEAR_DIR_PAR
        , PUT_WORKING_DIR
        , PUT_WORKING_DIR_PAR
        , GET_WORKING_DIR
        , GET_WORKING_DIR_PAR
        , CLEAR_WORKING_DIR
        , CLEAR_WORKING_DIR_PAR
        // SLURM stuff
        , SLURM_CANCEL_ALL
        , CANCEL_ALL // 兼容旧版
        , SLURM_CANCEL_THIS
        , CANCEL_THIS // 兼容旧版
        , SLURM_SUBMIT_SYSTEM
        , SLURM_SUBMIT_BASH
        , SLURM_SUBMIT_SRUN
        , SLURM_SUBMIT_SRUN_BASH
    }
    public static Task fromString(final Object aTaskCreator, String aStr) {
        Pair<String, List<String>> tPair = getKeyValue_(aStr);
        Type tKey = Type.valueOf(tPair.first);
        String[] tValue = tPair.second.toArray(new String[0]);
        switch (tKey) {
        case MERGE:
            return UT.Tasks.mergeTask(fromString(aTaskCreator, tValue[0]), fromString(aTaskCreator, tValue[1]));
        case SLURM_CANCEL_ALL: case CANCEL_ALL:
        case SLURM_CANCEL_THIS: case CANCEL_THIS:
        case SLURM_SUBMIT_SYSTEM: case SLURM_SUBMIT_BASH: case SLURM_SUBMIT_SRUN: case SLURM_SUBMIT_SRUN_BASH:
            return fromString_(aTaskCreator, (aTaskCreator instanceof ServerSLURM) ? (ServerSLURM)aTaskCreator : null, tKey, tValue);
        case SYSTEM:
        case PUT_DIR:     case GET_DIR:     case CLEAR_DIR:
        case PUT_DIR_PAR: case GET_DIR_PAR: case CLEAR_DIR_PAR:
        case REMOVE_DIR:  case RMDIR:
        case MAKE_DIR:    case MKDIR:
        case PUT_FILE:    case GET_FILE:
        case PUT_WORKING_DIR:   case PUT_WORKING_DIR_PAR:
        case GET_WORKING_DIR:   case GET_WORKING_DIR_PAR:
        case CLEAR_WORKING_DIR: case CLEAR_WORKING_DIR_PAR:
            return fromString_(aTaskCreator, (aTaskCreator instanceof ServerSLURM) ? ((ServerSLURM)aTaskCreator).ssh() : (aTaskCreator instanceof ServerSSH) ? (ServerSSH)aTaskCreator : null, tKey, tValue);
        case NULL: default:
            return null;
        }
    }
    
    static Task fromString_(final Object aTaskCreator, ServerSLURM aSLURM, Type aKey, String... aValues) {
        if (aSLURM == null) return null;
        switch (aKey) {
        case SLURM_CANCEL_ALL: case CANCEL_ALL:
            return aSLURM.task_cancelAll();
        case SLURM_CANCEL_THIS: case CANCEL_THIS:
            return aSLURM.task_cancelThis();
        case SLURM_SUBMIT_SYSTEM:
            return aSLURM.task_submitSystem     (fromString(aTaskCreator, aValues[0]), fromString(aTaskCreator, aValues[1]), aValues[2], aValues[3], Integer.parseInt(aValues[4]), aValues[5]);
        case SLURM_SUBMIT_BASH:
            return aSLURM.task_submitBash       (fromString(aTaskCreator, aValues[0]), fromString(aTaskCreator, aValues[1]), aValues[2], aValues[3], Integer.parseInt(aValues[4]), aValues[5]);
        case SLURM_SUBMIT_SRUN:
            return aSLURM.task_submitSrun       (fromString(aTaskCreator, aValues[0]), fromString(aTaskCreator, aValues[1]), aValues[2], aValues[3], Integer.parseInt(aValues[4]), Integer.parseInt(aValues[5]), aValues[6]);
        case SLURM_SUBMIT_SRUN_BASH:
            return aSLURM.task_submitSrunBash   (fromString(aTaskCreator, aValues[0]), fromString(aTaskCreator, aValues[1]), aValues[2], aValues[3], Integer.parseInt(aValues[4]), Integer.parseInt(aValues[5]), aValues[6]);
        default:
            return null;
        }
    }
    
    static Task fromString_(final Object aTaskCreator, ServerSSH aSSH, Type aKey, String... aValues) {
        if (aSSH == null) return null;
        switch (aKey) {
        case SYSTEM:
            return aSSH.task_system         (aValues[0]);
        case PUT_DIR:
            return aSSH.task_putDir         (aValues[0]);
        case GET_DIR:
            return aSSH.task_getDir         (aValues[0]);
        case CLEAR_DIR:
            return aSSH.task_clearDir       (aValues[0]);
        case REMOVE_DIR: case RMDIR:
            return aSSH.task_rmdir          (aValues[0]);
        case MAKE_DIR: case MKDIR:
            return aSSH.task_mkdir          (aValues[0]);
        case PUT_FILE:
            return aSSH.task_putFile        (aValues[0]);
        case GET_FILE:
            return aSSH.task_getFile        (aValues[0]);
        case PUT_DIR_PAR:
            return aSSH.task_putDir         (aValues[0], Integer.parseInt(aValues[1]));
        case GET_DIR_PAR:
            return aSSH.task_getDir         (aValues[0], Integer.parseInt(aValues[1]));
        case CLEAR_DIR_PAR:
            return aSSH.task_clearDir       (aValues[0], Integer.parseInt(aValues[1]));
        case PUT_WORKING_DIR:
            return aSSH.task_putWorkingDir  ();
        case PUT_WORKING_DIR_PAR:
            return aSSH.task_putWorkingDir  (Integer.parseInt(aValues[0]));
        case GET_WORKING_DIR:
            return aSSH.task_getWorkingDir  ();
        case GET_WORKING_DIR_PAR:
            return aSSH.task_getWorkingDir  (Integer.parseInt(aValues[0]));
        case CLEAR_WORKING_DIR:
            return aSSH.task_clearWorkingDir();
        case CLEAR_WORKING_DIR_PAR:
            return aSSH.task_clearWorkingDir(Integer.parseInt(aValues[0]));
        default:
            return null;
        }
    }
    
    static final List<String> ZL_STR = new ArrayList<>();
    // deserialize the String in formation "Key{value1:value2:...}"
    static Pair<String, List<String>> getKeyValue_(String aStr) {
        int tValueIdx = aStr.indexOf("{");
        if (tValueIdx < 0) return new Pair<>(aStr, ZL_STR);
        String tKey = aStr.substring(0, tValueIdx);
        List<String> tValues = new ArrayList<>();
        // 直接遍历查找，注意在括号内部时不需要分割 :
        ++tValueIdx;
        int tIdx = tValueIdx;
        int tBlockCounter = 1;
        while (tBlockCounter > 0 && tIdx < aStr.length()) {
            if (aStr.charAt(tIdx)=='{') ++tBlockCounter;
            if (aStr.charAt(tIdx)=='}') --tBlockCounter;
            if (tBlockCounter == 1 && aStr.charAt(tIdx)==':') {
                // 如果是 "null" 字符串则认为是 null
                String tValue = aStr.substring(tValueIdx, tIdx);
                if (tValue.equals("null")) tValue = null;
                tValues.add(tValue);
                tValueIdx = tIdx + 1;
            }
            if (tBlockCounter == 0) tValues.add(aStr.substring(tValueIdx, tIdx)); // 到达最后，最后一项放入 value
            ++tIdx;
        }
        return new Pair<>(tKey, tValues);
    }
}
