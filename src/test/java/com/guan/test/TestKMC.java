package com.guan.test;

import com.guan.kmc.BiTrapSimple;

public class TestKMC {
    
    public static void main(String[] args) {
        BiTrapSimple kmcEngineEqu = new BiTrapSimple(100000, 0.1, 0.4, 2000);
        // 恒定温度模拟一段时间直到平衡
        kmcEngineEqu.run(20, 100000);
        // 创建带有冷速的
        BiTrapSimple kmcEngineCooldown = new BiTrapSimple(kmcEngineEqu.numberLow(), kmcEngineEqu.numberHigh(), 0.1, 0.4, 2000, 0.5);
        // 冷却
        kmcEngineCooldown.run(1000, 100000);
    }
}
