package com.chenjj.io.nio.netty.codec.protobuf;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: chenjj
 * @Date: 2018-02-06
 * @Description:
 */
public class SubReqClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for (int i = 1; i < 11; i++) {
            // 每一次write都会使用ProtobufVarint32LengthFieldPrepender和ProtobufEncoder对消息进行编码
            ctx.write(subReq(i));
        }
        ctx.flush();
    }

    private SubscribeReqProto.SubscribeReq subReq(int i) {
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setSubReqID(i);
        builder.setUserName("chenjunjiang");
        builder.setProductName("Netty Book For Protobuf");
        List<String> address = new ArrayList<>();
        address.add("chengdu");
        address.add("beijing");
        address.add("xiamen");
        builder.addAllAddress(address);
        return builder.build();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SubscribeRespProto.SubscribeResp subscribeResp = (SubscribeRespProto.SubscribeResp) msg;
        System.out.println("Receive server response : [" + subscribeResp.getSubReqID() + ", " +
                subscribeResp.getRespCode() + ", " + subscribeResp.getDesc() + "]");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
