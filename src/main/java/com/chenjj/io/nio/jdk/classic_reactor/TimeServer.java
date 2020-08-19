package com.chenjj.io.nio.jdk.classic_reactor;

/**
 * @Author: chenjj
 * @Date: 2018-01-25
 * @Description: 使用nio创建服务端
 */
public class TimeServer {

  /**
   * @class_name: main
   * @param: [args]
   * @creat_date: 2018-01-25
   * @creat_time: 下午 03:01
   **/
  public static void main(String[] args) {
    int port = 8080;
    if (args != null && args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        // 异常之后采用默认值
      }
    }
    MultiplexerTimeServer multiplexerTimeServer = new MultiplexerTimeServer(port);
    /*
    jvm进程退出的时机是：虚拟机中所有存活的线程都是守护线程。只要还有存活的非守护线程虚拟机就不会退出，
    而是等待非守护线程执行完毕；反之，如果虚拟机中的线程都是守护线程，那么不管这些线程的死活java虚拟机都会退出。
     */
    /*new Thread(()->{
      while (true){
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("1111111111111111111");
      }
    }).start();*/
    new Thread(multiplexerTimeServer, "NIO-MultiplexerTimeServer-001").start();
    System.out.println("Main thread end!");
  }
}
