package com.chenjj.io.nio.netty.customProtocol;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.jboss.marshalling.ByteOutput;

/**
 * @Author: chenjj
 * @Date: 2018-02-13
 * @Description:
 */
public class ChannelBufferByteOutput implements ByteOutput {

  private final ByteBuf byteBuf;

  public ChannelBufferByteOutput(ByteBuf byteBuf) {
    this.byteBuf = byteBuf;

  }

  @Override
  public void write(int b) throws IOException {
    byteBuf.writeByte(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    byteBuf.writeBytes(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    byteBuf.writeBytes(b, off, len);
  }

  @Override
  public void close() throws IOException {
    // nothing to do
  }

  @Override
  public void flush() throws IOException {
    // nothing to do
  }

  ByteBuf getByteBuf() {
    return byteBuf;
  }
}
