package com.github.stephenc.continuous.k2p.injectable;

import com.github.stephenc.continuous.k2p.MissingValueStrategy;
import com.github.stephenc.continuous.k2p.source.ConfigmapInjectionSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigmapInjectable extends AbstractInjectable<ConfigmapInjectionSource> {
    private static final Pattern PATH = Pattern.compile("^(?://(?<namespace>[^/]+)/)(?<name>[^/]+)/(?<key>[^/]+)$");
    private final String key;

    public ConfigmapInjectable(String name, MissingValueStrategy strategy, String path) {
        super(name, strategy, new ConfigmapInjectionSource(path));
        final Matcher matcher = PATH.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed path to configmap value: " + path);
        }
        key = matcher.group("key");
    }

    @Override
    protected String getValue(ConfigmapInjectionSource source) {
        return source.getValues().getData().get(key);
    }
}
