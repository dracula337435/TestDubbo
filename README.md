# 使用dubbo的问题

此工程做复现问题及探索用

## 症状

1. provider没有启动，且consumer的check为true，启动consumer不报错，有check“失效”的感觉
1. 若此时consumer调用一次provider，一定失败；但是此时启动provider，再调用，仍然失败
1. 若此时先不调用，直接启动provider，正常调用

## 此工程复现上述症状

1. 配置好java，zookeeper并启动zkServer
1. 注意配置文件src/main/resources/org/dracula/test2/consumer/load-consumer.xml中设置check为true  
```<dubbo:consumer check="true" />```
1. 启动ConsumerMain  
```注意此时可以启动成功，复现症状1```
1. 启动Java Mission Controll，使用JMX，在“MBean树”中找到org.dracula.test2.consumer下的WrapperMannual
1. 在右侧“MBean功能”中选择“操作”选项卡，trySayHello1()，并点击“执行”按钮。注意执行1，不是2
1. 此时可在控制台中看到ConsumerMain打出的异常  
```复现症状2前半部分```
1. 启动Provider1Main和Provider2Main
1. 回到Java Mission Controll，执行trySayHello1()和trySayHello2()  
```注意此时函数1失败，2成功，复现症状2后半部分和症状3```  
至此3个症状均复现

## 初步研究原因

1. 针对症状1。对consumer启动成功的标志定义过早，还没到check起作用时，于是就有了check“失效”的表象。  
具体说明：目前的写法相当于把完整的启动拆成了两个步骤，步骤1初始化spring容器，步骤2手工getBean得到远程代理。
check在步骤2中起作用，七部的用法中@Autowired相当于使用了getBean。  

|不同用法启动成功的标志|
|:---:|

|     | 步骤1 | 步骤2（check起作用） |
|:---:|:----:|:------------------:|
| 二部的 | √ | × |
| 七部的 | √ | √ |

若尝试七部的做法可解开上述load-consumer.xml文件中如下注释配置。此时启动，如果provider不启动，consumer启动抛出异常，表象为check在启动时起作用。
```    
<bean class="org.dracula.test2.consumer.Wrapper" >
    <property name="testInterface1" ref="testInterface1" />
</bean>
```

2. 针对症状2。consumer的getBean过程中
，若check为true，找不到provider，forbidden被置为true，则不再重新找provider
，若check为false，找不到provider，会重试

## 建议
查阅[dubbo官方文档此页](http://dubbo.apache.org/#!/docs/user/demos/preflight-check.md?lang=zh-cn)可得如下信息
>Dubbo 缺省会在启动时检查依赖的服务是否可用，不可用时会抛出异常，阻止 Spring 初始化完成，以便上线时，能及早发现问题，默认 check="true"。

>可以通过 check="false" 关闭检查，比如，测试时，有些服务不关心，或者出现了循环依赖，必须有一方先启动。

>另外，如果你的 Spring 容器是懒加载的，或者通过 API 编程延迟引用服务，请关闭 check，否则服务临时不可用时，会抛出异常，拿到 null 引用，如果 check="false"，总是会返回引用，当服务恢复时，能自动连上。
及试验结果

二部的情况为第三段“通过 API 编程延迟引用服务”  
同时根据试验，建议将check置为false