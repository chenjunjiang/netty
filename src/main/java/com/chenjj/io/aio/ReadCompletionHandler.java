package com.chenjj.io.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @Author: chenjj
 * @Date: 2018-01-29
 * @Description:
 */
public class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

  private AsynchronousSocketChannel asynchronousSocketChannel;

  public ReadCompletionHandler(AsynchronousSocketChannel asynchronousSocketChannel) {
    if (this.asynchronousSocketChannel == null) {
      this.asynchronousSocketChannel = asynchronousSocketChannel;
    }
  }

  @Override
  public void completed(Integer result, ByteBuffer attachment) {
    attachment.flip();
    byte[] body = new byte[attachment.remaining()];
    attachment.get(body);
    try {
      String request = new String(body, "UTF-8");
      System.out.println("The time server receive order : " + request);
      String currentTime =
          "QUERY TIME ORDER".equalsIgnoreCase(request) ? System.currentTimeMillis() + ""
              : "BAD ORDER";
      doWrite(currentTime);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  private void doWrite(String currentTime) {
    if (currentTime != null && currentTime.trim().length() > 0) {
      byte[] bytes = currentTime.getBytes();
      ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
      byteBuffer.put(bytes);
      byteBuffer.flip();
      asynchronousSocketChannel.write(byteBuffer, byteBuffer,
          new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
              // 如果没有发送完成，继续发送
              if (attachment.hasRemaining()) {
                asynchronousSocketChannel.write(attachment, attachment, this);
              }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
              try {
                // 本列为简单demo，并没有对异常进行分类判断，直接关闭链路，释放资源。
                asynchronousSocketChannel.close();
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          });
    }
  }

  @Override
  public void failed(Throwable exc, ByteBuffer attachment) {
    try {
      // 本列为简单demo，并没有对异常进行分类判断，直接关闭链路，释放资源。
      asynchronousSocketChannel.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
