package com.github.stephenc.continuous.k2p.injectable;

import com.github.stephenc.continuous.k2p.MissingValueStrategy;
import com.github.stephenc.continuous.k2p.source.FileInjectionSource;
import java.io.File;

public class FileInjectable extends AbstractInjectable<FileInjectionSource> {
    public FileInjectable(String name, MissingValueStrategy strategy,
                             String path) {
        super(name, strategy, new FileInjectionSource(new File(path)));
    }

    @Override
    protected String getValue(FileInjectionSource source) {
        return source.getValue();
    }
}
