package com.github.stephenc.continuous.k2p.injectable;

import com.github.stephenc.continuous.k2p.MissingValueStrategy;
import com.github.stephenc.continuous.k2p.source.InjectionSource;

public abstract class AbstractInjectable<S extends InjectionSource> {
    private final String name;
    private final MissingValueStrategy strategy;
    private final S source;

    protected AbstractInjectable(String name, MissingValueStrategy strategy, S source) {
        this.name = name;
        this.strategy = strategy;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public MissingValueStrategy getStrategy() {
        return strategy;
    }

    public boolean isRedacted() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public final String valueOf(InjectionSource source) {
        if (source.equals(this.source)) {
            return getValue((S) source);
        }
        throw new IllegalArgumentException("Supplied source " + source + "is not our source " + this.source);
    }

    protected abstract String getValue(S source);

    public S getSource() {
        return source;
    }
}
