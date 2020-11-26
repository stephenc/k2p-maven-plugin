package com.github.stephenc.continuous.k2p.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.codehaus.plexus.util.IOUtil;

public class FileInjectionSource implements InjectionSource {
    private final File file;

    private transient String value;

    public FileInjectionSource(File file) {
        this.file = file;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void resolve() throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            value = IOUtil.toString(is);
        } finally {
            IOUtil.close(is);
        }
    }

    @Override
    public int compareTo(InjectionSource o) {
        if (this == o) {
            return 0;
        }
        if (getClass() != o.getClass()) {
            return o.getClass().getName().compareTo(getClass().getName());
        }
        FileInjectionSource that = (FileInjectionSource) o;

        return file.compareTo(that.file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileInjectionSource that = (FileInjectionSource) o;

        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }
}
