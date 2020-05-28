https://www.cnblogs.com/ruber/p/10186571.html
# 1、README-ChannelPipeline 添加handler的顺序和执行顺序
pipeline.addLast(new outboundsHandler1()); //out1，编码器1
pipeline.addLast(new outboundsHandler2()); //out2，编码器2
pipeline.addLast(new InboundsHandler1()); //in1，解码器1
pipeline.addLast(new InboundsHandler2()); //in2，解码器2
pipeline.addLast("handler", new HelloServerHandler()); // handler
读取数据的时候，需要解码数据，此时执行顺序和注册顺序一致 in1 --> in2 -->in3，
解码完成后进行业务逻辑处理，然后通过ctx.writeAndFlush()发送数据，
发送数据时需要使用编码器进行编码，out的执行顺是和注册顺序相反的，也就是out2 -->out1，
这就完成了从in -->out的转换。

# 2、ctx.channel().writeAndFlush()和 ctx.writeAndFlush()区别
pipeline.addLast(new InboundsHandler1()); //in1，解码器1
pipeline.addLast(new InboundsHandler2()); //in2，解码器2
pipeline.addLast("handler", new HelloServerHandler()); // handler
pipeline.addLast(new outboundsHandler1()); //out1，编码器1
pipeline.addLast(new outboundsHandler2()); //out2，编码器2
比如注册时out放handler后面，接收执行到handler时，执行ctx.writeAndFlush()，会发生什么呢，
out不会被调用，因为ctx.writeAndFlush()是从当前节点往前查找out，而out节点注册在当前节点后边，
这种情况要想让out执行，handler里面应该执行ctx.channel().writeAndFlush()；
这就会从链表结尾开始往前查找out，这就是两种writeAndFlush的区别。

out继承自ChannelOutboundHandlerAdapter， 通常可以用来编码。
in继承自ChannelInboundHandlerAdapter，通常用来解码。

我们的业务处理handler(接收数据、处理数据、发送数据)是继承自ChannelInboundHandlerAdapter。
