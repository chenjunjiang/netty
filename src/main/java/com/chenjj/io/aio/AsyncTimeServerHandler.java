package com.chenjj.io.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: chenjj
 * @Date: 2018-01-29
 * @Description:
 */
public class AsyncTimeServerHandler implements Runnable {

  private int port;

  CountDownLatch countDownLatch;
  AsynchronousServerSocketChannel asynchronousServerSocketChannel;

  public AsyncTimeServerHandler(int port) {
    this.port = port;
    try {
      asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
      asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
      System.out.println("The Time server is start in port: " + port);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    countDownLatch = new CountDownLatch(1);
    doAccept();
    try {
      // 让线程在此阻塞，防止服务端执行完成退出。实际项目中不需要启动独立的线程来处理AsynchronousServerSocketChannel
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void doAccept() {
    asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
  }
}
