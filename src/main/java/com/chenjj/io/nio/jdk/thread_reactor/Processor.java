package com.chenjj.io.nio.jdk.thread_reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * process方法并不直接处理I/O请求，而是把该I/O操作提交给上述线程池去处理，这样就充分利用了多线程的优势，
 * 同时将对新连接的处理和读/写操作的处理放在了不同的线程中，读/写操作不再阻塞对新连接请求的处理。
 */
public class Processor {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(16);

    public void process(SelectionKey selectionKey) {
        System.out.println("接收到请求数据,提交给线程池处理......" + selectionKey);
        executorService.submit(() -> {
            try {
                System.out.println("开始处理请求数据......" + Thread.currentThread().getName());
                // Read the data
                SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // 因为上面已经将SocketChannel设置为异步非阻塞模式，因此read是非阻塞的，所以读取到的字节数有三种情况
                int readBytes = socketChannel.read(readBuffer);
                if (readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order: " + body + " " + Thread.currentThread().getName());
                    String currentTime =
                            "QUERY TIME ORDER".equalsIgnoreCase(body) ? System.currentTimeMillis() + ""
                                    : "BAD ORDER";
                    doWrite(socketChannel, currentTime);
                } else if (readBytes < 0) {
                    System.out.println("对端链路已经关闭......" + Thread.currentThread().getName());
                    // 对端链路已经关闭，需要关闭SocketChannel，释放资源
                    selectionKey.cancel();
                    socketChannel.close();
                } else {
                    // 读到0字节，忽略，属于正常现象
                    System.out.println("读到0字节......" + Thread.currentThread().getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
                /**
                 * 发生异常后需要关闭SocketChannel，释放资源。
                 * 比如： 客户端强制或正常关闭连接之后，这里就会收到异常信息，
                 * 如果这里不关闭SocketChannel，那么服务端就认为这个连接还存在，那么前面的while循环中就一直
                 * 能取到该连接的事件信息，但是客户端已经关闭了，不再发送数据过来了，就会导致上面的
                 * int readBytes = socketChannel.read(readBuffer);
                 * 这句代码一直抛异常。如果关闭了SocketChannel，那么前面的while循环就取不到该连接的任何信息了，
                 * 也就不会执行到这里来了。它就在那里循环等待新的连接请求。
                 */
                if (selectionKey != null) {
                    selectionKey.cancel();
                    if (selectionKey.channel() != null) {
                        try {
                            selectionKey.channel().close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void doWrite(SocketChannel socketChannel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            System.out.println("response: " + response);
            System.out.println();
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            socketChannel.write(writeBuffer);
        }
    }
}
