package com.chenjj.io.nio.jdk.many_reactor;

/**
 * 多Reactor模式
 * <p>
 * Netty中使用的Reactor模式，引入了多Reactor(具体到Java就是对应了多个Selector)，也即一个主Reactor负责监控所有的连接请求，
 * 多个子Reactor负责监控并处理读/写请求，减轻了主Reactor的压力，降低了主Reactor压力太大而造成的延迟。
 * 并且每个子Reactor分别属于一个独立的线程，每个成功连接后的Channel的所有操作由同一个线程处理。
 * 这样保证了同一请求的所有状态和上下文在同一个线程中，避免了不必要的上下文切换，同时也方便了监控请求响应状态。
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
        new Thread(multiplexerTimeServer, "NIO-MultiplexerTimeServer-003").start();
    }
}
