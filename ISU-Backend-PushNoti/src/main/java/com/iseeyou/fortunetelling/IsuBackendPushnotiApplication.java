package com.iseeyou.fortunetelling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoAuditing
@EnableAsync
public class IsuBackendPushnotiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IsuBackendPushnotiApplication.class, args);
    }

}
