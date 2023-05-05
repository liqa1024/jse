package com.guan.test;

import com.guan.code.UT;
import com.guan.lmp.Lmpdat;
import com.guan.atom.MonatomicParameterCalculator;
import com.guan.parallel.ParforThreadPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParBenchmark {
    private static void runMPC() {
        System.out.println("Setup: ");
        Lmpdat tLmpdat;
        try {tLmpdat = Lmpdat.read_(UT.IO.readAllLines(UT.IO.getResource(ParBenchmark.class, "data-1").openStream()).toArray(new String[0]));} catch (IOException e) {throw new RuntimeException(e);}
        MonatomicParameterCalculator MPC = tLmpdat.getMonatomicParameterCalculator(6);
        
        System.out.println("Warmup: ");
        UT.Timer.tic();
        MPC.calRDF(1000, 40.0);
        UT.Timer.toc("Normal");
        
        System.out.println("Run: ");
        UT.Timer.tic();
        MPC.calRDF(1000, 40.0);
        UT.Timer.toc("Normal");
        
        System.out.println("RunG: ");
        UT.Timer.tic();
        MPC.calRDF_G(1000, 40.0);
        UT.Timer.toc("Gauss");
        
        System.out.println("Teardown: ");
        MPC.shutdown();
    }
    
    private static void runMultiMPC() {
        Lmpdat tLmpdat;
        try {tLmpdat = Lmpdat.read_(UT.IO.readAllLines(UT.IO.getResource(ParBenchmark.class, "data-1").openStream()).toArray(new String[0]));} catch (IOException e) {throw new RuntimeException(e);}
        final int tParNum = 8;
        final double tRMax = 10.0;
        MonatomicParameterCalculator[] MPCs = new MonatomicParameterCalculator[tParNum];
        for (int i = 0; i < tParNum; ++i) MPCs[i] = tLmpdat.getMonatomicParameterCalculator();
        ParforThreadPool tPT = new ParforThreadPool(tParNum);
        
        UT.Timer.tic();
        for (int i = 0; i < tParNum; ++i) for (int j = 0; j < 400; ++j) MPCs[i].calRDF(1000, tRMax);
        UT.Timer.toc("Single Thread");
        
        UT.Timer.tic();
        tPT.parfor_(tParNum, i -> {
            for (int j = 0; j < 400; ++j) MPCs[i].calRDF(1000, tRMax);
        });
        UT.Timer.toc("Multi Thread");
        
        UT.Timer.tic();
        for (int i = 0; i < tParNum; ++i) for (int j = 0; j < 400; ++j) MPCs[i].calRDF(1000, tRMax);
        UT.Timer.toc("Single Thread");
        
        UT.Timer.tic();
        tPT.parfor_(tParNum, i -> {
            for (int j = 0; j < 400; ++j) MPCs[i].calRDF(1000, tRMax);
        });
        UT.Timer.toc("Multi Thread");
        
        System.out.println("Teardown: ");
        for (int i = 0; i < tParNum; ++i) MPCs[i].shutdown();
        tPT.shutdown();
    }
    
    /** 使用 toTaskCall 来实现并行的一个例子 */
    private static void runTask() throws ExecutionException, InterruptedException {
        final int tThreadNum = 4;
        
        Lmpdat tLmpdat;
        try {tLmpdat = Lmpdat.read_(UT.IO.readAllLines(UT.IO.getResource(ParBenchmark.class, "data-1").openStream()).toArray(new String[0]));} catch (IOException e) {throw new RuntimeException(e);}
        MonatomicParameterCalculator[] MPCs = new MonatomicParameterCalculator[tThreadNum];
        for (int i = 0; i < tThreadNum; ++i) MPCs[i] = tLmpdat.getMonatomicParameterCalculator();
        
        ExecutorService tPool = Executors.newFixedThreadPool(tThreadNum);
        Future<?>[] tTasks = new Future<?>[tThreadNum];
        List<double[][]> tResults = new ArrayList<>(tThreadNum);
        
        UT.Timer.tic();
        for (int i = 0; i < tThreadNum; ++i) tTasks[i] = tPool.submit(UT.Hack.toTaskCall(MPCs[i], "calRDF", 1000, 60.0));
        for (Future<?> tTask : tTasks) tResults.add((double[][]) tTask.get());
        UT.Timer.toc("Multi Thread");
        
        UT.Timer.tic();
        MPCs[0].calRDF(1000, 60.0);
        UT.Timer.toc("Single Thread");
        
        tPool.shutdown();
    }
    
    
    /** An example to use parfor */
    private static void runParfor() {
        ParforThreadPool tPT = new ParforThreadPool(4, 4);
        
        tPT.parfor(23, (i, threadID) -> {
            System.out.printf("Thread: %d, i: %d\n", threadID, i);
            try {Thread.sleep(10);} catch (InterruptedException ignored) {}
        });
        
        tPT.shutdown();
    }
    
    public static void main(String[] args) throws Exception {
        runTask();
    }
    
}
