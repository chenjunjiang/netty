package com.chenjj.io.nio.netty.trafficshaping;

import io.netty.channel.ChannelHandlerContext;

/**
 * 如果待写数据大小是否超过了我们设定的最大写数据大小，unwritable标识就变为非0，说明这时我们不应该再继续进行write操作了
 * （这里写操作一般为通过ChannelHandlerContext或Channel进行的写操作）。
 * "Channel#isWritable()"方法将返回false，此时Channel变为不可写状态。
 * 随着数据的输出，unwritable这个标识(它就在isWritable方法里面)会在0与非0之间
 * 改变，此时就是会触发ChannelWritabilityChanged事件，以通知ChannelPipeline中的各个ChannelHandler当前Channel可写性发生了改变。
 * 其实就是会调用channelWritabilityChanged方法，我们会根据Channel是否可写来决定是否继续发送消息。
 */
public class MyServerHandlerForPlain extends MyServerCommonHandler {
    @Override
    protected void sentData(ChannelHandlerContext ctx) {
        sentFlag = true;
        ctx.writeAndFlush(tempStr, getChannelProgressivePromise(ctx, channelProgressiveFuture -> {
            if (ctx.channel().isWritable() && !sentFlag) {
                sentData(ctx);
            }
        }));
    }

    /**
     * 当前Channel的可写状态发生变化之后就会调用该方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // Channel可写之后继续发送消息
        if (ctx.channel().isWritable()) {
            System.out.println(" ###### 重新开始写数据 ######");
            if (!sentFlag) {
                System.out.println(" ++++++++ 发送新数据包 ++++++++");
                sentData(ctx);
            } else {
                System.out.println(" ===== 写暂停，等前面的数据全部写完 =====");
            }
        }
    }
}
