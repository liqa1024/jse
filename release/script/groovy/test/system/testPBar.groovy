package test.system

import jtool.code.UT

UT.Timer.pbar(50);
for (i in 0..<50) {
    sleep(100);
    UT.Timer.pbar();
}

