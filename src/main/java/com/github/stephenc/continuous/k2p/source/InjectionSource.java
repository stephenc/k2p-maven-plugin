package com.github.stephenc.continuous.k2p.source;

import java.io.IOException;

/**
 * A source of injection. We group by source as some sources can provide multiple properties and we want to minimize
 * round-trips.
 */
public interface InjectionSource extends Comparable<InjectionSource> {
    void resolve() throws IOException;
}
