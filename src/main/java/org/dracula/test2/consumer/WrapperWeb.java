package org.dracula.test2.consumer;

import org.dracula.test2.TestInterface1;
import org.dracula.test2.TestInterface2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author dk
 */
@RestController
public class WrapperWeb implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/trySayHello1")
    public String trySayHello1(@RequestParam(value = "name", defaultValue = "gxk") String name){
        TestInterface1 testInterface1 = applicationContext.getBean(TestInterface1.class);
        return testInterface1.sayHello(name);
    }

    @GetMapping("/trySayHello2")
    public String trySayHello2(@RequestParam(value = "name", defaultValue = "gxk") String name){
        TestInterface2 testInterface2 = applicationContext.getBean(TestInterface2.class);
        return testInterface2.sayHello(name);
    }

}
