package com.chenjj.io.aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @Author: chenjj
 * @Date: 2018-01-29
 * @Description:
 */
public class AcceptCompletionHandler implements
    CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler> {

  @Override
  public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
    // 因为AsynchronousServerSocketChannel可以接收成千上万个客户端，所以需要继续调用它的accept方法，接收其它的客户端
    // 连接，最终形成一个循环。每当接收一个客户端连接成功之后，再异步接收新的客户端连接。
    attachment.asynchronousServerSocketChannel.accept(attachment, this);
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    // 异步读取客户端的请求消息
    result.read(byteBuffer, byteBuffer, new ReadCompletionHandler(result));
  }

  @Override
  public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
    attachment.countDownLatch.countDown();
  }
}
