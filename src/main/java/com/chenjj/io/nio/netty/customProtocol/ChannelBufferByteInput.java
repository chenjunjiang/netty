package com.chenjj.io.nio.netty.customProtocol;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

import org.jboss.marshalling.ByteInput;

/**
 * @Author: chenjj
 * @Date: 2018-02-12
 * @Description:
 */
public class ChannelBufferByteInput implements ByteInput {

    private final ByteBuf byteBuf;

    public ChannelBufferByteInput(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public int read() throws IOException {
        if (byteBuf.isReadable()) {
            return byteBuf.readByte() & 0xff;
        }
        return -1;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int available = available();
        if (available == 0) {
            return -1;
        }
        len = Math.min(available, len);
        byteBuf.readBytes(b, off, len);

        return len;
    }

    @Override
    public int available() throws IOException {
        return byteBuf.readableBytes();
    }

    @Override
    public long skip(long n) throws IOException {
        int readable = byteBuf.readableBytes();
        if (readable < n) {
            n = readable;
        }
        byteBuf.readerIndex((int) (byteBuf.readerIndex() + n));

        return n;
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }
}
