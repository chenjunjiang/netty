# Netty ByteBuf解析
https://blog.csdn.net/yjw123456/article/details/77843931

当我进行数据传输的时候，往往需要使用到缓冲区，常用的缓冲区就是JDK NIO类库提供的java.nio.Buffer。
实际上，7种基础类型(Boolean除外)都有自己的缓冲区实现。对于NIO编程而言，我们主要使用的是ByteBuffer。从更能
角度而言，ByteBuffer完全可以满足NIO编程的需要，但是由于NIO编程的复杂性，ByteBuffer也有其局限性。主要缺点
如下：
1、ByteBuffer长度固定，一旦分配完成，它的容量不能动态扩展和收缩，当需要编码的POJO对象大于ByteBuffer的容量
时，会发送索引越界异常；
2、ByteBuffer只有一个标识位置的指针position，读写的时候需要手工调用flip()和rewind()等，使用者必须小心谨慎
地处理这些API，否则很容易导致程序处理失败；
3、ByteBuffer的API功能有限，一些高级和实用的特性它不支持，需要使用者自己编程实现。
为了弥补这些不足，Netty提供了自己的ByteBuffer实现-ByteBuf。

ByteBuf的工作原理
首先，ByteBuf依然是个Byte数组的缓冲区，它的基本功能应该与JDK的ByteBuffer一致，提供了以下几类基本功能：
1、7中Java基础类型、byte数组、ByteBuffer(ByteBuf)等的读写；
2、缓冲区自身的copy和slice等；
3、设置网络字节序；
4、构造缓冲区实例；
5、操作位置指针等方法。
由于JDK的ByteBuffer已经提供了这些基础能力的实现，因此，Netty ByteBuf的实现可以有两种策略：
1、参考JDK ByteBuffer的实现，增加额外的功能，解决原ByteBuffer的缺点；
2、聚合JDK ByteBuffer，通过Facade模式对其进行包装，可以减少自身的代码量，降低实现成本。
JDK ByteBuffer由于只有一个位置指针用于处理读写操作，因此每次读写的时候都需要额外调用flip()和clear()等方法，
否则功能将出错。
    ByteBuf通过两个位置指针来协助缓冲区的读写操作，读操作使用readerIndex，写操作使用writerIndex。
readerIndex和writerIndex的取值一开始是0，随着数据的写入writerIndex会增加，读取数据会使readerIndex
增加，但是它不会超过writerIndex。在读取之后，0~readerIndex就将被视为discard的，调用discardReadBytes方法，
可以释放这部分空间，它的作用类似ByteBuffer的compact方法。readerIndex和writerIndex之间的数据是可读取的，等价于
ByteBuffer position和limit之间的数据。writerIndex和capacity之间的空间是可写的，等价于ByteBuffer limit
和capacity之间的可用空间。
    由于写操作不修改readerIndex指针，读操作不修改writerIndex指针，因此读写之间不再需要调整位置执行，这极大地
简化了缓冲区的读写操作，避免了由于遗漏或者不熟悉flip()操作导致的功能异常。
    ByteBuf是如何实现动态扩展的呢？通常情况下，当我们对ByteBuffer进行put操作的时候，如果缓冲区剩余可写空间不够，
就会发生BufferOverflowException异常。为了避免发生这个问题，通常在进行put操作的时候会对剩余可用空间进行校验。
如果空间不足，需要重新创建一个新的ByteBuffer，并将之前的ByteBuffer复制到新创建的ByteBuffer中，最后释放老的ByteBuffer。
代码示例如下：
if (this.buffer.remaining() < needSize)
{
   int toBeExtSize = needSize > 128 ? needSize : 128;
   ByteBuffer tmpBuffer = ByteBuffer.allocate(this.buffer.capacity()+toBeExtSize);
   this.buffer.flip();
   tmpBuffer.put(this.buffer);
   this.buffer = tmpBuffer;
}
    从上面的代码可以看出，为了防止ByteBuffer溢出，每进行一次put操作，都需要对可用空间进行校验，这导致了代码冗余，稍有不慎，
