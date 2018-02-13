package com.chenjj.io.nio.netty.customProtocol;

import com.chenjj.io.nio.netty.codec.marshalling.MarshallingCodeFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import java.io.IOException;
import org.jboss.marshalling.Marshaller;

/**
 * @Author: chenjj
 * @Date: 2018-02-13
 * @Description:
 */
@Sharable
public class MarshallingEncoder {

  private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
  Marshaller marshaller;

  public MarshallingEncoder() throws IOException {
    marshaller = MarshallingCodeFactory.buildMarshalling();
  }

  protected void encode(Object msg, ByteBuf byteBuf) throws IOException {
    try {
      int lengthPos = byteBuf.writerIndex();
      byteBuf.writeBytes(LENGTH_PLACEHOLDER);
      ChannelBufferByteOutput output = new ChannelBufferByteOutput(byteBuf);
      marshaller.start(output);
      marshaller.writeObject(msg);
      marshaller.finish();
      byteBuf.setInt(lengthPos, byteBuf.writerIndex() - lengthPos - 4);
    } finally {
      marshaller.close();
    }
  }
}
