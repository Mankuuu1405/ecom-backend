package com.one;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AimApplication {

	public static void main(String[] args) {
		SpringApplication.run(AimApplication.class, args);
	}

}