就可能引入其它问题。为了解决这个问题，ByteBuf对write操作进行了封装，由ByteBuf的write操作负责进行剩余可用空间的校验。
如果可用缓冲区不足，ByteBuf会自动进行动态扩展。对于使用者而言，不需要关心底层的校验和扩展细节，只要不超过设置的最大
缓冲区容量即可。当可用空间不足时，ByteBuf会帮助我们实现自动扩展。
    @Override
    public ByteBuf writeByte(int value) {
        ensureWritable0(1);
        _setByte(writerIndex++, value);
        return this;
    }

ByteBuf的功能介绍
    1、顺序读操作(read)
    ByteBuf的read操作类似于ByteBuffer的get操作，主要API功能说明如下：
    方法名称                              返回值          功能说明
    readBoolean                          boolean        返回当前readerIndex处的Boolean值，然后将readerIndex加1
    readByte                             byte           返回当前readerIndex处的byte值，然后将readerIndex加1
    readUnsignedByte                     byte           返回当前readerIndex处的无符号byte值，然后将readerIndex加1
    readShort                            short          返回当前readerIndex处的short值，然后将readerIndex加2
    readUnsignedShort                    short          返回当前readerIndex处的无符号short值，然后将readerIndex加2
    readMedium                           int            返回当前readerIndex处的24位整型值，然后将readerIndex加3(该类型并非Java的基本类型，大多数场景用不到)
    readUnsignedMedium                   int            返回当前readerIndex处的无符号24位整型值，然后将readerIndex加3(该类型并非Java的基本类型，大多数场景用不到)
    readInt                              int            返回当前readerIndex处的int值，然后将readerIndex加4
    readUnsignedInt                      int            返回当前readerIndex处的无符号int值，然后将readerIndex加4
    readLong                             long           返回当前readerIndex处的long值，然后将readerIndex加8
    readChar                             char           返回当前readerIndex处的char值，然后将readerIndex加2
    readFloat                            float          返回当前readerIndex处的float值，然后将readerIndex加4
    readDouble                           double         返回当前readerIndex处的double值，然后将readerIndex加8
    readBytes(int length)                ByteBuf        将当前ByteBuf中的数据读取到新创建的ByteBuf中，读取的长度为
                                                        length。操作完成之后，返回的ByteBuf的readerIndex为0，writerIndex
                                                        为length。如果读取长度length大于当前操作的ByteBuf的可写字节数，将抛出
                                                        IndexOutOfBoundsException，操作失败
    readSlice(int length)                ByteBuf        返回当前ByteBuf新创建的子区域，子区域与原ByteBuf共享缓冲区，
                                                        但是独立维护自己的readerIndex和writerIndex。新创建的子区域readerIndex
                                                        为0，writerIndex为length。如果读取长度length大于当前操作的ByteBuf的可写字节数，
                                                        将抛出IndexOutOfBoundsException，操作失败
    readBytes(ByteBuf dst)               ByteBuf        将当前ByteBuf中的数据读取到目标ByteBuf中，直到目标ByteBuf没有
                                                        剩余的空间可写。操作完成后，当前ByteBuf的readerIndex+=读取的字节数，
                                                        如果目标ByteBuf可写的字节数小于当前ByteBuf可读取的字节数，
                                                        将抛出IndexOutOfBoundsException，操作失败
    readBytes(ByteBuf dst, int length)   ByteBuf        将当前ByteBuf中的数据读取到目标ByteBuf中，读取的字节数长度为
                                                        length，操作完成之后，当前ByteBuf的readerIndex+=length，
                                                        如果需要读取的字节数长度大于当前ByteBuf可读字节数或者目标ByteBuf
                                                        可写的字节数，将抛出IndexOutOfBoundsException，操作失败
    readBytes(ByteBuf dst, int dstIndex
    , int length)                        ByteBuf        将当前ByteBuf中的数据读取到目标ByteBuf中，读取的字节数长度
                                                        为length，目标ByteBuf的起始索引为dstIndex，非writerIndex，
                                                        操作完成之后，当前ByteBuf的readerIndex+=length，如果读取
                                                        的字节数长度length大于当前ByteBuf可读的字节数或者dstIndex
                                                        小于0，或者dstIndex+length大于目标ByteBuf的capacity，
                                                        将抛出IndexOutOfBoundsException，操作失败
    readBytes(byte[] dst)                ByteBuf        将当前ByteBuf中的数据读取到目标byte数组中，读取的字节数长度为dst.length，
                                                        操作完成之后，当前ByteBuf的readerIndex+=dst.length，如果目标字节数组
                                                        的长度大于当前ByteBuf的可读字节数，
                                                        将抛出IndexOutOfBoundsException，操作失败
    readBytes(byte[] dst, int dstIndex
    , int length)                        ByteBuf        将当前ByteBuf中的数据读取到目标byte数组中，读取的字节数长度
                                                        为length，目标字节数组的起始索引为dstIndex，如果dstIndex，
                                                        小于0，或者length大于当前ByteBuf的可读字节数，或者dstIndex+length大于
                                                        dst.length，将抛出IndexOutOfBoundsException，操作失败
    readBytes(ByteBuffer dst)            ByteBuf        将当前ByteBuf中的数据读取到目标ByteBuffer数组中，直到位置指针
                                                        达到ByteBuffer的limit，操作完成之后，当前ByteBuf的
                                                        readerIndex+=dst.remaining()，如果目标ByteBuffer的可写
                                                        字节数大于当前ByteBuf的可读字节数，将抛出IndexOutOfBoundsException，
                                                        操作失败
    readBytes(OutputStream out,          ByteBuf        
    int length)                                         将当前ByteBuf中的数据读取到目标输出流中，读取的字节数长度为length，
                                                        如果操作成功，当前ByteBuf的readerIndex+=length，如果length大于
                                                        当前ByteBuf的可读取字节数， 将抛出IndexOutOfBoundsException，操作失败
                                                        ，如果读取过程中OutputStream自身发生了IO异常，则抛出IoException
    readBytes(GatheringByteChannel out   int
    , int length)                                       将当前ByteBuf中的数据写入到目标GatheringByteChannel中，写入的最大字节数
                                                        长度为length。注意： 由于GatheringByteChannel是非阻塞的，调用它的write
                                                        操作并不能保证一次能够将所有需要写入的字节数都写入成功，即存在"写半包问题"，
                                                        因此，它写入的字节数范围为[0,length]。如果操作成功，当前ByteBuf的
                                                        readerIndex+=实际写的字节数。如果需要写入的length大于当前ByteBuf的可读
                                                        字节数，则抛出IndexOutOfBoundsException；如果操作过程中GatheringByteChannel
                                                        发生了IO异常，则抛出IoException，无论抛出何种异常，操作都将失败，与其它
                                                        read方法不同的是，本方法返回值不是当前的ByteBuf，而是写GatheringByteChannel
                                                        的实际字节数
                                                        

    2、顺序写操作(write)
    ByteBuf的write操作类似于ByteBuffer的put操作，主要API功能说明如下：
    方法名称                               返回值         功能说明
    writeBoolean(boolean value)           ByteBuf       将参数value写入到当前的ByteBuf中，操作成功之后writerIndex+=1，
                                                        如果当前ByteBuf可写的字节数小于1，则抛出IndexOutOfBoundsException，
                                                        操作失败
    writeByte(int value)                  ByteBuf       将参数value写入到当前的ByteBuf中，操作成功之后writerIndex+=1， 
                                                        如果当前ByteBuf可写的字节数小于1，则抛出IndexOutOfBoundsException，
                                                        操作失败
    writeShort(int value)                 ByteBuf       将参数value写入到当前的ByteBuf中，操作成功之后writerIndex+=2， 
                                                        如果当前ByteBuf可写的字节数小于2，则抛出IndexOutOfBoundsException，
                                                        操作失败
    writeMedium(int value)                ByteBuf       将参数value写入到当前的ByteBuf中，操作成功之后writerIndex+=3， 
                                                        如果当前ByteBuf可写的字节数小于3，则抛出IndexOutOfBoundsException，
                                                        操作失败
    writeInt(int value)                   ByteBuf       将参数value写入到当前的ByteBuf中，操作成功之后writerIndex+=4， 
                                                        如果当前ByteBuf可写的字节数小于4，则抛出IndexOutOfBoundsException，
                                                        操作失败
    writeLong(int value)                 ByteBuf        将参数value写入到当前的ByteBuf中，操作成功之后writerIndex+=8， 
                                                        如果当前ByteBuf可写的字节数小于8，则抛出IndexOutOfBoundsException，
                                                        操作失败
    writeChar(int value)                 ByteBuf        将参数value写入到当前的ByteBuf中，操作成功之后writerIndex+=2， 
                                                        如果当前ByteBuf可写的字节数小于2，则抛出IndexOutOfBoundsException，
                                                        操作失败
    writeBytes(ByteBuf src)              ByteBuf        将源ByteBuf src中的所有可读字节写入到当前ByteBuf中，操作成功之后当前
                                                        ByteBuf的writerIndex+=src.readableBytes。如果源ByteBuf src可读的
                                                        字节数大于当前ByteBuf的可写字节数，则抛出IndexOutOfBoundsException,
                                                        操作失败
    writeBytes(ByteBuf src, int length)  ByteBuf         将源ByteBuf src中的所有可读字节写入到当前ByteBuf中，写入的字节数长度为length。
                                                        操作成功之后，当前ByteBuf的writerIndex+=length。如果length大于源
                                                        ByteBuf的可读字节数或者当前ByteBuf的可写字节数，
                                                        则抛出IndexOutOfBoundsException,操作失败
    writeBytes(ByteBuf src, 
    int srcIndex, int length)           ByteBuf         将源ByteBuf src中的所有可读字节写入到当前ByteBuf中，写入的字节数长度
                                                        为length，起始索引为srcIndex。操作成功之后，当前ByteBuf的writerIndex+=length。
                                                        如果srcIndex小于0，或者scrIndex+length大于源ByteBuf src的容量；或者
                                                        写入长度length大于当前ByteBuf的可写字节数，
                                                        则抛出IndexOutOfBoundsException,操作失败
    writeBytes(byte[] src)              ByteBuf         将源字节数组src中的所有字节写入到当前ByteBuf中。操作成功之后，当前ByteBuf
                                                        的writerIndex+=src.length；如果源字节数组src的长度大于当前ByteBuf的可写
                                                        字节数，则抛出IndexOutOfBoundsException,操作失败
    writeBytes(byte[] src, 
    int srcIndex, int length)           ByteBuf         将源字节数组src中的字节写入到当前ByteBuf中，写入的字节数长度
                                                        为length，起始索引为srcIndex。
                                                        操作成功之后，当前ByteBuf的writerIndex+=length。
                                                        如果srcIndex小于0，或者scrIndex+length大于源src的容量；或者
                                                        写入长度length大于当前ByteBuf的可写字节数，
                                                        则抛出IndexOutOfBoundsException,操作失败
    writeBytes(ByteBuffer src)        ByteBuf           将源ByteBuffer src中的可读字节写入到当前ByteBuf中，写入的长度为
                                                        src.remaining()。操作成功之后，当前ByteBuf的writerIndex+=src.remaining()
                                                        ，如果源ByteBuffer src的可读字节数大于当前ByteBuf的可写字节数，
                                                        则抛出IndexOutOfBoundsException,操作失败
    writeBytes(InputStream src, 
    int length)                       int                将源InputStream src中的内容写入到当前ByteBuf中，写入的最大字节数长度为
                                                        length。实际写入的字节数可能小于length。操作成功之后，当前ByteBuf的
                                                        writerIndex+=实际写入的字节数；如果length大于源ByteBuf的可读字节数或者
                                                        当前ByteBuf的可写字节数，则抛出IndexOutOfBoundsException,操作失败
                                                        如果InputStream读取的时候发生了IO异常，则抛出IOException
    writeBytes(
    ScatteringByteChannel src, 
    int length)                      int               将源ScatteringByteChannel src中的内容写入到当前ByteBuf中，写入的最大
                                                       字节数长度为length，实际写入的字节数可能小于length。操作成功之后，当前
                                                       ByteBuf的writerIndex+=实际写入的字节数；如果length大于源src的可读字节数
                                                       或者当前ByteBuf的可写字节数，则抛出IndexOutOfBoundsException,操作失败；
                                                       如果ScatteringByteChannel读取的时候发生了IO异常，则抛出IOException
    writeZero(int length)           ByteBuf            将当前缓冲区内容填充为NUL(0x00)，起始位置为writerIndex，填充的长度为length
                                                       ，填充成功之后，writerIndex+=length；如果length大于当前ByteBuf的可写字节
                                                       数则抛出IndexOutOfBoundsException,操作失败


    3、readerIndex和writerIndex
    Netty提供了两个指针变量用于支持顺序读取和写入操作：readerIndex用于标识服务索引，writerIndex用于标识写入索引。两个位置指针
    把ByteBuf缓冲区分成了三个区域：
    discardable bytes    |   readable bytes    |   writeable bytes
    0     <=       readerIndex       <=    writerIndex      <=    capacity
    调用ByteBuf的read操作时，从readerIndex处开始读取。readerIndex到writerIndex之间的空间为可读的字节缓冲区；从writerIndex
    到capacity之间为可写的字节缓冲区；0到readerIndex之间是已经读取过的缓冲区，可以调用discardReadBytes操作来重用这部分空间，
    以节约内存，防止ByteBuf的动态扩张。这在私有协议栈消息解码的时候非常有用，因为TCP底层可能粘包，几百个整包消息被TCP粘包之后
    作为一个整包发送。这样，通过discardReadBytes操作可以重用之前已经解过码的缓冲区，从而防止接收缓冲区因为容量不足导致的扩张。
    但是，discardReadBytes操作是把双刃剑，不能滥用。
    
    4、discardable bytes
    相比于其它的java对象，缓冲区的分配和释放是个耗时的操作，因为，我们需要尽量重用它们。由于缓冲区的动态扩张需要进行字节数组的复制，
    它是个耗时的操作，因此， 为了最大限度地提升性能，往往需要尽最大努力提升缓冲区的重用率。
    假如缓冲区包含了N个整包消息，每个消息的长度为L，消息的可写字节数为R。当读取M个整包消息后，如果不对ByteBuf做压缩或者
    discardReadBytes操作，则可写的缓冲区长度依然为R。如果调用discardReadBytes操作，则可写字节数会变为R=(R+M*L)，之前已经读取
    的M个整包的空间会被重用。假如此时ByteBuf需要写入R+1个字节，则不需要动态扩张ByteBuf。
    ByteBuf的discardReadBytes操作效果图如下：
    操作之前：
    discardable bytes    |   readable bytes    |   writeable bytes
    0     <=       readerIndex       <=    writerIndex      <=    capacity
    操作之后：
    readable bytes    |   writeable bytes(got more space)
    readerIndex(0) <= writerIndex(decreased)                  <=    capacity
    需要指出的是，调用discardReadBytes会发生字节数组的内存复制，所以，频繁调用将会导致性能下降，因此在调用它之前要确认你确实需要
    这样做，例如牺牲性能来换取更多的可用内存。调用discardReadBytes操作之后的writeable bytes内容处理策略跟ByteBuf接口的具体实现有关。
    
    5、readable bytes和writeable bytes
    可读空间段是数据实际存储的区域，以read或者skip开头的任何操作都将会从readerIndex开始读取或者跳过指定的数据，操作完成之后readerIndex
    增加了读取或者跳过的字节数长度。如果读取的字节数长度大于实际可读的字节数，则抛出IndexOutOfBoundsException。当新分配、包装
    或者复制一个新的ByteBuf对象时，它的readerIndex为0。
    可写空间段是尚未被使用可以填充的空闲空间，任何以write开头的操作都会从writerIndex开始向空闲空间写入字节，操作完成之后，writerIndex
    增加了写入的字节数长度。如果写入的字节数大于可写的字节数，则会抛出IndexOutOfBoundsException异常。新分配一个ByteBuf对象时，
    它的writerIndex为0，通过包装或者复制的方式创建一个新的ByteBuf对象时，它的writerIndex是ByteBuf的容量。
    
    6、clear操作
    正如JDK ByteBuffer的clear操作，它并不会清空缓冲区内容本身，例如填充为NUL(0x00)。它主要用来操作位置指针，例如position、limit和
    mark。对于ByteBuf，它也是用来操作readerIndex和writerIndex，将它们还原为初始分配值。
    clear之前：
    discardable bytes    |   readable bytes    |   writeable bytes
    0     <=       readerIndex       <=    writerIndex      <=    capacity
    clear之后：
                           writeable bytes(got more space)
    0 = readerIndex = writerIndex                           <=    capacity
    
    7、mark和reset
    当对缓冲区进行读操作时，由于某种原因，可能需要对之前的操作进行回滚。读操作并不会改变缓冲区的内容，回滚操作主要就是重新设置索引信息。
    对于JDK的ByteBuffer，调用mark操作会将当前的位置指针备份到mark变量中，当调用reset之后，重新将指针的当前位置恢复为备份mark中的值。
    Netty的ByteBuf也有类似的rest和mark接口，因为ByteBuf有读索引和写索引，因此，它总共有4个相关的方法：
    markReaderIndex：将当前readerIndex备份到markedReaderIndex中；
    restReaderIndex：将当前readerIndex设置为markedReaderIndex；
    markWriterIndex：将当前writerIndex备份到markedWriterIndex中；
    resetWriterIndex：将当前writerIndex设置为markedWriterIndex；
    
    8、查找操作
    很多时候，需要从ByteBuf中查找某个字符串，例如通过"\r\n"作为文本字符串的换行符，利用"NUL(0x00)"作为分隔符。
    ByteBuf提供了多种查找方法用于满足不同的应用场景：
    (1) indexOf(int fromIndex, int toIndex, byte value)：从当前ByteBuf中定位出首次出现value的位置。起始索引为fromIndex，
    终点是toIndex。如果没有查找到则返回-1，否则返回第一条满足搜索条件的位置索引。
    (2) bytesBefore(byte value)：从当前ByteBuf中定位首次出现value的位置。起始索引为readerIndex，终点是writerIndex。如果没有
    查找到则返回-1，否则返回第一条满足搜索条件的位置索引。该方法不会修改readerIndex和writerIndex。
    (3) bytesBefore(int length, byte value)：从当前ByteBuf中定位首次出现value的位置。起始索引为readerIndex，
    终点是readerIndex+length。如果没有查找到则返回-1，否则返回第一条满足搜索条件的位置索引。如果length大于当前缓冲区的可读字节数，则
    抛出IndexOutOfBoundsException异常。
    (4) bytesBefore(int index, int length, byte value)： 从当前ByteBuf中定位首次出现value的位置。起始索引为index，
    终点是index+length。如果没有查找到则返回-1，否则返回第一条满足搜索条件的位置索引。如果index+length大于当前缓冲区的容量，则抛出
    IndexOutOfBoundsException异常。
    (5) forEachByte(ByteBufProcessor processor)： 遍历当前ByteBuf的可读字节数组。与ByteBufProcessor设置的查找条件进行对比。
    如果满足条件，则返回位置索引，否则返回-1。
    (6) forEachByte(int index, int length, ByteBufProcessor processor)： 以index为起始位置，index+length为终止位置进行遍历，
    与ByteBufProcessor设置的查找条件进行对比，如果满足条件，则返回位置索引，否则返回-1。
    (7) forEachByteDesc(ByteBufProcessor processor)： 遍历当前ByteBuf的可读字节数组。与ByteBufProcessor设置的查找条件进行对比。
    如果满足条件，则返回位置索引，否则返回-1。注意对字节数组进行迭代的时候采用逆序的方式，也就是从writerIndex-1开始迭代，直到readerIndex。
    (8) forEachByteDesc(int index, int length, ByteBufProcessor processor)： 以index为起始位置，index+length为终止位置进行遍历，
    与ByteBufProcessor设置的查找条件进行对比，如果满足条件，则返回位置索引，否则返回-1。采用逆序查找的方式，从index+length-1开始，
    直到index。
    对于查找的字节而言，存在一些常用值，例如回车换行符、常用的分隔符等。Netty为了减少业务的重复定义， 在ByteBufProcessor接口中对这些常用
    的查找字节进行了抽象。
    
    9、derived buffers
    类似于数据库的视图，ByteBuf提供了多个接口用于创建某个ByteBuf的视图或者复制ByteBuf：
    (1) duplicate：返回当前ByteBuf的复制对象，复制返回的ByteBuf与操作的ByteBuf共享缓冲区内容，但是维护自己独立的读写索引。当修改复制后
    的ByteBuf内容后，之前的ByteBuf的内容也随之改变，双方持有的是同一个内容指针引用。
    (2) copy：复制一个新的ByteBuf对象，它的内容和索引都是独立的，复制操作本身并不会修改原ByteBuf的读写索引。
    (3) copy(int index, int length)： 从指定的索引开始复制，复制的字节长度为length，复制后的ByteBuf内容和读写索引都与之前的独立。
    (4) slice：返回当前ByteBuf的可读子缓冲区，起始位置从readerIndex到writerIndex，返回后的ByteBuf与原ByteBuf共享内容，但是读写索引
    独立维护。该操作并不会修改原ByteBuf的readerIndex和writerIndex。
    (5) slice(int index, int length)：返回当前ByteBuf的可读子缓冲区，起始位置从index到index+length，返回后的ByteBuf与原ByteBuf
    共享内容，但是读写索引独立维护。该操作并不会修改原ByteBuf的readerIndex和writerIndex。
    
    10、转换成标准的ByteBuffer
    我们知道， 当通过NIO的SocketChannel进行网络读写时，操作的对象是JDK标准的ByteBuffer。由于netty统一使用ByteBuf替代原生的ByteBuffer
    ，所以必须从接口层面支持两者的相互转换，将ByteBuf转换成ByteBuffer的方法有两个：
    (1) ByteBuffer nioBuffer(): 将当前ByteBuf可读的缓冲区转换成ByteBuffer，两者共享一个缓冲区内容引用，对ByteBuffer的读写操作并不会
    修改原yteBuf的读写索引。需要指出的是，返回后的ByteBuffer无法感知原ByteBuf的动态扩展操作。
    (2) ByteBuffer nioBuffer(int index, int length)：将当前ByteBuf从index开始长度为length的缓冲区转换成ByteBuffer，
    两者共享一个缓冲区内容引用，对ByteBuffer的读写操作并不会修改原yteBuf的读写索引。
    需要指出的是，返回后的ByteBuffer无法感知原ByteBuf的动态扩展操作。
    
    11、随机读写(set和get)
    除了顺序读写之外，ByteBuf还支持随机读写，它与顺序读写最大的差别在于可以随机指定读写的索引位置。无论是get操作还是set操作，ByteBuf都会
    对其索引和长度等进行合法性校验，与顺序读写一直。但是，set操作与write操作不同的是它不支持动态扩展缓冲区，所以使用者必须保证当前的缓冲区
    可写的字节数大于需要写入的字节长度，否则会抛出数组或者缓冲区越界异常。

