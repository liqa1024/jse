package me.tongfei.progressbar.wrapped;

import me.tongfei.progressbar.ProgressBarBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Any iterable, when being iterated over, is tracked by a progress bar.
 * @author Tongfei Chen
 * @since 0.6.0
 */
public class ProgressBarWrappedIterable<T> implements Iterable<T> {

    private final Iterable<T> underlying;
    private final ProgressBarBuilder pbb;

    public ProgressBarWrappedIterable(Iterable<T> underlying, ProgressBarBuilder pbb) {
        this.underlying = underlying;
        this.pbb = pbb;
    }

    public ProgressBarBuilder getProgressBarBuilder() {
        return pbb;
    }

    @Override
    public @NotNull ProgressBarWrappedIterator<T> iterator() {
        Iterator<T> it = underlying.iterator();
        return new ProgressBarWrappedIterator<>(
                it,
                pbb.setInitialMax(underlying.spliterator().getExactSizeIfKnown()).build()
                // getExactSizeIfKnown return -1 if not known, then indefinite progress bar naturally
        );
    }
}
