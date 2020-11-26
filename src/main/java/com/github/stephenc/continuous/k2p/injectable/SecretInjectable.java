package com.github.stephenc.continuous.k2p.injectable;

import com.github.stephenc.continuous.k2p.MissingValueStrategy;
import com.github.stephenc.continuous.k2p.source.SecretInjectionSource;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecretInjectable extends AbstractInjectable<SecretInjectionSource> {
    private static final Pattern PATH = Pattern.compile("^(?://(?<namespace>[^/]+)/)(?<name>[^/]+)/(?<key>[^/]+)$");
    private final String key;

    public SecretInjectable(String name, MissingValueStrategy strategy, String path) {
        super(name, strategy, new SecretInjectionSource(path));
        final Matcher matcher = PATH.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed path to secret value: " + path);
        }
        key = matcher.group("key");
    }

    @Override
    protected String getValue(SecretInjectionSource source) {
        return new String(source.getValues().getData().get(key), StandardCharsets.UTF_8);
    }

    @Override
    public boolean isRedacted() {
        return true;
    }
}
