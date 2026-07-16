package com.orbytum.api;

import io.github.cdimascus.DotEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		DotEnv.load();
		SpringApplication.run(ApiApplication.class, args);
	}

}
