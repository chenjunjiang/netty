package com.chenjj.io.nio.netty.customProtocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @Author: chenjj
 * @Date: 2018-02-13
 * @Description:
 */
public class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {

  MarshallingEncoder marshallingEncoder;

  public NettyMessageEncoder() throws IOException {
    this.marshallingEncoder = new MarshallingEncoder();
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, NettyMessage msg, ByteBuf out) throws Exception {
    if (msg == null || msg.getHeader() == null) {
      throw new Exception("The encode message is null");
    }
    out.writeInt((msg.getHeader().getCrcCode()));
    out.writeInt((msg.getHeader().getLength()));
    out.writeLong((msg.getHeader().getSessionID()));
    out.writeByte((msg.getHeader().getType()));
    out.writeByte((msg.getHeader().getPriority()));
    out.writeInt((msg.getHeader().getAttachment().size()));
    String key = null;
    byte[] keyArray = null;
    Object value = null;
    for (Entry<String, Object> param : msg.getHeader().getAttachment()
        .entrySet()) {
      key = param.getKey();
      keyArray = key.getBytes("UTF-8");
      out.writeInt(keyArray.length);
      out.writeBytes(keyArray);
      value = param.getValue();
      marshallingEncoder.encode(value, out);
    }
    key = null;
    keyArray = null;
    value = null;
    if (msg.getBody() != null) {
      marshallingEncoder.encode(msg.getBody(), out);
    } else {
      out.writeInt(0);
    }
    out.setInt(4, out.readableBytes() - 8);
  }
}
