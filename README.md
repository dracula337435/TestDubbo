# 使用dubbo的问题

此工程做复现问题及探索用

## 症状

使用dubbo 2.5.4，zookeeper 3.4.10，spring 4.2.1
1. provider未启动，且consumer的check为true，启动consumer不报错，check参数表现为“失效”状态
1. 在1的前提下，若此时consumer调用一次provider，一定失败；但是此时启动provider，再调用，仍然失败
1. 在1的前提下，若此时先不调用，直接启动provider，正常调用

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
另
使用spring 5.0.4，dubbo 2.6.0均可复现  
增加8080端口访问网站，/sayHello1和/sayHello2分别用于调用接口1和2


## 简单解释

1. 针对症状1。对consumer启动成功的标志定义过早，还没到check起作用时，于是就有了check“失效”的表象。  

1. 针对症状2和症状3。在dubbo机制中，check参数是在consumer的getBean()过程中生效：
若check为true且找不到provider，第一次getBean()抛出异常；结合spring的缓存，此后再进行的getBean()会返回null，造成provider服务后续均无法被正常使用

## 具体解释

1. 针对症状1  
目前二部的写法相当于把完整的启动拆成了两个步骤，步骤1初始化spring容器，步骤2手工getBean()得到远程代理。
check在步骤2中起作用，与之形成对比的是七部的用法，使用@Autowired相当于使用了getBean()，若check为ture且provider不可用，consumer程序启动不成功，即check起作用。  

|不同用法启动成功的标志|
|:---:|

|     | 步骤1 | 步骤2（check起作用） |
|:---:|:----:|:------------------:|
| 二部的 | √ | × |
| 七部的 | √ | √ |

若要将getBean()放入start()中，可使用一下方法
显式注入，解开上述load-consumer.xml文件中如下注释配置。
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
或强制饥饿加载  
可起到“在初始化时就做出远程代理”的方法有：
置init为true
```
    <dubbo:reference interface="TestInterface" init="true">
```
或
```
    <dubbo:consumer init="true">
```

由此可进一步断定问题出在getBean时

2. 针对症状2和症状3  
症状2和症状3的最重要区别在于provider启动前是否调用了getBean()，对症状1的分析可说明这个区别造成的影响，检查provider是否可用发生在调用getBean()时，症状2前半部分在provider不可用时调用getBean()，抛出异常。  
源码（dubbo的ReferenceConfig类，406行）简写如下
```
    if(check 且 provider不可用){
        抛出异常
    }
    /*略*/
    制作远程代理
```
至此可知有无远程代理决定着症状2前半部分和症状3的对比  
  
再看症状2后半部分，启动provider后consumer一直不能使用，原因为：  
在第一次getBean()创建代理的失败后，dubbo不会再次创建这个代理，因此第二次getBean()失败；第三次及以后，spring直接从缓存中取代理，拿到的一直是一个空对象，getBean()从此一直失败。  
以上各次getBean()对应的处理逻辑及源码位置如下表所示：

|次数|处理逻辑|源码位置|
|:---:|:---:|:---:|
|1|initialized属性被置true|dubbo的ReferenceConfig类，185行|
|1|抛出异常|dubbo的ReferenceConfig类，467行|
|2|受initialized属性影响，不尝试制作代理而是直接返回null|dubbo的ReferenceConfig类，183行|
|2|放入缓存|spring的FactoryBeanRegistrySupport，118行|
|3及以后|从缓存取|spring的AbstractBeanFactory，1635行|

注：源码使用版本，spring 5.0.4，dubbo 2.5.4  
注：其实，在第二次getBean()，dubbo返回代理为null，spring将之转为NullBean的实例，放入缓存，取出NullBean后转为null返回；而第一次getBean()，spring接到dubbo的异常，没有到缓存这步。


## 建议

查阅[dubbo官方文档此页](http://dubbo.apache.org/#!/docs/user/demos/preflight-check.md?lang=zh-cn)可得如下信息
>Dubbo 缺省会在启动时检查依赖的服务是否可用，不可用时会抛出异常，阻止 Spring 初始化完成，以便上线时，能及早发现问题，默认 check="true"。

>可以通过 check="false" 关闭检查，比如，测试时，有些服务不关心，或者出现了循环依赖，必须有一方先启动。

>另外，如果你的 Spring 容器是懒加载的，或者通过 API 编程延迟引用服务，请关闭 check，否则服务临时不可用时，会抛出异常，拿到 null 引用，如果 check="false"，总是会返回引用，当服务恢复时，能自动连上。

二部的情况为第三段“通过 API 编程延迟引用服务”  
同时根据试验，建议将check置为false，可使用一下3种方式：
1. 在每个<dubbo:reference>标签中配置check为false
1. 在<dubbo:consumer>标签中设置check属性，同时注意consumer标签的设置可被reference标签覆盖
1. 在java启动命令中增加参数-Ddubbo.reference.check=false，同时注意此参数不可被reference标签覆盖，与方法2的“可被reference标签覆盖”不同