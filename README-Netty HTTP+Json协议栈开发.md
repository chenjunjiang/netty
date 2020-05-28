模拟一个简单的用户订购系统

订购请求信息：
字段名称	    类型	         备注
订购数量	    Int64	     订购的商品数量
客户信息	    Customer	 客户信息，负责POJO对象
账单地址	    Address	     账单的地址
寄送方式	    Shipping	 枚举类型如下：
                         普通邮寄
                         宅急送
                         国际邮递
                         国内快递
                         国际快递

送货地址	    Address	     送货地址
总价	        float	     商品总价

客户信息定义：
字段名称	    类型	         备注
客户ID	    Int64	     客户ID,长整型
姓	        String	     客户姓氏，字符串
名	        String	     客户名字，字符串
全名	        List<String> 客户全称，字符列表

地址信息：
字段名称	    类型	             备注
街道1	    String	 
街道2	    String	 
城市	        String	 
省份	        String	 
邮编	        String	 
国家	        String

邮递方式：
字段名称	    类型	             备注
普通邮递	    枚举类型	 
宅急送	    枚举类型	 
国际邮递	    枚举类型	 
国内快递	    枚举类型	 
国际快递	    枚举类型

流程设计如下：
1、client端构造请求消息，将请求消息编码为HTTP+json格式
2、client端发起连接，通过HTTP协议栈发送HTTP请求消息
3、server端对HTTP+json请求消息进行解码，解码成请求POJO
4、server端构造应答消息并编码，通过HTTP+json方式返回给客户端
5、client端对HTTP+json响应消息进行解码，解码成响应POJO











