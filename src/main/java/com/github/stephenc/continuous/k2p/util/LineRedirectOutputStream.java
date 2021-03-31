package com.github.stephenc.continuous.k2p.util;

import java.io.OutputStream;

public class LineRedirectOutputStream extends OutputStream {
    private StringBuilder currentLine = new StringBuilder();
    private final Sink<String> linePrinter;

    public LineRedirectOutputStream(Sink<String> linePrinter) {
        this.linePrinter = linePrinter;
    }

    @Override
    public void write(final int b) {
        if ((char) b == '\n') {
            printAndReset();
            return;
        }
        currentLine.append((char) b);
    }

    @Override
    public void flush() {
        if (currentLine.length() > 0) {
            printAndReset();
        }
    }

    private void printAndReset() {
        linePrinter.accept(currentLine.toString());
        currentLine = new StringBuilder();
    }
}
