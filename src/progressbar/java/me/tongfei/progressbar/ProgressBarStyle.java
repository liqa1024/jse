package me.tongfei.progressbar;

/**
 * Represents the display style of a progress bar.
 * @author Tongfei Chen
 * @since 0.5.1
 */
public class ProgressBarStyle {
    
    String leftBracket;
    String delimitingSequence;
    String rightBracket;
    char block;
    char space;
    String fractionSymbols;
    char rightSideFractionSymbol;

    ProgressBarStyle(String leftBracket, String delimitingSequence, String rightBracket, char block, char space, String fractionSymbols, char rightSideFractionSymbol) {
        this.leftBracket = leftBracket;
        this.delimitingSequence = delimitingSequence;
        this.rightBracket = rightBracket;
        this.block = block;
        this.space = space;
        this.fractionSymbols = fractionSymbols;
        this.rightSideFractionSymbol = rightSideFractionSymbol;
    }

    public static ProgressBarStyle COLORFUL_UNICODE_BLOCK =
            new ProgressBarStyle("\u001b[33m│", "", "│\u001b[0m", '█', ' ', " ▏▎▍▌▋▊▉", ' ');

    public static ProgressBarStyle COLORFUL_UNICODE_BAR =
            new ProgressBarStyle("\u001b[33m", "\u001b[90m", "\u001b[0m", '━', '━', "━╸", '╺');

    /** Use Unicode block characters to draw the progress bar. */
    public static ProgressBarStyle UNICODE_BLOCK =
            new ProgressBarStyle("|", "", "|", '█', ' ', " ▏▎▍▌▋▊▉", ' ');

    /** Use only ASCII characters to draw the progress bar. */
    public static ProgressBarStyle ASCII =
            new ProgressBarStyle("[", "", "]", '=', ' ', " >", '>');

    /** Creates a builder to build a custom progress bar style. */
    public static ProgressBarStyleBuilder builder() {
        return new ProgressBarStyleBuilder();
    }
}
