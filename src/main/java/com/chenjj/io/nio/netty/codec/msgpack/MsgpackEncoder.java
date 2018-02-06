package com.chenjj.io.nio.netty.codec.msgpack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class MsgpackEncoder extends MessageToByteEncoder<Object> {

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
    MessagePack messagePack = new MessagePack();
    // Serialize
    byte[] raw = messagePack.write(msg);
    // 写入到ByteBuf
    out.writeBytes(raw);
  }
}
