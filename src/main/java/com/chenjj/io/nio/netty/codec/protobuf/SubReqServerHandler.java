package com.chenjj.io.nio.netty.codec.protobuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: chenjj
 * @Date: 2018-02-06
 * @Description:
 */
public class SubReqServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 由于ProtobufDecoder已经对消息进行了自动解码，因此接收到请求消息可以直接使用。
        SubscribeReqProto.SubscribeReq subscribeReq = (SubscribeReqProto.SubscribeReq) msg;
        if ("chenjunjiang".equalsIgnoreCase(subscribeReq.getUserName())) {
            System.out
                    .println("Service accept client subscribe request : [" + subscribeReq.toString() + "]");
            // 由于使用了ProtobufEncoder，所以不需要对SubscribeRespProto.SubscribeResp手工编码
            ctx.writeAndFlush(resp(subscribeReq.getSubReqID()));
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
