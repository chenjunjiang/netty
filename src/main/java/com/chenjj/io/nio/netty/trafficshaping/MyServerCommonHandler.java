package com.chenjj.io.nio.netty.trafficshaping;

import io.netty.channel.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class MyServerCommonHandler extends SimpleChannelInboundHandler<String> {
    protected final int M = 1024 * 1024;
    protected final int KB = 1024;
    protected AtomicLong consumeMsgLength;
    protected Runnable counterTask;
    protected String tempStr;
    private long priorProgress;
    protected boolean sentFlag;

    /**
     * Gets called after the ChannelHandler was added to the actual context and it's ready to handle events.
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        consumeMsgLength = new AtomicLong();
        /**
         * 写一个简单的task来计算每秒数据的发送速率（并非精确的计算）。
         */
        counterTask = () -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                long length = consumeMsgLength.getAndSet(0);
                System.out.println("*** rate（KB/S）：" + (length / KB));
            }
        };
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < M; i++) {
            // 每次添加26个字节
            builder.append("abcdefghijklmnopqrstuvwxyz");
        }
        tempStr = builder.toString();
        System.out.println("tempStr is: " + tempStr);
    }

    protected abstract void sentData(ChannelHandlerContext ctx);

    /**
     * 当有客户端连接上服务端之后就开发给客户端发送消息
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        sentData(ctx);
        new Thread(counterTask).start();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("===== receive client msg : " + msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }

    /**
     * Special ChannelPromise which will be notified once the associated bytes is transferring.
     */
    protected ChannelProgressivePromise getChannelProgressivePromise(ChannelHandlerContext ctx,
                                                                     Consumer<ChannelProgressiveFuture> completedAction) {
        ChannelProgressivePromise channelProgressivePromise = ctx.newProgressivePromise();
        channelProgressivePromise.addListener(new ChannelProgressiveFutureListener() {
            /**
             * 通过当前channel，每次把消息发送到网络上都会调用该方法，统计到的数据大小就是每次发送出去的消息大小
             * @param future
             * @param progress
             * @param total
             * @throws Exception
             */
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                // 由于progress表示累积的值，所以要统计每次新增的就需要减去之前的
                System.out.println("每次发送的数据大小是:" + (progress - priorProgress));
                consumeMsgLength.addAndGet(progress - priorProgress);
                priorProgress = progress;
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                sentFlag = false;
                if (future.isSuccess()) {
                    System.out.println("发送成功！！！！！");
                    // tempStr的长度就是26M
                    priorProgress -= 26 * M;
                    //Optional.ofNullable(completedAction).ifPresent(action -> action.accept(future));
                } else {
                    System.out.println("发送失败！！！！！");
                    future.cause().printStackTrace();
                }
            }
        });
        return channelProgressivePromise;
    }
}
