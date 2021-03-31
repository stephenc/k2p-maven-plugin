package com.github.stephenc.continuous.k2p.util;

public interface Sink<T> {
    void accept(T t);
}
