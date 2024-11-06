package org.example.flow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class QueueTicketingFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueTicketingFlowApplication.class, args);
    }

}
