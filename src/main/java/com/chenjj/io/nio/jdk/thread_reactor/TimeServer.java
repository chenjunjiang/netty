package com.chenjj.io.nio.jdk.thread_reactor;

/**
 * 多工作线程Reactor模式
 * <p>
 * 经典Reactor模式中，尽管一个线程可同时监控多个连接（Socket），
 * 但是所有读/写请求以及对新连接请求的处理都在同一个线程中处理，无法充分利用多CPU的优势，
 * 而且读/写操作也会阻塞对新连接请求的处理。因此可以引入多线程，并行处理多个读/写操作。
 */
public class TimeServer {
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
        new Thread(multiplexerTimeServer, "NIO-MultiplexerTimeServer-002").start();
    }
}
