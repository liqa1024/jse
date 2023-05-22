package com.jtool.jobs;


import com.jtool.code.UT;

import java.util.ArrayList;
import java.util.List;

import static com.jtool.code.CS.WORKING_DIR;

/**
 * @author liqa
 * <p> 按步骤完成任务的管理器，主要用来实现 groovy 上类似的分段执行脚本 </p>
 * <p> 每部分任务都需要是“独立”的，不能有变量关联，可以通过读写文件的方式来交流数据 </p>
 * <p> 每次运行都会执行个 then() 和其下一个 doJob()，然后会在下一个 then() 之前停止 </p>
 */
public class StepJobManager {
    private final String mWorkingDir;
    private final String mStepFile;
    private final List<Runnable> mJobList;
    private final List<Runnable> mConnectorList;
    private final int mForceStep;
    
    public StepJobManager(String aUniqueName) {this(aUniqueName, -1);}
    public StepJobManager(String aUniqueName, int aForceStep) {
        mWorkingDir = WORKING_DIR.replaceAll("%n", "SJM@"+aUniqueName);
        mStepFile = mWorkingDir+"step";
        mJobList = new ArrayList<>();
        mConnectorList = new ArrayList<>();
        mForceStep = aForceStep;
    }
    
    
    
    /** 使用这个结构让使用更加规范 */
    private final StepJob STEP_JOB = new StepJob();
    private final Connector CONNECTOR = new Connector();
    protected class StepJob {
        public Connector doJob(Runnable aJobDo) {
            mJobList.add(aJobDo);
            return CONNECTOR;
        }
    }
    protected class Connector {
        public StepJob then(Runnable aThenDo) {
            mConnectorList.add(aThenDo);
            return STEP_JOB;
        }
        /** 结束组装，并且开始任务 */
        public void finish(Runnable aFinishDo) throws Exception {
            // 首先创建工作目录
            UT.IO.makeDir(mWorkingDir);
            // 检测记录工作进度的文件是否存在，如果不存在说明需要从头开始
            int tStep;
            if (mForceStep >= 0) {
                tStep = mForceStep;
            } else
            if (UT.IO.isFile(mStepFile)) {
                tStep = Integer.parseInt(UT.IO.readAllLines_(mStepFile).get(0));
            } else {
                tStep = 0;
            }
            if (tStep == mConnectorList.size()) {
                // 步骤是最终步，执行最终的任务，并删除文件
                aFinishDo.run();
                UT.IO.delete(mStepFile);
            } else {
                // 否则执行此步骤的任务
                mConnectorList.get(tStep).run();
                mJobList.get(tStep).run();
                // 增加步骤，存储到文件
                ++tStep;
                UT.IO.write(mStepFile, String.valueOf(tStep));
            }
        }
    }
    
    /** 开始组装 */
    public StepJob init(Runnable aInitDo) {
        mConnectorList.add(aInitDo);
        return STEP_JOB;
    }
}
