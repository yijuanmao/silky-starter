package com.silky.starter.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * LimitedInputStream 类用于限制输入流的大小
 *
 * @author zy
 * @date 2025-08-20 15:02
 **/
public class LimitedInputStream extends InputStream {
    private final InputStream wrappedStream;
    private long bytesRead;
    private final long maxBytes;

    public LimitedInputStream(InputStream wrappedStream, long startPos, long maxBytes) throws IOException {
        this.wrappedStream = wrappedStream;
        this.maxBytes = maxBytes;
        wrappedStream.skip(startPos);
    }

    @Override
    public int read() throws IOException {
        if (bytesRead >= maxBytes) return -1;
        int result = wrappedStream.read();
        if (result != -1) bytesRead++;
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bytesRead >= maxBytes) return -1;
        int adjustedLen = (int) Math.min(len, maxBytes - bytesRead);
        int result = wrappedStream.read(b, off, adjustedLen);
        if (result != -1) bytesRead += result;
        return result;
    }
}
