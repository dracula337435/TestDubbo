package org.dracula.test2.consumer;

import org.dracula.test2.CommonMain;

/**
 * @author dk
 */
public class ConsumserMain {

    public static void main(String[] args){
        CommonMain.useDubboContainer("org/dracula/test2/consumer/load-consumer.xml");
    }

}
