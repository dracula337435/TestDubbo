package org.dracula.test2.consumer;

import org.dracula.test2.TestInterface1;
import org.springframework.beans.factory.annotation.Autowired;

public class Wrapper {

    @Autowired
    private TestInterface1 testInterface1;

    public void test(){
        System.out.println(testInterface1.sayHello("gxk"));
    }

    public TestInterface1 getTestInterface1() {
        return testInterface1;
    }

    public void setTestInterface1(TestInterface1 testInterface1) {
        this.testInterface1 = testInterface1;
    }
}
