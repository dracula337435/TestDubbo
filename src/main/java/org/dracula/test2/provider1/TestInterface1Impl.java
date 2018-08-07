package org.dracula.test2.provider1;

import org.dracula.test2.TestInterface1;

/**
 * @author dk
 */
public class TestInterface1Impl implements TestInterface1 {

    @Override
    public String sayHello(String name) {
        return "hello "+name;
    }
}
