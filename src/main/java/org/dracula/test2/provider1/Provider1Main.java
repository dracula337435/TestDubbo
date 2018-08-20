package org.dracula.test2.provider1;

import org.dracula.test2.CommonMain;

/**
 * @author dk
 */
public class Provider1Main {

    public static void main(String[] args){
        CommonMain.useDubboContainer("classpath*:org/dracula/test2/provider1/load-provider1.xml");
    }

}
