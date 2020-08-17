# Netty使用过程中的注意事项

1、不要在非NioEventLoop线程中不停歇的发送非ByteBuf、ByteBufHolder或者FileRegion对象的大数据包，如：
new Thread(() -> {
            while (true) {
                if(ctx.channel().isWritable()) {
                    ctx.writeAndFlush(tempStr, getChannelProgressivePromise(ctx, null));
                }
            }
 }).start();
因为写操作是一个I/O操作，当你在非NioEventLoop线程上执行了Channel的I/O操作的话，该操作会封装为一个task 被提交至NioEventLoop的任务队列中，
以使得I/O操作最终是NioEventLoop线程上得到执行。
而提交这个任务的流程，仅会对ByteBuf、ByteBufHolder或者FileRegion对象进行真实数据大小的估计（其他情况默认估计大小为8 bytes），
并将估计后的数据大小值对该ChannelOutboundBuffer的totalPendingSize属性值进行累加。
而totalPendingSize同WriteBufferWaterMark一起来控制着Channel的unwritable。
所以，如果你在一个非NioEventLoop线程中不断地发送一个非ByteBuf、ByteBufHolder或者FileRegion对象的大数据包时，
最终就会导致提交大量的任务到NioEventLoop线程的任务队列中，而当NioEventLoop线程在真实执行这些task时可能发生OOM。

2、关于 “OP_WRITE” 与 “Channel#isWritable()”
https://www.jianshu.com/p/bea1b4ea8402
首先，我们需要明确的一点是，“OP_WRITE” 与 “Channel#isWritable()” 虽然都是的对数据的可写性进行检测，但是它们是分别针对不同层面的可写性的。
“OP_WRITE”是当内核的发送缓冲区满的时候，我们程序执行write操作（这里是真实写操作了，将数据通过TCP协议进行网络传输）无法将数据写出，
这时我们需要注册OP_WRITE事件。这样当发送缓冲区空闲时就OP_WRITE事件就会触发，我们就可以继续write未写完的数据了。
这可以看做是对系统层面的可写性的一种检测。
而“Channel#isWritable()”则是检测程序中的缓存的待写出的数据大小超过了我们设定的相关最大写数据大小，如果超过了isWritable()方法将返回false，
说明这时我们不应该再继续进行write操作了（这里写操作一般为通过ChannelHandlerContext或Channel进行的写操作）。
