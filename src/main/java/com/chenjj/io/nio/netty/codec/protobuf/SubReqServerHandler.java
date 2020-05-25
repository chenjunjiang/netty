package com.chenjj.io.nio.netty.codec.protobuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-02-06
 * @Description:
 */
public class SubReqServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * channel可读的时候，就会使用ProtobufVarint32FrameDecoder和ProtobufDecoder
     * 对消息进行解码，然后再调用channelRead方法。
     * 由于客户端在每次写一条消息的时候都用ProtobufVarint32LengthFieldPrepender和ProtobufEncoder进行了编码
     * ，所有这里也是一条一条消息就行解码，只要成功解码一条消息后就会调用channelRead方法
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 由于ProtobufDecoder已经对消息进行了自动解码，因此接收到请求消息可以直接使用。
        SubscribeReqProto.SubscribeReq subscribeReq = (SubscribeReqProto.SubscribeReq) msg;
        if ("chenjunjiang".equalsIgnoreCase(subscribeReq.getUserName())) {
            System.out
                    .println("Service accept client subscribe request : [" + subscribeReq.toString() + "]");
            // 由于使用了ProtobufEncoder，所以不需要对SubscribeRespProto.SubscribeResp手工编码
            ctx.writeAndFlush(resp(subscribeReq.getSubReqID()));
            Thread.sleep(1000);
        }
    }

    private SubscribeRespProto.SubscribeResp resp(int subReqID) {
        SubscribeRespProto.SubscribeResp.Builder builder = SubscribeRespProto.SubscribeResp
                .newBuilder();
        builder.setSubReqID(subReqID);
        builder.setRespCode(0);
        builder.setDesc("Netty book order succeed, 3 days later, sent to the designated address");
        return builder.build();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
