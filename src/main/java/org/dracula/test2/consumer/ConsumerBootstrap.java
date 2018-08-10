package org.dracula.test2.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * @author dk
 */
@SpringBootApplication
@ImportResource("classpath:org/dracula/test2/consumer/load-consumer.xml")
public class ConsumerBootstrap {

    public static void main(String[] args){
        SpringApplication.run(ConsumerBootstrap.class, args);
    }

}
