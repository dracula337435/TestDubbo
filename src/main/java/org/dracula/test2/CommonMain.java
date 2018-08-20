package org.dracula.test2;

import com.alibaba.dubbo.container.Main;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author dk
 */
abstract public class CommonMain {

    public static void fun(String... configLocations){
        AbstractApplicationContext ac = new ClassPathXmlApplicationContext(configLocations);
        ac.start();
        System.out.println("started");
        Object obj = new Object();
        synchronized (obj){
            try {
                obj.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ac.close();
    }

    public static void useDubboContainer(String configLocation){
        System.setProperty("dubbo.spring.config", configLocation);
        Main.main(null);
    }

}
