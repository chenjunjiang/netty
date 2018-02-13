package com.chenjj.io.nio.netty.customProtocol;

import com.chenjj.io.nio.netty.codec.marshalling.MarshallingCodeFactory;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Unmarshaller;

/**
 * @Author: chenjj
 * @Date: 2018-02-12
 * @Description: 消息解码工具类
 */
public class MarshallingDecoder {

  private final Unmarshaller unmarshaller;

  public MarshallingDecoder() throws IOException {
    unmarshaller = MarshallingCodeFactory.buildUnMarshalling();
  }

  protected Object decode(ByteBuf in) throws Exception {
    int objectSize = in.readInt();
    ByteBuf byteBuf = in.slice(in.readerIndex(), objectSize);
    ByteInput input = new ChannelBufferByteInput(byteBuf);
    try {
      unmarshaller.start(input);
      Object object = unmarshaller.readObject();
      unmarshaller.finish();
      in.readerIndex(in.readerIndex() + objectSize);

      return object;
    } finally {
      unmarshaller.close();
    }
  }
}
