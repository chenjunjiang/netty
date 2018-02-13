package com.chenjj.io.nio.netty.customProtocol;

/**
 * @Author: chenjj
 * @Date: 2018-02-12
 * @Description: 数据结构定义
 */
public final class NettyMessage {

  // 消息头
  private Header header;
  // 消息体
  private Object body;

  public final Header getHeader() {
    return header;
  }

  public final void setHeader(Header header) {
    this.header = header;
  }

  public final Object getBody() {
    return body;
  }

  public final void setBody(Object body) {
    this.body = body;
  }

  @Override
  public String toString() {
    return "NettyMessage [" + "header=" + header + "]";
  }
}
