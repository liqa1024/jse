package com.jtool.code.task;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

/**
 * @author liqa
 * <p> General task for this project </p>
 */
@Deprecated
@SuppressWarnings({"DeprecatedIsStillUsed", "deprecation", "RedundantSuppression"})
public class Task extends TaskCall<Boolean> {
    public Task(Callable<Boolean> aCall) {super(aCall);}
    
    public static Task of(final Runnable aRun) {return new Task(() -> {aRun.run(); return true;});}
    public static Task of(final Callable<?> aCall) {return new Task(() -> {Object tOut = aCall.call(); return (tOut instanceof Boolean) ? (Boolean)tOut : tOut!=null;});}
    
    
    /**
     * Merge two tasks into one task
     * @author liqa
     * @param aTask1 the first Task will call
     * @param aTask2 the second Task will call
     * @return the Merged (Serializable) Task
     */
    public static Task mergeTask(final @Nullable Task aTask1, final @Nullable Task aTask2) {
        if (aTask1 != null) {
            if (aTask2 == null) return aTask1;
            return new SerializableTask(() -> aTask1.call() && aTask2.call()) {
                @Override public String toString() {return String.format("%s{%s:%s}", Type.MERGE.name(), (aTask1 instanceof SerializableTask) ? aTask1 : Type.NULL.name(), (aTask2 instanceof SerializableTask) ? aTask2 : Type.NULL.name());}
            };
        }
        return aTask2;
    }
    
    /**
     * Try to call a task
     * @author liqa
     * @param aTask the Task to call
     * @return true if it runs successfully, false otherwise
     */
    public static boolean tryTask(Task aTask) {
        if (aTask == null) return false;
        boolean tSuc;
        try {tSuc = aTask.call();} catch (Exception e) {return false;}
        return tSuc;
    }
    
    /**
     * Try to run a task with tolerant
     * @author liqa
     * @param aTask the Task to call
     * @param aTolerant tolerant number
     * @return true if it runs successfully, false otherwise
     */
    public static boolean tryTask(Task aTask, int aTolerant) {
        if (aTask == null) return false;
        boolean tSuc = false;
        for (int i = 0; i < aTolerant; ++i) {
            try {tSuc = aTask.call();} catch (Exception e) {continue;}
            if (tSuc) break;
        }
        return tSuc;
    }
}
