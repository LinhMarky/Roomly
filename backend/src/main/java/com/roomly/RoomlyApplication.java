package com.roomly;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(info = @Info(title = "ROOMLY API", version = "1.0"))
public class RoomlyApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomlyApplication.class, args);
    }
}