ByteBuf相关辅助类

    1、ByteBufHolder
    ByteBufHolder是ByteBuf的容器，在Netty中，它非常有用。例如HTTP协议的请求消息和应答消息都可以携带消息体，这个消息体在NIO ByteBuffer
    中就是个ByteBuffer对象，在Netty中就是ByteBuf对象。由于不同的协议消息体可以包含不同的协议字段和功能，因此，需要对ByteBuf进行包装和
    抽象，不同的子类可以有不同的实现。为了满足这些定制化的需求，Netty抽象出了ByteBufHolder对象，它包含一个ByteBuf，另外提供了一些其它
    实用的方法， 使用者继承ByteBufHolder接口后可以按需封装自己的实现。
    
    2、ByteBufAllocator
    ByteBufAllocator是字节缓冲区分配器，按照Netty的缓冲区实现不同，共有两种不同的分配器：基于内存池的缓冲区分配器和普通的字节缓冲区分配器。
    ByteBufAllocator主要API功能如下：
    方法名称                               返回值说明                             功能说明
    buffer()                              ByteBuf                               分配一个字节缓冲区，缓冲区的类型由
                                                                                ByteBufAllocator的实现类决定
    buffer(int capacity)                  ByteBuf                               分配一个初始容量为capacity的字节缓冲区，缓冲区的类型由
                                                                                ByteBufAllocator的实现类决定
    buffer(int capacity, 
    int maxCapacity)                      ByteBuf                               分配一个初始容量为capacity，最大容量为maxCapacity的
                                                                                字节缓冲区，缓冲区的类型由ByteBufAllocator的实现类决定
    ioBuffer(int capacity, 
        int maxCapacity)                  ByteBuf                               分配一个初始容量为capacity，最大容量为maxCapacity的
                                                                                 direct buffer，因为direct buffer的IO操作性能更高
    heapBuffer(int capacity, 
            int maxCapacity)              ByteBuf                               分配一个初始容量为capacity，最大容量为maxCapacity的
                                                                                 heap buffer
    
    3、CompositeByteBuf
    CompositeByteBuf允许将多个ByteBuf的实例组装到一起，形成一个统一的视图，有点类似于数据库将多个表的字段组装到一起统一用视图展示。
    CompositeByteBuf在一些场景下非常有用，例如某个协议POJO对象包含两部分：消息头和消息体，它们都是ByteBuf对象。当需要对消息进行编码的
    时候需要进行整合，如果使用JDK的默认能力，有以下两种方式：
    (1) 将某个ByteBuffer复制到另一个ByteBuffer中，或者创建一个新的ByteBuffer，将两者复制到新建的ByteBuffer中；
    (2) 通过List或者数组等容器，将消息头和消息体放到容器中进行统一维护和处理。
    上面的做法非常别扭。实际上我们遇到的问题跟数据库中视图解决的问题一致--缓冲区有多个，但是需要统一展示和处理，必须有存放它们的统一容器。
    为了解决这个问题，Netty提供了CompositeByteBuf。
    
    4、ByteBufUtil
    ByteBufUtil是一个非常有用的工具类，它提供了一系列静态方法用于操作ByteBuf对象。其中最有用的方法就是对字符串的编码和解码：
    (1) ByteBuf encodeString(ByteBufAllocator alloc, CharBuffer src, Charset charset)：对需要编码的字符串src按照
    指定的字符集charset进行编码，利用指定的ByteBufAllocator生成一个新的ByteBuf；
    (2) String decodeString(ByteBuf src, int readerIndex, int len, Charset charset)：使用指定的ByteBuf和charset
    对ByteBuf src进行解码， 获取解码后的字符串。
    还有一个非常有用的方法就是hexDump，它能够将参数ByteBuf的内容以16进制字符串的方式打印出来，用于输出日志或者打印码流，方便问题
    定位，提升系统的可维护性。
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    