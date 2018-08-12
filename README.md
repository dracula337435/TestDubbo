# 使用dubbo的问题

此工程做复现问题及探索用

## 症状

使用dubbo 2.5.4，zookeeper 3.4.10，spring 4.2.1
1. provider没有启动，且consumer的check为true，启动consumer不报错，有check“失效”的感觉
1. 若此时consumer调用一次provider，一定失败；但是此时启动provider，再调用，仍然失败
1. 若此时先不调用，直接启动provider，正常调用

## 此工程复现上述症状

1. 配置好java，zookeeper并启动zkServer
1. 注意配置文件src/main/resources/org/dracula/test2/consumer/load-consumer.xml中设置check为true  
```<dubbo:consumer check="true" />```
1. 启动ConsumerMain  
```注意此时可以启动成功，复现症状1```
1. 启动Java Mission Control，使用JMX，在“MBean树”中找到org.dracula.test2.consumer下的WrapperMannual
1. 在右侧“MBean功能”中选择“操作”选项卡，trySayHello1()，并点击“执行”按钮。注意执行1，不是2
1. 此时可在控制台中看到ConsumerMain打出的异常  
```复现症状2前半部分```
1. 启动Provider1Main和Provider2Main
1. 回到Java Mission Control，执行trySayHello1()和trySayHello2()  
```注意此时函数1失败，2成功，复现症状2后半部分和症状3```  
至此3个症状均复现  
另使用spring 5.0.4，dubbo 2.6.0均可复现

## 简单解释

1. 针对症状1。对consumer启动成功的标志定义过早，还没到check起作用时，于是就有了check“失效”的表象。  

2. 针对症状2和症状3。dubbo机制本身就是consumer的getBean过程中,
若check为true，找不到provider，getBean抛出异常，此后再getBean会返回null；
若check为false，无论是否找到provider，都可以做出远程代理

## 具体解释

1. 针对症状1
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
或
```
    @Autowired
    TestInterface1 testInterface1;
```
其他同样可起到“在初始化时就做出远程代理”的方法有：
在reference或consumer标签，置init为true  

这几种途径可将getBean上提前到start阶段，进而导致start阶段出错
由此可进一步断定问题出在getBean时

2. 针对症状2和症状3  
症状2和症状3的重大区别在于调用前是否使用getBean调用，对症状1的分析可说明这个区别造成的影响，检查provider是否可用发生在调用getBean时，症状2前半部分在provider不可用时调用getBean，抛出异常。  
源码（dubbo的ReferenceConfig类，406行）类似如下
```
    if(check 且 provider不可用){
        抛出异常
    }
    /*略*/
    制作远程代理
```
至此可知有无远程代理决定着症状2前半部分和症状3的对比  
  
再看症状2后半部分，启动provider后consumer一直不能使用，原因为：  
在一次失败的代理创建后，dubbo没有给这个代理第二次创建的机会，这解释了第二次getBean的失败；第三次及以后，spring直接从缓存中取代理，而拿到的一直是一个空对象，这解释了其后的getBean失败。  
每一次getBean背后发生了什么及源码位置如下表所示：

|第几次getBean|背后发生了什么|源码位置|
|:---:|:---:|:---:|
|1|initialized属性被置true|dubbo的ReferenceConfig类，185行|
|1|抛出异常|dubbo的ReferenceConfig类，467行|
|2|受initialized属性影响，不尝试制作代理而是直接返回null|dubbo的ReferenceConfig类，183行|
|2|放入缓存|spring的FactoryBeanRegistrySupport，118行|
|3及以后|从缓存取|spring的AbstractBeanFactory，1635行|
注：源码使用版本，spring 5.0.4，dubbo 2.5.4


## 建议

查阅[dubbo官方文档此页](http://dubbo.apache.org/#!/docs/user/demos/preflight-check.md?lang=zh-cn)可得如下信息
>Dubbo 缺省会在启动时检查依赖的服务是否可用，不可用时会抛出异常，阻止 Spring 初始化完成，以便上线时，能及早发现问题，默认 check="true"。

>可以通过 check="false" 关闭检查，比如，测试时，有些服务不关心，或者出现了循环依赖，必须有一方先启动。

>另外，如果你的 Spring 容器是懒加载的，或者通过 API 编程延迟引用服务，请关闭 check，否则服务临时不可用时，会抛出异常，拿到 null 引用，如果 check="false"，总是会返回引用，当服务恢复时，能自动连上。

二部的情况为第三段“通过 API 编程延迟引用服务”  
同时根据试验，建议将check置为false

不用逐个在reference中配置check，可在<dubbo:consumer>标签中设置check属性。consumer标签的设置可被reference标签覆盖