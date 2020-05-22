package com.chenjj.io.nio.netty.codec.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: chenjj
 * @Date: 2018-02-06
 * @Description:
 */
public class TestSubscribeReqProto {

    private static byte[] encode(SubscribeReqProto.SubscribeReq subscribeReq) {
        return subscribeReq.toByteArray();
    }

    private static SubscribeReqProto.SubscribeReq decode(byte[] body)
            throws InvalidProtocolBufferException {
        return SubscribeReqProto.SubscribeReq.parseFrom(body);
    }

    private static SubscribeReqProto.SubscribeReq createSubscribeReq() {
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setSubReqID(1);
        builder.setUserName("chenjunjiang");
        builder.setProductName("Netty");
        List<String> address = new ArrayList<>();
        address.add("chengdu");
        address.add("beijing");
        address.add("xiamen");
        builder.addAllAddress(address);
        return builder.build();
    }

    /**
     * 编码之前后解码之后是一致的
     *
     * @param args
     * @throws InvalidProtocolBufferException
     */
    public static void main(String[] args) throws InvalidProtocolBufferException {
        SubscribeReqProto.SubscribeReq subscribeReq = createSubscribeReq();
        System.out.println("Before encode : " + subscribeReq.toString());
        SubscribeReqProto.SubscribeReq subscribeReq1 = decode(encode(subscribeReq));
        System.out.println("After decode : " + subscribeReq1.toString());
        System.out.println("Assert equal : --> " + subscribeReq.equals(subscribeReq1));
    }
}
