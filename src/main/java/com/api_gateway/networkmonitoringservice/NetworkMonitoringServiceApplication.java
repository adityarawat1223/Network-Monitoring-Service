package com.api_gateway.networkmonitoringservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NetworkMonitoringServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetworkMonitoringServiceApplication.class, args);
    }

}
