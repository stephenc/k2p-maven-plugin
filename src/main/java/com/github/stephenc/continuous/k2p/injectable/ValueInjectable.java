package com.github.stephenc.continuous.k2p.injectable;

import com.github.stephenc.continuous.k2p.MissingValueStrategy;
import com.github.stephenc.continuous.k2p.source.ValueInjectionSource;

public class ValueInjectable extends AbstractInjectable<ValueInjectionSource> {
    public ValueInjectable(String name, MissingValueStrategy strategy, String value) {
        super(name, strategy, new ValueInjectionSource(value));
    }

    @Override
    protected String getValue(ValueInjectionSource source) {
        return source.getValue();
    }
}
