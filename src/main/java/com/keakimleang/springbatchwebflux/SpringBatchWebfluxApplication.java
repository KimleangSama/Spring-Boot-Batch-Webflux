package com.keakimleang.springbatchwebflux;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.properties.*;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.keakimleang.springbatchwebflux.config.props")
public class SpringBatchWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchWebfluxApplication.class, args);
    }

}
