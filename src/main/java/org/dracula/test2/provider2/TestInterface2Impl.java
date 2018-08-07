package org.dracula.test2.provider2;

import org.dracula.test2.TestInterface2;

/**
 * @author dk
 */
public class TestInterface2Impl implements TestInterface2 {

    @Override
    public String sayHello(String name) {
        return "hello "+name;
    }
}
