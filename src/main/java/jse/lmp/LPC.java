package jse.lmp;

import jse.system.ISystemExecutor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class LPC extends LmpParameterCalculator {
    public LPC(ILmpExecutor aLMP, String aPairStyle, String aPairCoeff) {super(aLMP, aPairStyle, aPairCoeff);}
    public LPC(String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {super(aLmpExe, aLogPath, aPairStyle, aPairCoeff);}
    public LPC(String aLmpExe, String aPairStyle, String aPairCoeff) {super(aLmpExe, aPairStyle, aPairCoeff);}
    public LPC(ISystemExecutor aEXE, String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {super(aEXE, aLmpExe, aLogPath, aPairStyle, aPairCoeff);}
    public LPC(ISystemExecutor aEXE, String aLmpExe, String aPairStyle, String aPairCoeff) {super(aEXE, aLmpExe, aPairStyle, aPairCoeff);}
}
