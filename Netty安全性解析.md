# Netty安全性解析
RSA加密、解密、签名、验签的原理及方法
https://www.cnblogs.com/pcheng/p/9629621.html
https 单向认证和双向认证！
https://www.cnblogs.com/kabi/p/11629603.html


  Netty通过SslHandler提供了对SSL的支持，它支持的SSL协议类型包括：SSL V2、SSL V3和TLS。
  
  1、SSL单向认证
  单向认证，即客户端只验证服务端的合法性，服务端不用验证客户端。
  首先，利用JDK的keytool工具，Netty服务端依次生成服务端的密钥对和证书仓库、服务端自签名证书。
  生成Netty服务端私钥和证书仓库命令：
  $ keytool -genkey -alias securechat -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass sNetty -storepass sNetty -keystore sChat.jks
  执行之后会有警告：
  Warning:
  JKS 密钥库使用专用格式。建议使用 "keytool -importkeystore -srckeystore sChat.jks -destkeystore sChat.jks -deststoretype pkcs12" 迁移到行业标准格式 PKCS12。
  再次执行警告中的命令：
  keytool -importkeystore -srckeystore sChat.jks -destkeystore sChat.jks -deststoretype pkcs12
  输入源密钥库口令: sNetty
  已成功导入别名 securechat 的条目。
  已完成导入命令: 1 个条目成功导入, 0 个条目失败或取消
  Warning:
  已将 "sChat.jks" 迁移到 Non JKS/JCEKS。将 JKS 密钥库作为 "sChat.jks.old" 进行了备份。
  
  生成Netty服务端自签名证书：
  $ keytool -export -alias securechat -keystore sChat.jks -storepass sNetty -file sChat.cer
  
  生成客户端的密钥对和证书仓库，用于将服务端的证书保存到客户端的授信证书仓库中
  keytool -genkey -alias smcc -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass cNetty -storepass cNetty -keystore cChat.jks
  执行之后会有警告：
  Warning:
  JKS 密钥库使用专用格式。建议使用 "keytool -importkeystore -srckeystore cChat.jks -destkeystore cChat.jks -deststoretype pkcs12" 迁移到行业标准格式 PKCS12。
  再次执行警告中的命令：
  keytool -importkeystore -srckeystore cChat.jks -destkeystore cChat.jks -deststoretype pkcs12
  输入源密钥库口令:cNetty
  已成功导入别名 smcc 的条目。
  已完成导入命令: 1 个条目成功导入, 0 个条目失败或取消
  Warning:
  已将 "cChat.jks" 迁移到 Non JKS/JCEKS。将 JKS 密钥库作为 "cChat.jks.old" 进行了备份。
  
  随后，将Netty服务端的证书导入到客户端的证书仓库中：
  keytool -import -trustcacerts -alias securechat -file sChat.cer -storepass cNetty -keystore cChat.jks
  所有者: CN=localhost
  发布者: CN=localhost
  序列号: 1380fee4
  有效期为 Tue Aug 18 14:58:35 CST 2020 至 Wed Aug 18 14:58:35 CST 2021
  证书指纹:
           MD5:  0F:44:D3:D6:22:06:CD:73:DB:07:1E:6F:1C:F6:69:A0
           SHA1: 38:12:3C:67:81:8C:71:1C:9F:7B:0A:45:CF:85:54:C2:02:39:87:28
           SHA256: E8:61:8F:AA:64:88:E3:C9:66:BA:7B:9C:D2:B3:90:F4:98:4C:95:29:99:2B:23:55:E8:77:76:B1:47:5F:73:6F
  签名算法名称: SHA256withRSA
  主体公共密钥算法: 2048 位 RSA 密钥
  版本: 3
  
  扩展:
  
  #1: ObjectId: 2.5.29.14 Criticality=false
  SubjectKeyIdentifier [
  KeyIdentifier [
  0000: 3D 2E D1 75 5E 4E DB 89   B2 C7 2A AF F8 34 3C B8  =..u^N....*..4<.
  0010: 76 04 88 88                                        v...
  ]
  ]
  是否信任此证书? [否]:  是
  证书已添加到密钥库中
  
  
  注意：如果需要打印SSL握手的日志，需要添加-Djavax.net.debug=ssl,handshake虚拟机参数
  
  2、单向认证原理
  SSL单向认证的过程总结如下：
  a、SSL客户端向服务端传送客户端SSL协议的版本号、支持的加密算法种类、产生的随机数，以及其它可选信息；
  b、服务端给客户端返回SSL协议版本号、加密算法种类、随机数等信息，同时也返回服务器端的证书，即公钥证书；
  c、客户端使用服务端返回的信息验证服务器的合法性，包括：
     证书是否过期
     发型服务器证书的CA是否可靠
     返回的公钥是否能正确解开返回证书中的数字签名
     服务器证书上的域名是否和服务器的实际域名相匹配
     验证通过后，将继续进行通信，否则，终止通信
  d、客户端向服务端发送自己所能支持的对称加密方案，供服务器端进行选择；
  e、服务器端在客户端提供的加密方案中选择加密程度最高的加密方式；
  f、服务器将选择好的加密方案通过明文方式返回给客户端；
  g、客户端接收到服务端返回的加密方式后，使用该加密方式生成产生随机码，用作通信过程中对称加密的密钥，使用服务端返回的公钥进行加密，将加密后的随机码发送至服务器；
  h、服务器收到客户端返回的加密信息后，使用自己的私钥进行解密，获取对称加密密钥。 在接下来的会话中，服务器和客户端将会使用该密码进行对称加密，保证通信过程中信息的安全；
  
  
  3、双向认证
  双向认证和单向认证原理基本差不多，只是除了客户端需要认证服务端以外，增加了服务端对客户端的认证。
  首先，利用JDK的keytool工具，Netty服务端依次生成服务端的密钥对和证书仓库、服务端自签名证书。
  生成Netty服务端私钥和证书仓库命令：
  $ keytool -genkey -alias securechat -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass sNetty -storepass sNetty -keystore sChat.jks
  执行之后会有警告：
  Warning:
  JKS 密钥库使用专用格式。建议使用 "keytool -importkeystore -srckeystore sChat.jks -destkeystore sChat.jks -deststoretype pkcs12" 迁移到行业标准格式 PKCS12。
  再次执行警告中的命令：
  keytool -importkeystore -srckeystore sChat.jks -destkeystore sChat.jks -deststoretype pkcs12
  输入源密钥库口令: sNetty
  已成功导入别名 securechat 的条目。
  已完成导入命令: 1 个条目成功导入, 0 个条目失败或取消
  Warning:
  已将 "sChat.jks" 迁移到 Non JKS/JCEKS。将 JKS 密钥库作为 "sChat.jks.old" 进行了备份。
  
  生成Netty服务端自签名证书：
  $ keytool -export -alias securechat -keystore sChat.jks -storepass sNetty -file sChat.cer
  
  生成客户端的密钥对和证书仓库，用于将服务端的证书保存到客户端的授信证书仓库中
  keytool -genkey -alias smcc -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass cNetty -storepass cNetty -keystore cChat.jks
  执行之后会有警告：
  Warning:
  JKS 密钥库使用专用格式。建议使用 "keytool -importkeystore -srckeystore cChat.jks -destkeystore cChat.jks -deststoretype pkcs12" 迁移到行业标准格式 PKCS12。
  再次执行警告中的命令：
  keytool -importkeystore -srckeystore cChat.jks -destkeystore cChat.jks -deststoretype pkcs12
  输入源密钥库口令:cNetty
  已成功导入别名 smcc 的条目。
  已完成导入命令: 1 个条目成功导入, 0 个条目失败或取消
  Warning:
  已将 "cChat.jks" 迁移到 Non JKS/JCEKS。将 JKS 密钥库作为 "cChat.jks.old" 进行了备份。
  
  随后，将Netty服务端的证书导入到客户端的证书仓库中：
  keytool -import -trustcacerts -alias securechat -file sChat.cer -storepass cNetty -keystore cChat.jks
  所有者: CN=localhost
  发布者: CN=localhost
  序列号: 123eeb29
  有效期为 Wed Aug 19 10:04:05 CST 2020 至 Thu Aug 19 10:04:05 CST 2021
  证书指纹:
           MD5:  21:A5:86:4D:9A:0A:B8:3A:0A:A5:BD:FA:6F:7C:2A:25
           SHA1: 4C:0B:74:DD:90:08:4A:8D:2F:C0:7F:AF:98:96:45:24:5A:0F:21:75
           SHA256: 0C:A1:83:7B:EE:D3:70:85:A0:8B:8B:95:19:1F:A5:8B:B3:C6:55:38:DE:06:AA:6A:1A:C0:11:05:08:35:F6:2A
  签名算法名称: SHA256withRSA
  主体公共密钥算法: 2048 位 RSA 密钥
  版本: 3
  
  扩展:
  
  #1: ObjectId: 2.5.29.14 Criticality=false
  SubjectKeyIdentifier [
  KeyIdentifier [
  0000: E8 19 0B 6D C4 AD FE 46   8D 1B AF 4C E3 58 48 93  ...m...F...L.XH.
  0010: 74 FD 45 46                                        t.EF
  ]
  ]
  
  是否信任此证书? [否]:  是
  证书已添加到密钥库中
  
  生成客户端的自签名证书：
  keytool -export -alias smcc -keystore cChat.jks -storepass cNetty -file cChat.cer
  存储在文件 <cChat.cer> 中的证书
  
  然后将客户端的自签名证书导入到服务端的信任证书仓库中：
  keytool -import -trustcacerts -alias smcc -file cChat.cer -storepass sNetty -keystore sChat.jks
  所有者: CN=localhost
  发布者: CN=localhost
  序列号: 1e4d2b5f
  有效期为 Wed Aug 19 10:04:58 CST 2020 至 Thu Aug 19 10:04:58 CST 2021
  证书指纹:
           MD5:  B8:41:70:E2:7F:FF:9F:CF:FA:77:88:A6:3B:42:F6:05
           SHA1: 50:41:10:27:03:73:54:51:D4:BA:DB:F6:0B:02:59:81:8F:C2:5C:9E
           SHA256: FD:D0:61:05:0D:CE:4D:D1:49:E3:F1:D9:DE:F5:8B:D4:24:7D:B4:16:B2:2F:69:6D:A3:59:85:B7:6E:C5:13:F2
  签名算法名称: SHA256withRSA
  主体公共密钥算法: 2048 位 RSA 密钥
  版本: 3
  
  扩展:
  
  #1: ObjectId: 2.5.29.14 Criticality=false
  SubjectKeyIdentifier [
  KeyIdentifier [
  0000: FE 09 1D 27 E1 2F 8E D1   B9 81 A3 F5 69 F0 3D 7D  ...'./......i.=.
  0010: CC DF 62 0F                                        ..b.
  ]
  ]
  
  是否信任此证书? [否]:  是
  证书已添加到密钥库中
  
  4、双向认证原理：
  a、客户端向服务端发送SSL协议版本号、加密算法种类、随机数等信息。
  b、服务端给客户端返回SSL协议版本号、加密算法种类、随机数等信息，同时也返回服务器端的证书，即公钥证书
  c、客户端使用服务端返回的信息验证服务器的合法性，包括：
  证书是否过期
  发型服务器证书的CA是否可靠
  返回的公钥是否能正确解开返回证书中的数字签名
  服务器证书上的域名是否和服务器的实际域名相匹配
  验证通过后，将继续进行通信，否则，终止通信
  d、服务端要求客户端发送客户端的证书，客户端会将自己的证书发送至服务端
  e、验证客户端的证书，通过验证后，会获得客户端的公钥
  f、客户端向服务端发送自己所能支持的对称加密方案，供服务器端进行选择
  g、服务器端在客户端提供的加密方案中选择加密程度最高的加密方式
  h、将加密方案通过使用之前获取到的公钥进行加密，返回给客户端
  i、客户端收到服务端返回的加密方案密文后，使用自己的私钥进行解密，获取具体加密方式，而后，产生该加密方式的随机码，用作加密过程中的密钥，使用之前从服务端证书中获取到的公钥进行加密后，发送给服务端
  j、服务端收到客户端发送的消息后，使用自己的私钥进行解密，获取对称加密的密钥，在接下来的会话中，服务器和客户端将会使用该密码进行对称加密，保证通信过程中信息的安全。