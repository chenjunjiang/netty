package com.chenjj.io.nio.netty.codec.msgpack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import org.msgpack.MessagePack;
import org.msgpack.type.Value;

/**
 * @Author: chenjj
 * @Date: 2018-02-01
 * @Description:
 */
public class MsgpackDecoder extends MessageToMessageDecoder<ByteBuf> {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    final byte[] byteArray;
    final int length = msg.readableBytes();
    byteArray = new byte[length];
    // 读取ByteBuf中的数据到byteArray中
    msg.getBytes(msg.readerIndex(), byteArray, 0, length);
    MessagePack messagePack = new MessagePack();
    // 调用read方法将byteArray反序列化为对象并加入到解码列表中
    Value value = messagePack.read(byteArray);
    out.add(value);
  }
}
