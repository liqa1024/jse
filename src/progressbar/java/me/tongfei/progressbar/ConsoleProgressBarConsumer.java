package me.tongfei.progressbar;

import java.io.PrintStream;


/**
 * Progress bar consumer that prints the progress bar state to console.
 * By default {@link System#err} is used as {@link PrintStream}.
 *
 * @author Tongfei Chen
 * @author Alex Peelman
 */
public class ConsoleProgressBarConsumer implements ProgressBarConsumer {
    
    final int maxRenderedLength;
    final PrintStream out;
    final String refreshPrompt;

    public ConsoleProgressBarConsumer(PrintStream out) {
        this(out, DEFAULT_MAX_WIDTH);
    }
    public ConsoleProgressBarConsumer(PrintStream out, int maxRenderedLength) {
        this(out, maxRenderedLength, "\r");
    }
    public ConsoleProgressBarConsumer(PrintStream out, int maxRenderedLength, String refreshPrompt) {
        this.maxRenderedLength = maxRenderedLength <= 0 ? DEFAULT_MAX_WIDTH : maxRenderedLength;
        this.out = out;
        this.refreshPrompt = refreshPrompt;
    }

    @Override
    public int getMaxRenderedLength() {return maxRenderedLength;}

    @Override
    public void accept(String str) {
        out.print(refreshPrompt + StringDisplayUtils.trimDisplayLength(str, getMaxRenderedLength()));
    }

    @Override
    public void close() {
        out.println();
        out.flush();
    }
}
