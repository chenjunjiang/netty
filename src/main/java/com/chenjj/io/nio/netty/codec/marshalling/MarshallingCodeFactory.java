package com.chenjj.io.nio.netty.codec.marshalling;

import io.netty.handler.codec.marshalling.DefaultMarshallerProvider;
import io.netty.handler.codec.marshalling.DefaultUnmarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallingDecoder;
import io.netty.handler.codec.marshalling.MarshallingEncoder;
import io.netty.handler.codec.marshalling.UnmarshallerProvider;
import java.io.IOException;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * @Author: chenjj
 * @Date: 2018-02-07
 * @Description:
 */
public class MarshallingCodeFactory {

  public static MarshallingDecoder buildMarshallingDecoder() {
    final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
    final MarshallingConfiguration configuration = new MarshallingConfiguration();
    configuration.setVersion(5);
    UnmarshallerProvider provider = new DefaultUnmarshallerProvider(marshallerFactory,
        configuration);
    // 1024指的是单个消息序列化后的最大长度
    MarshallingDecoder decoder = new MarshallingDecoder(provider, 1024);

    return decoder;
  }

  public static MarshallingEncoder buildMarshallingEncoder() {
    final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
    final MarshallingConfiguration configuration = new MarshallingConfiguration();
    configuration.setVersion(5);
    MarshallerProvider provider = new DefaultMarshallerProvider(marshallerFactory, configuration);
    MarshallingEncoder encoder = new MarshallingEncoder(provider);

    return encoder;
  }

  public static Marshaller buildMarshalling() throws IOException {
    final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
    final MarshallingConfiguration configuration = new MarshallingConfiguration();
    configuration.setVersion(5);
    Marshaller marshaller = marshallerFactory.createMarshaller(configuration);

    return marshaller;
  }

  public static Unmarshaller buildUnMarshalling() throws IOException {
    final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
    final MarshallingConfiguration configuration = new MarshallingConfiguration();
    configuration.setVersion(5);
    Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);

    return unmarshaller;
  }
}
