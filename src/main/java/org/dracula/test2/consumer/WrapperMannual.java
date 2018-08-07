package org.dracula.test2.consumer;

import org.dracula.test2.TestInterface1;
import org.dracula.test2.TestInterface2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource
public class WrapperMannual implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @ManagedOperation
    public void trySayHello1(){
        try {
            TestInterface1 testInterface1 = applicationContext.getBean(TestInterface1.class);
            System.out.println(testInterface1.sayHello("gxk"));
        } catch (BeansException e) {
            e.printStackTrace();
        }
    }

    @ManagedOperation
    public void trySayHello2(){
        try {
            TestInterface2 testInterface2 = applicationContext.getBean(TestInterface2.class);
            System.out.println(testInterface2.sayHello("gxk"));
        } catch (BeansException e) {
            e.printStackTrace();
        }
    }

}
