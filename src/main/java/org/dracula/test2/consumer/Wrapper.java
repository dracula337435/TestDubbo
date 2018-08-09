package org.dracula.test2.consumer;

import org.dracula.test2.TestInterface1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * @author dk
 */
@ManagedResource
public class Wrapper {

    @Autowired
    private TestInterface1 testInterface1;

    @ManagedOperation
    public void test(){
        String sayHello = null;
        try {
            sayHello = testInterface1.sayHello("gxk");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(sayHello);
    }

    public TestInterface1 getTestInterface1() {
        return testInterface1;
    }

    public void setTestInterface1(TestInterface1 testInterface1) {
        this.testInterface1 = testInterface1;
    }
}
