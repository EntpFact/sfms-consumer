package com.hdfcbank.sfmsconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.hdfcbank"
})
public class SfmsConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SfmsConsumerApplication.class, args);
	}

}
