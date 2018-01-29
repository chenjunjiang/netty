package com.chenjj.io.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: chenjj
 * @Date: 2018-01-29
 * @Description:
 */
public class AsyncTimeClientHandler implements CompletionHandler<Void, AsyncTimeClientHandler>,
    Runnable {

  private AsynchronousSocketChannel asynchronousSocketChannel;
  private String host;
  private int port;
  private CountDownLatch countDownLatch;

  public AsyncTimeClientHandler(String host, int port) {
    this.host = host;
    this.port = port;
    try {
      asynchronousSocketChannel = AsynchronousSocketChannel.open();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    printTrack();
    countDownLatch = new CountDownLatch(1);
    asynchronousSocketChannel.connect(new InetSocketAddress(host, port), this, this);
    try {
      countDownLatch.await();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void completed(Void result, AsyncTimeClientHandler attachment) {
    printTrack();
    byte[] request = "QUERY TIME ORDER".getBytes();
    ByteBuffer writeBuffer = ByteBuffer.allocate(request.length);
    writeBuffer.put(request);
    writeBuffer.flip();
    asynchronousSocketChannel.write(writeBuffer, writeBuffer,
        new CompletionHandler<Integer, ByteBuffer>() {
          @Override
          public void completed(Integer result, ByteBuffer attachment) {
            // printTrack();
            if (attachment.hasRemaining()) {
              asynchronousSocketChannel.write(attachment, attachment, this);
            } else {
              ByteBuffer readBuffer = ByteBuffer.allocate(1024);
              asynchronousSocketChannel.read(readBuffer, readBuffer,
                  new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                      printTrack();
                      attachment.flip();
                      byte[] bytes = new byte[attachment.remaining()];
                      attachment.get(bytes);
                      String body;
                      try {
                        body = new String(bytes, "UTF-8");
                        System.out.println("Now is : " + body);
                        countDownLatch.countDown();
                      } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                      }
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                      try {
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
              asynchronousSocketChannel.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        });
  }

  @Override
  public void failed(Throwable exc, AsyncTimeClientHandler attachment) {
    try {
      asynchronousSocketChannel.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 通过打印线程堆栈可以发现，JDK底层通过线程池ThreadPoolExecutor来执行回调通知， 异步回调通知类由AsynchronousChannelGroupImpl实现，经过层层调用，最终回调completed方法。
   */
  private void printTrack() {
    Thread currentThread = Thread.currentThread();
    System.out
        .println(currentThread.toString() + "java.lang.Thread State:" + currentThread.getState());
    StackTraceElement[] st = currentThread.getStackTrace();
    if (st == null) {
      System.out.println("无堆栈...");
      return;
    }
    StringBuffer sbf = new StringBuffer();
    for (StackTraceElement e : st) {
      if (sbf.length() > 0) {
        sbf.append(" <- ");
        sbf.append(System.getProperty("line.separator"));
      }
      sbf.append(java.text.MessageFormat.format("{0}.{1}() {2}"
          , e.getClassName()
          , e.getMethodName()
          , e.getLineNumber()));
    }
    System.out.println(sbf.toString());
  }
}
